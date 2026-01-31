package cash.ice.fbc.service.impl;

import cash.ice.common.dto.zim.*;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.utils.Tool;
import cash.ice.fbc.config.FbcProperties;
import cash.ice.fbc.dto.FbcTransferSubmissionRequest;
import cash.ice.fbc.dto.FbcVerifyOtpRequest;
import cash.ice.fbc.entity.FbcPayment;
import cash.ice.fbc.error.FbcException;
import cash.ice.fbc.repository.FbcPaymentRepository;
import cash.ice.fbc.service.FbcPaymentService;
import cash.ice.fbc.service.FbcSenderService;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class FbcPaymentServiceImpl implements FbcPaymentService {
    public static final String FBC = "FBC";
    public static final String SERVICE_HEADER = "ICEcash-FBC-on-us";

    private final FbcPaymentRepository fbcPaymentRepository;
    private final FbcSenderService fbcSenderService;
    private final KafkaSender kafkaSender;
    private final FbcProperties fbcProperties;

    @Override
    public void processPayment(PaymentRequestZim paymentRequest) {
        log.info("  New payment process for vendorRef: {}", paymentRequest.getVendorRef());
        FbcPayment fbcPayment = new FbcPayment()
                .setCreatedTime(Instant.now())
                .setVendorRef(paymentRequest.getVendorRef())
                .setPaymentRequest(paymentRequest)
                .setStatus("init");
        try {
            validatePaymentRequest(paymentRequest);
            var verificationResponse = fbcSenderService.sendAccountVerification(paymentRequest.getAccountNumber());
            fbcPayment.setVerificationResponse(verificationResponse);
            Objects.requireNonNull(verificationResponse.getResponse().getCustomerAccountNumber(), "'customerAccountNumber' verification response field is empty");
//            Objects.requireNonNull(verificationResponse.getResponse().getBranchCode(), "'branchCode' verification response field is empty");          // todo need?

            fbcPayment.setTransferSubmissionRequest(new FbcTransferSubmissionRequest()
                    .setCurrency(paymentRequest.getCurrencyCode())
                    .setAmount(paymentRequest.getAmount())
                    .setExternalReference(paymentRequest.getVendorRef())
                    .setInitiatorID(fbcProperties.getInitiatorId())
                    .setPaymentDetails(fbcProperties.getPaymentDetails())
                    .setSourceAccountNumber(fbcPayment.getVerificationResponse().getResponse().getCustomerAccountNumber())
                    .setSourceBranchCode(fbcPayment.getVerificationResponse().getResponse().getBranchCode()));
            var transferResponse = fbcSenderService.sendTransferSubmission(fbcPayment.getTransferSubmissionRequest());
            fbcPayment.setTransferSubmissionResponse(transferResponse);

            fbcPaymentRepository.save(fbcPayment.setStatus("otp"));
            PaymentOtpWaitingZim otpWaitingResponse = new PaymentOtpWaitingZim()
                    .setVendorRef(paymentRequest.getVendorRef())
                    .setMobile(fbcPayment.getTransferSubmissionResponse().getResponse().getPhoneNumber())
                    .setBankName(FBC);
            kafkaSender.sendZimPaymentResult(fbcPayment.getVendorRef(), otpWaitingResponse);
            log.info("  sent waiting OTP response: " + otpWaitingResponse);

        } catch (FbcException | TransactionException | DataAccessResourceFailureException e) {
            throw e;
        } catch (Throwable e) {
            throw new FbcException(fbcPayment, e.getMessage(), e);
        }
    }

    public void validatePaymentRequest(PaymentRequestZim paymentRequest) {
        Objects.requireNonNull(paymentRequest.getAccountNumber(), "'accountNumber' payment field is not provided");
        Objects.requireNonNull(paymentRequest.getAmount(), "'amount' payment field is not provided");
        Objects.requireNonNull(paymentRequest.getCurrencyCode(), "'currencyCode' is not provided");
//        Objects.requireNonNull(paymentRequest.getPaymentDescription(), "'paymentDescription' is not provided");           // todo need?
        if (paymentRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("'amount' payment field is not positive");
        }
    }

    @Override
    public void processOtp(PaymentOtpRequestZim otpRequest, Headers headers) {
        log.info("  OTP process for vendorRef: {}", otpRequest.getVendorRef());
        FbcPayment fbcPayment = findFbcPayment(otpRequest.getVendorRef());
        try {
            Objects.requireNonNull(otpRequest.getOtp(), "'otp' field is not provided");
            fbcPayment.setVerifyOtpRequest(new FbcVerifyOtpRequest()
                    .setTransactionReference(otpRequest.getVendorRef())
                    .setOtp(otpRequest.getOtp()));
            var verifyResponse = fbcSenderService.sendVerifyOtp(fbcPayment.getVerifyOtpRequest());
            fbcPayment.setVerifyOtpResponse(verifyResponse);

            var statusResponse = fbcSenderService.sendQueryStatus(otpRequest.getVendorRef());           // todo need?
            fbcPayment.setStatusResponse(statusResponse);

            fbcPaymentRepository.save(fbcPayment.setStatus("success").setUpdatedTime(Instant.now()));
            PaymentSuccessZim successResponse = new PaymentSuccessZim()
                    .setVendorRef(otpRequest.getVendorRef())
                    .setBankName(FBC);
            kafkaSender.sendZimPaymentResult(otpRequest.getVendorRef(), successResponse, headers, SERVICE_HEADER);
            log.info("  sent successful payment response: " + successResponse);

        } catch (FbcException | TransactionException | DataAccessResourceFailureException e) {
            throw e;
        } catch (Throwable e) {
            throw new FbcException(fbcPayment, e.getMessage(), e);
        }
    }

    @Override
    public void processError(FbcException e) {
        log.warn("  Handling error: {}, reason: {}, for {}", e.getCauseCanonicalName(), e.getMessage(), e.getVendorRef());
        if (e.getFbcPayment() != null) {
            try {
                fbcPaymentRepository.save(e.getFbcPayment()
                        .setReason(e.getMessage())
                        .setStatus("error")
                        .setUpdatedTime(Instant.now()));
            } catch (Exception ex) {
                log.warn("  Failed storing error, reason: {}, for {}", ex.getMessage(), e.getVendorRef());
            }
        }
        kafkaSender.sendZimPaymentError(e.getFbcPayment() != null ? e.getFbcPayment().getVendorRef() : e.getVendorRef(),
                new PaymentErrorZim(e.getVendorRef(), e.getMessage(), ErrorCodes.EC8001, Tool.currentDateTime()));
    }

    @Override
    public void processRefund(String vendorRef) {
        log.error("Refund for FBC payments is not allowed! vendorRef: {}", vendorRef);
    }

    private FbcPayment findFbcPayment(String vendorRef) {
        List<FbcPayment> payments = fbcPaymentRepository.findByVendorRef(vendorRef);
        if (payments.isEmpty()) {
            throw new FbcException(vendorRef, "No pending payment for vendorRef: " + vendorRef);
        }
        return payments.getLast();
    }
}

package cash.ice.posb.service.impl;

import cash.ice.common.dto.zim.*;
import cash.ice.posb.PosbRestClient;
import cash.ice.posb.dto.PosbPayment;
import cash.ice.posb.dto.posb.*;
import cash.ice.posb.error.PosbException;
import cash.ice.posb.error.PosbRetryableException;
import cash.ice.posb.kafka.PosbKafkaProducer;
import cash.ice.posb.repository.PosbPaymentRepository;
import cash.ice.posb.service.PosbPaymentService;
import cash.ice.posb.service.PosbValidationService;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor
@Slf4j
public class PosbPaymentServiceImpl implements PosbPaymentService {
    public static final String POSB = "POSB";
    public static final String SERVICE_HEADER = "ICEcash-POSB";
    public static final String TYPE_ID_HEADER = "__TypeId__";

    private final PosbValidationService validationService;
    private final PosbPaymentRepository paymentRepository;
    private final PosbRestClient posbRestClient;
    private final PosbKafkaProducer posbKafkaProducer;

    @Override
    public void processPayment(PaymentRequestZim paymentRequest) {
        log.info("  New payment process for vendorRef: {}", paymentRequest.getVendorRef());
        PosbPayment posbPayment = new PosbPayment()
                .setCreatedTime(Instant.now())
                .setVendorRef(paymentRequest.getVendorRef())
                .setPaymentRequest(paymentRequest)
                .setTraceId(UUID.randomUUID().toString())
                .setStatus("init")
                .setInstructionRequest(new PosbInstructionRequest()
                        .setPaymentReference(paymentRequest.getVendorRef())
                        .setCustomerAccountNumber(paymentRequest.getAccountNumber())
                        .setAmount(paymentRequest.getAmount())
                        .setCurrency(paymentRequest.getCurrencyCode())
                        .setDescription(paymentRequest.getPaymentDescription()));
        try {
            validationService.validatePaymentRequest(paymentRequest);
            log.debug("  sending instruction request: {}, traceId: {}", posbPayment.getInstructionRequest(), posbPayment.getTraceId());
            PosbInstructionResponse instructionResponse = posbRestClient.sendInstruction(
                    posbPayment.getInstructionRequest(),
                    posbPayment.getTraceId());
            log.info("  instruction response: " + instructionResponse);
            posbPayment.setInstructionResponse(instructionResponse).setStatus("otp");
            if (paymentRepository.findByVendorRef(posbPayment.getVendorRef()).isEmpty()) {
                paymentRepository.save(posbPayment);
            } else {
                paymentRepository.update(posbPayment);
            }
            PaymentOtpWaitingZim otpWaitingResponse = new PaymentOtpWaitingZim()
                    .setVendorRef(paymentRequest.getVendorRef())
                    .setMobile(instructionResponse.getCustomerMobileNumber())
                    .setBankName(POSB);
            posbKafkaProducer.sendPaymentResponse(posbPayment.getVendorRef(), otpWaitingResponse, false);
            log.info("  sent waiting OTP response: " + otpWaitingResponse);

        } catch (HttpClientResponseException e) {
            throw new PosbException(posbPayment, String.format("Instruction error response: %s '%s'", e.getStatus().getCode(), e.getMessage()), e);
        } catch (HttpClientException e) {                                      // mongo unreachable: DataAccessException
            throw new PosbRetryableException(posbPayment, e.getMessage(), e);
        } catch (Throwable e) {
            throw new PosbException(posbPayment, e.getMessage(), e);
        }
    }

    @Override
    public void processOtp(PaymentOtpRequestZim otpRequest) {
        log.info("  OTP process for vendorRef: {}", otpRequest.getVendorRef());
        PosbPayment posbPayment = findPosbPayment(otpRequest.getVendorRef());
        try {
            validationService.validateOtpRequest(otpRequest);
            posbPayment.setConfirmationRequest(new PosbConfirmationRequest()
                    .setPaymentReference(otpRequest.getVendorRef())
                    .setOtp(otpRequest.getOtp()));
            log.debug("  sending confirmation request: {}, traceId: {}", posbPayment.getConfirmationRequest(), posbPayment.getTraceId());
            PosbConfirmationResponse confirmationResponse = posbRestClient.sendConfirmation(
                    posbPayment.getConfirmationRequest(),
                    posbPayment.getTraceId());
            log.info("  confirmation response: " + confirmationResponse);
            posbPayment.setConfirmationResponse(confirmationResponse).setStatus("confirmed");
            validationService.checkSuccessfulStatus(confirmationResponse.getStatus());

            log.debug("  sending status request for vendorRef: {}", otpRequest.getVendorRef());
            PosbStatusResponse statusResponse = posbRestClient.getPaymentStatus(otpRequest.getVendorRef());
            log.info("  status response: " + statusResponse);
            posbPayment.setStatusResponse(statusResponse);
            validationService.checkSuccessfulStatus(statusResponse.getStatus());

            paymentRepository.update(posbPayment.setStatus("success").setUpdatedTime(Instant.now()));
            PaymentSuccessZim successResponse = new PaymentSuccessZim()
                    .setVendorRef(otpRequest.getVendorRef())
                    .setBankName(POSB)
                    .setTransactionId(statusResponse.getPaymentReference());
            posbKafkaProducer.sendPaymentResponse(otpRequest.getVendorRef(), successResponse, true);
            log.info("  sent successful payment response: " + successResponse);

        } catch (HttpClientResponseException e) {
            throw new PosbException(posbPayment, String.format("Confirmation error response: %s '%s'", e.getStatus().getCode(), e.getMessage()), e);
        } catch (HttpClientException e) {                                      // mongo unreachable: DataAccessException
            throw new PosbRetryableException(posbPayment, e.getMessage(), e);
        } catch (Throwable e) {
            throw new PosbException(posbPayment, e.getMessage(), e);
        }
    }

    @Override
    public void processError(Throwable cause) {
        try {
            if (cause instanceof PosbException p) {
                log.warn("  Handling error: {}, reason: {}, for {}", p.getCause().getClass().getCanonicalName(), p.getMessage(), p.getVendorRef());
                if (p.getPosbPayment() != null) {
                    try {
                        p.getPosbPayment().setReason(p.getMessage())
                                .setStatus("error")
                                .setUpdatedTime(Instant.now());
                        if (paymentRepository.findByVendorRef(p.getPosbPayment().getVendorRef()).isEmpty()) {
                            paymentRepository.save(p.getPosbPayment());
                        } else {
                            paymentRepository.update(p.getPosbPayment());
                        }
                    } catch (Exception e) {
                        log.warn("  Failed storing error, reason: {}, for {}", e.getMessage(), p.getVendorRef());
                    }
                }
                PaymentErrorZim paymentError = new PaymentErrorZim().setVendorRef(p.getVendorRef()).setMessage(p.getMessage());
                posbKafkaProducer.sendPaymentError(p.getVendorRef(), paymentError);
                log.info("  Error sent: {}", paymentError);

            } else {
                log.error("Unhandled error: {}", cause.getMessage(), cause);
            }
        } catch (Exception e) {
            log.warn("Failed handling error: {}", e.getMessage(), e);
        }
    }

    @Override
    public void processRefund(PaymentRefundRequestZim refundRequest) {
        log.info("  Refund process for vendorRef: {}", refundRequest.getVendorRef());
        try {
            PosbPayment posbPayment = findPosbPayment(refundRequest.getVendorRef());
            if (posbPayment.getConfirmationResponse() != null
                    && posbPayment.getConfirmationResponse().getStatus().equals("SUCCESSFUL")
                    && posbPayment.getReversalRequest() == null) {

                posbPayment.setReversalRequest(new PosbReversalRequest()
                        .setOriginalPaymentReference(refundRequest.getVendorRef())
                        .setPaymentReversalReference(UUID.randomUUID().toString())
                        .setCustomerAccountNumber(posbPayment.getPaymentRequest().getAccountNumber())
                        .setCurrency(posbPayment.getPaymentRequest().getCurrencyCode())
                        .setAmount(posbPayment.getPaymentRequest().getAmount())
                        .setDescription("Reversal for payment with reference: " + refundRequest.getVendorRef()));
                PosbReversalResponse reversalResponse = posbRestClient.sendReversal(
                        posbPayment.getReversalRequest(),
                        posbPayment.getTraceId());
                log.info("  reversal response: " + reversalResponse);
                paymentRepository.update(posbPayment
                        .setReversalResponse(reversalResponse)
                        .setStatus("refunded")
                        .setRefundedTime(Instant.now()));
                validationService.checkSuccessfulStatus(reversalResponse.getReversalStatus());

            } else {
                log.warn("Skipping refund for vendorRef: {}", refundRequest.getVendorRef());
            }
        } catch (Exception e) {
            log.warn("Refund failed. message: " + e.getMessage(), e);
        }
    }

    @Override
    public PosbPayment getPosbPayment(String vendorRef) {
        return paymentRepository.findByVendorRef(vendorRef).orElse(null);
    }

    private PosbPayment findPosbPayment(String vendorRef) {
        return paymentRepository.findByVendorRef(vendorRef).orElseThrow(() ->
                new PosbException(vendorRef, "No pending payment for vendorRef: " + vendorRef));
    }
}

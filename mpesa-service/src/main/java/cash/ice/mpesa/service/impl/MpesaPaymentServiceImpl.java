package cash.ice.mpesa.service.impl;

import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.dto.BeneficiaryNameResponse;
import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.utils.Tool;
import cash.ice.mpesa.config.MpesaProperties;
import cash.ice.mpesa.dto.Payment;
import cash.ice.mpesa.dto.ReversalStatus;
import cash.ice.mpesa.dto.TransactionStatus;
import cash.ice.mpesa.entity.MpesaPayment;
import cash.ice.mpesa.error.MpesaException;
import cash.ice.mpesa.repository.MpesaPaymentRepository;
import cash.ice.mpesa.service.MpesaPaymentService;
import cash.ice.mpesa.service.MpesaSenderService;
import com.fc.sdk.APIResponse;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cash.ice.common.error.ErrorCodes.EC9001;

@Service
@RequiredArgsConstructor
@Profile(IceCashProfile.PROD)
@Slf4j
public class MpesaPaymentServiceImpl implements MpesaPaymentService {
    protected final MpesaSenderService mpesaSenderService;
    protected final MpesaPaymentRepository mpesaPaymentRepository;
    protected final MpesaProperties mpesaProperties;

    @Override
    public MpesaPayment processPayment(Payment payment) {
        log.info("  New mpesa payment process for vendorRef: {}", payment.getVendorRef());
        MpesaPayment mpesaPayment = new MpesaPayment()
                .setVendorRef(payment.getVendorRef())
                .setPayment(payment)
                .setCreatedTime(Instant.now());
        try {
            validatePaymentRequest(payment);
            checkPaymentTimeout(mpesaPayment, payment);
            APIResponse response = sendPayment(payment, mpesaPayment);
            if (response != null) {
                mpesaPayment.setResponseStatus(response.getStatusCode() + " " + response.getReason())
                        .setResponseCode(response.getParameter("output_ResponseCode"))
                        .setResponseDesc(response.getParameter("output_ResponseDesc"))
                        .setTransactionId(response.getParameter("output_TransactionID"))
                        .setConversationId(response.getParameter("output_ConversationID"));
            } else {
                log.debug("  no response");
                mpesaPayment.setResponseStatus("no response");
            }

            if (mpesaProperties.isSendStatusQuery()) {
                if (response == null && mpesaProperties.getStatusQueryAfterErrorDelay() != null && mpesaProperties.getStatusQueryAfterErrorDelay().toMillis() > 0) {
                    log.debug("  delay {} (du" +
                            "e to no response) for vendorRef: {}", mpesaProperties.getStatusQueryAfterErrorDelay(), payment.getVendorRef());
                    Tool.sleep(mpesaProperties.getStatusQueryAfterErrorDelay().toMillis());
                } else if (response != null && !mpesaPayment.isPaymentSuccessful() && mpesaProperties.getStatusQueryAfterFailureResponseDelay() != null
                        && mpesaProperties.getStatusQueryAfterFailureResponseDelay().toMillis() > 0) {
                    log.debug("  delay {} (due to failure response) for vendorRef: {}", mpesaProperties.getStatusQueryAfterErrorDelay(), payment.getVendorRef());
                    Tool.sleep(mpesaProperties.getStatusQueryAfterErrorDelay().toMillis());
                }
                APIResponse statusResponse = sendQueryStatus(payment, mpesaPayment.getVendorRef());
                if (statusResponse != null) {
                    mpesaPayment.setTransactionStatus(statusResponse.getParameter("output_ResponseTransactionStatus"))
                            .setTransactionStatusLine(statusResponse.getStatusLine())
                            .setTransactionStatusResponse(String.format("%s %s", statusResponse.getParameter("output_ResponseCode"), statusResponse.getParameter("output_ResponseDesc")));
                } else {
                    log.debug("  no response");
                    mpesaPayment.setTransactionStatusResponse("no response");
                }
            }
            handleResponse(mpesaPayment, payment);
            return mpesaPayment;

        } catch (MpesaException | TransactionException | DataAccessResourceFailureException e) {
            throw e;
        } catch (Throwable e) {
            throw new MpesaException(mpesaPayment, e.getMessage(), EC9001, e);
        }
    }

    public void validatePaymentRequest(Payment payment) {
        Objects.requireNonNull(payment.getVendorRef(), "Payment 'vendorRef' is not provided");
        Objects.requireNonNull(payment.getMsisdn(), "Payment 'msisdn' field is not provided");
        Objects.requireNonNull(payment.getAmount(), "Payment 'amount' field is not provided");
        if (payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("'amount' payment field is negative");
        }
    }

    private void checkPaymentTimeout(MpesaPayment mpesaPayment, Payment payment) {
        Long expirationTime = (Long) payment.getMetaData().get(PaymentRequestZim.EXPIRATION_TIME);
        if (expirationTime != null && Instant.now().toEpochMilli() > expirationTime) {
            throw new MpesaException(mpesaPayment, "Payment expired", ErrorCodes.EC9008);
        }
    }

    @Override
    public void handleResponse(MpesaPayment mpesaPayment, Payment payment) {
        if (mpesaPayment.isPaymentSuccessful()) {
            log.debug("  Successful payment for: {}", mpesaPayment.getVendorRef());
            if (!"Completed".equals(mpesaPayment.getTransactionStatus())) {
                log.warn("  Error, transaction status is: {} for vendorRef: {}", mpesaPayment.getTransactionStatus(), mpesaPayment.getVendorRef());
            }
            mpesaPaymentRepository.save(mpesaPayment.setStatus("success").setUpdatedTime(Instant.now()));
        } else {
            if (mpesaPayment.getResponseCode() != null) {
                throw new MpesaException(mpesaPayment, String.format("Received error from external mpesa server: %s %s", mpesaPayment.getResponseCode(), mpesaPayment.getResponseDesc()), ErrorCodes.EC9002);
            } else {
                throw new MpesaException(mpesaPayment, String.format("Received error from external mpesa server: %s", mpesaPayment.getResponseStatus()), ErrorCodes.EC9003);
            }
        }
    }

    private APIResponse sendPayment(Payment payment, MpesaPayment mpesaPayment) {
        return switch (payment.getPaymentType()) {
            case Inbound -> sendInboundPayment(payment);
            case Outbound -> sendOutboundPayment(payment);
            default -> throw new MpesaException(mpesaPayment, String.format("Wrong paymentType: %s",
                    payment.getPaymentType()), ErrorCodes.EC9004);
        };
    }

    protected APIResponse sendInboundPayment(Payment payment) {
        return mpesaSenderService.sendInboundRequest(payment.getVendorRef(), payment.getMsisdn(), payment.getAmount());
    }

    protected APIResponse sendOutboundPayment(Payment payment) {
        return mpesaSenderService.sendOutboundRequest(payment.getVendorRef(), payment.getMsisdn(), payment.getAmount());
    }

    protected APIResponse sendQueryStatus(Payment payment, String paymentId) {
        return mpesaSenderService.sendQueryTransactionStatusRequest(paymentId);
    }

    protected APIResponse sendRefund(MpesaPayment mpesaPayment, Map<String, Object> paymentMetaData) {
        return mpesaSenderService.sendReversalRequest(mpesaPayment.getTransactionId(), mpesaPayment.getPayment().getAmount());
    }

    @Override
    public MpesaPayment getMpesaPayment(String vendorRef) {
        List<MpesaPayment> payments = mpesaPaymentRepository.findByVendorRef(vendorRef);
        return payments.isEmpty() ? null : payments.getLast();
    }

    @Override
    public MpesaPayment getMpesaPaymentByTransactionId(String transactionId) {
        List<MpesaPayment> payments = mpesaPaymentRepository.findByTransactionId(transactionId);
        return payments.isEmpty() ? null : payments.getLast();
    }


    @Override
    public void processError(MpesaException e) {
        log.warn("  Handling error: '{}' for {}, (root: {})", e.getMessage(), e.getVendorRef(), e.getCauseCanonicalName());
        if (e.getMpesaPayment() != null) {
            try {
                mpesaPaymentRepository.save(e.getMpesaPayment()
                        .setErrorCode(e.getErrorCode())
                        .setErrorMessage(e.getMessage())
                        .setStatus("error")
                        .setUpdatedTime(Instant.now()));
            } catch (Exception ex) {
                log.warn("  Failed storing error, reason: {}, for {}", ex.getMessage(), e.getVendorRef());
            }
            if (e.getMpesaPayment().isPaymentSuccessful()) {
                processRefund(e.getVendorRef());
            }
        }
        if (e.getCause() != null) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public MpesaPayment processRefund(String vendorRef) {
        log.info("  Refunding mpesa payment for vendorRef: {}", vendorRef);
        MpesaPayment mpesaPayment = getMpesaPayment(vendorRef);
        if (mpesaPayment == null) {
            log.error("No pending payment to refund for vendorRef: {}", vendorRef);
        } else if (mpesaPayment.isPaymentSuccessful() && !mpesaPayment.isRefunded()) {
            if (mpesaPayment.getTransactionId() != null) {
                mpesaPayment.setRefundTime(Instant.now());
                APIResponse response = sendRefund(mpesaPayment, mpesaPayment.getPayment().getMetaData());
                if (response == null) {
                    log.error("Error! No response for refund, vendorRef: {}", vendorRef);
                    mpesaPayment.setRefundResponseStatus("no response");
                } else {
                    mpesaPayment.setRefundResponseStatus(response.getStatusCode() + " " + response.getReason());
                    mpesaPayment.setRefundResponse(String.format("%s %s", response.getParameter("output_ResponseCode"), response.getParameter("output_ResponseDesc")));
                    mpesaPayment.setRefundTransactionId(response.getParameter("output_TransactionID"));
                    mpesaPayment.setRefundConversationId(response.getParameter("output_ConversationID"));
                    if ("INS-0".equals(response.getParameter("output_ResponseCode"))) {
                        mpesaPayment.setStatus("refunded").setRefunded(true);
                        if (mpesaProperties.isSendStatusQuery()) {
                            APIResponse statusResponse = sendQueryStatus(mpesaPayment.getPayment(), mpesaPayment.getRefundTransactionId());
                            if (statusResponse != null) {
                                mpesaPayment.setRefundTransactionStatus(statusResponse.getParameter("output_ResponseTransactionStatus"))
                                        .setRefundTransactionStatusResponse(String.format("%s %s", statusResponse.getParameter("output_ResponseCode"), statusResponse.getParameter("output_ResponseDesc")));
                            } else {
                                log.debug("  refund transaction status: no response");
                                mpesaPayment.setRefundTransactionStatusResponse("no response");
                            }
                        }

                    } else {
                        mpesaPayment.setStatus("refundError");
                        log.error("Error! Refund failed: {}: {} for vendorRef: {}", response.getParameter("output_ResponseCode"),
                                response.getParameter("output_ResponseDesc"), vendorRef);
                    }
                }
                mpesaPaymentRepository.save(mpesaPayment);
            } else {
                log.warn("  Cannot refund! 'TransactionId' is absent for {}", mpesaPayment.getVendorRef());
            }
        } else {
            log.warn("  refund, but payment successful: {}, already refunded: {}, for {}", mpesaPayment.isPaymentSuccessful(), mpesaPayment.isRefunded(), mpesaPayment.getVendorRef());
        }
        return mpesaPayment;
    }

    @Override
    public ReversalStatus manualRefund(String vendorRefOrTransactionId) {
        ReversalStatus reversalStatus = new ReversalStatus().setOriginalTransactionId(vendorRefOrTransactionId);
        MpesaPayment mpesaPayment = getMpesaPayment(vendorRefOrTransactionId);
        if (mpesaPayment != null) {
            mpesaPayment = processRefund(vendorRefOrTransactionId);
            reversalStatus.setResponseStatus(mpesaPayment.getRefundResponseStatus())
                    .setRefunded(mpesaPayment.isRefunded())
                    .setResponse(mpesaPayment.getRefundResponse())
                    .setReversalTransactionId(mpesaPayment.getRefundTransactionId())
                    .setReversalConversationId(mpesaPayment.getRefundConversationId())
                    .setTransactionStatus(mpesaPayment.getRefundTransactionStatus())
                    .setTransactionStatusResponse(mpesaPayment.getRefundTransactionStatusResponse());
        } else {
            log.info("  Refunding mpesa payment for transactionId: {}", vendorRefOrTransactionId);
            APIResponse response = mpesaSenderService.sendReversalRequest(vendorRefOrTransactionId, null);
            if (response != null) {
                reversalStatus.setResponseStatus(response.getStatusCode() + " " + response.getReason())
                        .setResponse(String.format("%s %s", response.getParameter("output_ResponseCode"), response.getParameter("output_ResponseDesc")))
                        .setReversalTransactionId(response.getParameter("output_TransactionID"))
                        .setReversalConversationId(response.getParameter("output_ConversationID"));
            } else {
                reversalStatus.setResponse("no response");
            }
            APIResponse statusResponse = mpesaSenderService.sendQueryTransactionStatusRequest(vendorRefOrTransactionId);
            if (statusResponse != null) {
                reversalStatus.setTransactionStatus(statusResponse.getParameter("output_ResponseTransactionStatus"))
                        .setTransactionStatusResponse(String.format("%s %s", statusResponse.getParameter("output_ResponseCode"), statusResponse.getParameter("output_ResponseDesc")));
            }
        }
        log.debug("  reversal status: {}", reversalStatus);
        return reversalStatus;
    }

    @Override
    public TransactionStatus queryTransactionStatus(String transactionOrConversationOrVendorId) {
        TransactionStatus status = new TransactionStatus().setQueryReference(transactionOrConversationOrVendorId);
        APIResponse response = mpesaSenderService.sendQueryTransactionStatusRequest(transactionOrConversationOrVendorId);
        if (response != null) {
            status.setStatusCode(response.getStatusCode())
                    .setStatusReason(response.getReason())
                    .setResponseCode(response.getParameter("output_ResponseCode"))
                    .setResponseDesc(response.getParameter("output_ResponseDesc"))
                    .setResponseTransactionStatus(response.getParameter("output_ResponseTransactionStatus"));
        } else {
            status.setStatusReason("no response");
        }
        log.debug("  status: {}", status);
        return status;
    }

    @Override
    public BeneficiaryNameResponse queryCustomerName(String msisdn) {
        BeneficiaryNameResponse nameResponse = new BeneficiaryNameResponse().setMsisdn(msisdn);
        APIResponse response = mpesaSenderService.sendQueryNameRequest(msisdn);
        if (response != null) {
            nameResponse.setName(response.getParameter("output_CustomerName"));
            if (response.getParameter("output_ResponseCode") == null) {
                nameResponse.setErrorCode(ErrorCodes.EC9003);
                nameResponse.setErrorMessage(String.format("Received error from external mpesa server: %s %s", response.getStatusCode(), response.getReason()));
            } else if (!"INS-0".equals(response.getParameter("output_ResponseCode"))) {
                nameResponse.setErrorCode(ErrorCodes.EC9002);
                nameResponse.setErrorMessage(String.format("Received error from external mpesa server: %s %s", response.getParameter("output_ResponseCode"), response.getParameter("output_ResponseDesc")));
            }
        } else {
            nameResponse.setErrorCode(ErrorCodes.EC9002);
            nameResponse.setErrorMessage("No response received from external mpesa server");
        }
        log.debug("  response: {}, {}", response != null ? response.getStatusLine() : "", nameResponse);
        return nameResponse;
    }
}

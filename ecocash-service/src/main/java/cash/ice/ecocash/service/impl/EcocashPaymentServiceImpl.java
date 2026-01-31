package cash.ice.ecocash.service.impl;

import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.dto.zim.PaymentErrorZim;
import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.common.dto.zim.PaymentSuccessZim;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.utils.Tool;
import cash.ice.ecocash.config.EcocashProperties;
import cash.ice.ecocash.controller.EcocashCallbackController;
import cash.ice.ecocash.dto.*;
import cash.ice.ecocash.entity.EcocashMerchant;
import cash.ice.ecocash.entity.EcocashPayment;
import cash.ice.ecocash.repository.EcocashMerchantRepository;
import cash.ice.ecocash.repository.EcocashPaymentRepository;
import cash.ice.ecocash.service.EcocashPaymentService;
import cash.ice.ecocash.service.EcocashSenderService;
import error.EcocashException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.header.Headers;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cash.ice.common.error.ErrorCodes.*;
import static cash.ice.ecocash.listener.EcocashServiceListener.SERVICE_HEADER;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Service
@RequiredArgsConstructor
@Profile(IceCashProfile.PROD)
@Slf4j
public class EcocashPaymentServiceImpl implements EcocashPaymentService {
    protected static final String MICROSERVICE_ID = "202";
    protected static final String COMPLETED = "COMPLETED";
    protected static final String FAILED = "FAILED";
    protected static final String TRANSACTION_TIMED_OUT = "TRANSACTION TIMED OUT";
    protected static final String ECOCASH = "Ecocash";
    protected static final List<String> FINISHED_STATUSES = List.of(COMPLETED, FAILED, TRANSACTION_TIMED_OUT);

    protected final EcocashProperties ecocashProperties;
    protected final EcocashPaymentRepository ecocashPaymentRepository;
    protected final EcocashMerchantRepository ecocashMerchantRepository;
    protected final EcocashSenderService ecocashSenderService;
    protected final KafkaSender kafkaSender;

    @Override
    public void processPayment(Payment payment, Headers headers) {
        log.info(">>> Ecocash process payment for vendorRef: {}", payment.getVendorRef());
        EcocashPayment ecocashPayment = new EcocashPayment()
                .setVendorRef(payment.getVendorRef())
                .setPendingPayment(payment.getPendingRequest())
                .setCreatedTime(Instant.now())
                .setKafkaHeaders(Tool.toKafkaHeaderKeys(headers))
                .setStatus("init");
        try {
            validateInitiator(payment.getInitiator());
            checkPaymentTimeout(ecocashPayment, payment.getMetaData());
            EcocashMerchant merchant = getMerchant(payment.getTx());
            ecocashPayment.setRequest(EcocashCallbackPayment.createPurchase()
                    .setClientCorrelator(Tool.generateDigits(14, false))
                    .setNotifyUrl(getNotifyUrl())
                    .setReferenceCode(payment.getVendorRef())
                    .setEndUserId(getMsisdnPart(payment.getInitiator()))
                    .setPaymentAmount(new PaymentAmount()
                            .setChargeMetaData(new ChargeMetaData()
                                    .setChannel(ecocashProperties.getPaymentAmountChannel())
                                    .setPurchaseCategoryCode(ecocashProperties.getPaymentAmountPurchaseCategoryCode())
                                    .setOnBeHalfOf(ecocashProperties.getPaymentAmountOnBehalfOf()))
                            .setCharginginformation(new ChargingInformation()
                                    .setAmount(payment.getAmount())
                                    .setCurrency(getCurrencyCode(payment.getCurrencyCode(), payment.getMetaData()))
                                    .setDescription(StringUtils.defaultString((String) payment.getMetaData().get("description")))))
                    .setMerchantCode(merchant.getCode())
                    .setMerchantPin(merchant.getPin())
                    .setMerchantNumber(merchant.getNumber())
                    .setCurrencyCode(getCurrencyCode(payment.getCurrencyCode(), payment.getMetaData()))
                    .setCountryCode(ecocashProperties.getRequestCountryCode())
                    .setTerminalId(MICROSERVICE_ID + StringUtils.defaultString(payment.getPartnerId()))
                    .setLocation(ecocashProperties.getRequestLocation())
                    .setSuperMerchantName(ecocashProperties.getRequestSuperMerchantName())
                    .setMerchantName(ecocashProperties.getRequestMerchantName())
                    .setRemarks(getRemarksFromRequest(payment.getMetaData())));
            ecocashPayment.setEndUserId(ecocashPayment.getRequest().getEndUserId());
            ecocashPayment.setClientCorrelator(ecocashPayment.getRequest().getClientCorrelator());

            EcocashCallbackPaymentResponse response = sendPayment(ecocashPayment, ecocashPayment.getRequest());
            log.debug("  Initial response: {}, for {}", response.getTransactionOperationStatus(), response.getReferenceCode());

            ecocashPayment = ecocashPaymentRepository.save(ecocashPayment.setStatus("sent").setInitialResponse(response));
            handleActiveResponse(response, ecocashPayment);

        } catch (EcocashException | TransactionException | DataAccessResourceFailureException e) {
            throw e;
        } catch (Throwable e) {
            throw new EcocashException(ecocashPayment, e.getMessage(), EC6001, e);
        }
    }

    protected EcocashCallbackPaymentResponse sendPayment(EcocashPayment ecocashPayment, EcocashCallbackPayment request) {
        return ecocashSenderService.sendPayment(request);
    }

    @Override
    public void callbackResponse(EcocashCallbackPaymentResponse response) {
        EcocashPayment ecocashPayment = ecocashPaymentRepository.findByClientCorrelator(response.getClientCorrelator())
                .orElseThrow(() -> new EcocashException("No pending payment for clientCorrelator: " +
                        response.getClientCorrelator(), EC6004));
        ecocashPayment.setCallbackResponse(Boolean.TRUE).setFinalResponse(response);

        if (!ecocashPayment.isFinishedPayment()) {
            long durationSeconds = Duration.between(ecocashPayment.getCreatedTime(), Instant.now()).getSeconds();
            handleResponse(response, ecocashPayment, durationSeconds);

        } else if (!response.getTransactionOperationStatus().equals(ecocashPayment.getTransactionOperationStatus())) {
            log.info("  callback got {} status, but already was {}",
                    response.getTransactionOperationStatus(), ecocashPayment.getTransactionOperationStatus());
            throw new EcocashException(ecocashPayment, ecocashPayment.getReason(), ecocashPayment.getErrorCode());
        } else {
            log.debug("  callback got already handled result");
        }
    }

    @Override
    public void checkStatus(EcocashPayment ecocashPayment, long paymentDurationSeconds) {
        EcocashCallbackPaymentResponse response = sendPaymentStatus(
                ecocashPayment,
                ecocashPayment.getEndUserId(),
                ecocashPayment.getClientCorrelator());
        handleResponse(response, ecocashPayment, paymentDurationSeconds);
    }

    protected EcocashCallbackPaymentResponse sendPaymentStatus(EcocashPayment ecocashPayment, String endUserId, String clientCorrelator) {
        return ecocashSenderService.requestPaymentStatus(endUserId, clientCorrelator);
    }

    @Override
    public void recheckStatus(EcocashPayment ecocashPayment) {
        try {
            EcocashCallbackPaymentResponse response = resendPaymentStatus(
                    ecocashPayment,
                    ecocashPayment.getEndUserId(),
                    ecocashPayment.getClientCorrelator());
            boolean recheckSuccess = COMPLETED.equals(response.getTransactionOperationStatus());
            ecocashPayment.setFinalResponse(response)
                    .setEcocashReference(response.getEcocashReference() != null ? response.getEcocashReference() : ecocashPayment.getEcocashReference())
                    .setRecheckedSuccess(recheckSuccess ? Boolean.TRUE : null);
            if (recheckSuccess) {
                log.info("  Recheck found COMPLETED response for timed out payment: {}, refunding", ecocashPayment.getVendorRef());
                refund(ecocashPayment.getVendorRef());
            }
        } finally {
            ecocashPaymentRepository.save(ecocashPayment);
        }
    }

    protected EcocashCallbackPaymentResponse resendPaymentStatus(EcocashPayment ecocashPayment, String endUserId, String clientCorrelator) {
        return ecocashSenderService.requestPaymentStatus(endUserId, clientCorrelator);
    }

    protected void handleResponse(EcocashCallbackPaymentResponse response, EcocashPayment ecocashPayment, long paymentDurationSeconds) {
        if (paymentDurationSeconds < getStatusPollTimeout(ecocashPayment)) {
            handleActiveResponse(response, ecocashPayment);
        } else {            // payment expired
            log.debug("  Expired payment, for {}, ecocashPayment: {}", ecocashPayment.getVendorRef(), ecocashPayment);
            ecocashPayment.setFinalResponse(response).setEcocashReference(response.getEcocashReference())
                    .setRecheck(ecocashProperties.isExpiredPaymentsRecheck() &&
                            !COMPLETED.equals(response.getTransactionOperationStatus()) ? Boolean.TRUE : null);
            throw new EcocashException(ecocashPayment, "Timed out.", EC6008);
        }
    }

    private void handleActiveResponse(EcocashCallbackPaymentResponse response, EcocashPayment ecocashPayment) {
        if (response == null) {
            throw new EcocashException(ecocashPayment, "Ecocash returned empty response", EC6005);
        }
        log.debug("  handle {} for {}, response: {}", response.getTransactionOperationStatus(),
                response.getOrginalMerchantReference(), response);
        ecocashPayment.setFinalResponse(response)
                .setTransactionOperationStatus(response.getTransactionOperationStatus())
                .setEcocashReference(response.getEcocashReference())
                .setFinishedPayment(FINISHED_STATUSES.contains(response.getTransactionOperationStatus()))
                .setUpdatedTime(Instant.now());
        validateResponse(response, ecocashPayment);

        switch (response.getTransactionOperationStatus()) {
            case COMPLETED -> handleCompletedResponse(ecocashPayment);
            case FAILED -> throw new EcocashException(ecocashPayment, "Received Failed", EC6005);
            case TRANSACTION_TIMED_OUT -> throw new EcocashException(ecocashPayment, "Timed out", EC6005);
        }
        ecocashPaymentRepository.save(ecocashPayment);
    }

    private void handleCompletedResponse(EcocashPayment ecocashPayment) {
        try {
            log.debug("  Completed response for " + ecocashPayment.getVendorRef());
            ecocashPayment.setStatus("success");
            performAfterPaymentAction(ecocashPayment);
            switch (ecocashPayment.getPendingPayment()) {
                case FeesData feesData -> kafkaSender.sendEcocashSuccessPayment(ecocashPayment.getVendorRef(), feesData,
                        Tool.toKafkaHeaders(ecocashPayment.getKafkaHeaders()), SERVICE_HEADER);
                case PaymentRequestZim paymentRequest -> kafkaSender.sendZimPaymentResult(paymentRequest.getVendorRef(),
                        new PaymentSuccessZim(paymentRequest.getVendorRef(), ECOCASH, ecocashPayment.getEcocashReference()),
                        Tool.toKafkaHeaders(ecocashPayment.getKafkaHeaders()), SERVICE_HEADER);
                case null, default ->
                        log.error("Wrong pending payment type: {}, cannot send success payment response", ecocashPayment.getPendingPayment());
            }

        } catch (Exception e) {
            throw new EcocashException(ecocashPayment, "Failed to perform after payment action: " +
                    e.getMessage(), EC6009);
        }
    }

    protected void performAfterPaymentAction(EcocashPayment ecocashPayment) {
        // todo LPP needs service to issue a licence
    }

    private void validateResponse(EcocashCallbackPaymentResponse response, EcocashPayment ecocashPayment) {
        EcocashCallbackPayment request = ecocashPayment.getRequest();
        validateResponseField("clientCorrelator", response.getClientCorrelator(),
                request.getClientCorrelator(), ecocashPayment);
        if (response.getPaymentAmount().getTotalAmountCharged() != null) {
            validateResponseField("totalAmountCharged", response.getPaymentAmount().getTotalAmountCharged(),
                    request.getPaymentAmount().getCharginginformation().getAmount(), ecocashPayment);
        }
        validateResponseField("endUserId", response.getEndUserId(), request.getEndUserId(), ecocashPayment);
    }

    private void validateResponseField(String fieldName, Object actualValue, Object expectedValue, EcocashPayment ecocashPayment) {
        if (!Objects.equals(actualValue, expectedValue)) {
            throw new EcocashException(ecocashPayment, String.format("response '%s' validation error: %s",
                    fieldName, actualValue), EC6006);
        }
    }

    private void validateInitiator(String initiator) {
        if (initiator == null || !initiator.matches(ecocashProperties.getPhoneExpression())) {
            throw new EcocashException("Initiator validation failed: " + initiator, EC6002);
        }
    }

    private String getCurrencyCode(String currencyCode, Map<String, Object> metaData) {
        return (String) metaData.getOrDefault("ecocashCurrencyCode", currencyCode);
    }

    private String getNotifyUrl() {
        WebMvcLinkBuilder linkBuilder = WebMvcLinkBuilder.linkTo(methodOn(EcocashCallbackController.class)
                .ecocashCallbackResponse(new EcocashCallbackPaymentResponse()));
        return ecocashProperties.getNotifyUrlHost() + linkBuilder.toUri().getPath();
    }

    private EcocashMerchant getMerchant(String transactionCode) {
        List<EcocashMerchant> merchants = ecocashMerchantRepository.findByTransactionCodesIn(transactionCode);
        if (!merchants.isEmpty()) {
            return merchants.getFirst();
        } else {
            return ecocashMerchantRepository.findByGeneral(Boolean.TRUE).orElseThrow(() ->
                    new EcocashException("No general merchant", EC6003));
        }
    }

    private String getMsisdnPart(String initiator) {
        return initiator.substring(initiator.length() - 9);
    }

    private String getRemarksFromRequest(Map<String, Object> metaData) {
        String remarks = (String) metaData.getOrDefault("cli", "ICECash");
        return Tool.truncate(remarks, 10);
    }

    public EcocashPayment getEcocashPayment(String vendorRef) {
        List<EcocashPayment> payments = ecocashPaymentRepository.findByVendorRef(vendorRef);
        return payments.isEmpty() ? null : payments.getLast();
    }

    private void checkPaymentTimeout(EcocashPayment ecocashPayment, Map<String, Object> metaData) {
        Long expirationTime = (Long) metaData.get(PaymentRequestZim.EXPIRATION_TIME);
        if (expirationTime != null && Instant.now().toEpochMilli() > expirationTime) {
            throw new EcocashException(ecocashPayment, "Payment expired", ErrorCodes.EC6008);
        }
    }

    @Override
    public int getStatusPollInitDelay(EcocashPayment ecocashPayment) {
        return ecocashProperties.getStatusPollInitDelay();
    }

    protected int getStatusPollTimeout(EcocashPayment ecocashPayment) {
        return ecocashProperties.getStatusPollTimeout();
    }

    @Override
    public void processError(EcocashException e) {
        log.warn("  Handling error: '{}' for {}, (root: {})", e.getMessage(), e.getVendorRef(), e.getCauseCanonicalName());
        if (e.getEcocashPayment() != null) {
            try {
                ecocashPaymentRepository.save(e.getEcocashPayment()
                        .setTransactionOperationStatus(FAILED)
                        .setErrorCode(e.getErrorCode())
                        .setReason(e.getMessage())
                        .setFinishedPayment(true)
                        .setStatus("error")
                        .setUpdatedTime(Instant.now()));
            } catch (Exception ex) {
                log.warn("  Failed storing error, reason: {}, for {}", ex.getMessage(), e.getVendorRef());
            }
            if (e.getEcocashPayment().getFinalResponse() != null && COMPLETED.equals(e.getEcocashPayment().getFinalResponse().getTransactionOperationStatus())) {
                log.info("  COMPLETED response for FAILED payment, need refund");
                refund(e.getVendorRef());
            }
            switch (e.getEcocashPayment().getPendingPayment()) {
                case FeesData feesData -> kafkaSender.sendErrorPayment(e.getVendorRef(),
                        new ErrorData(feesData, e.getErrorCode(), e.getMessage()), Tool.toKafkaHeaders(e.getEcocashPayment().getKafkaHeaders()));
                case PaymentRequestZim paymentRequest -> kafkaSender.sendZimPaymentError(e.getVendorRef(),
                        new PaymentErrorZim(paymentRequest.getVendorRef(), e.getMessage(), e.getErrorCode(), Tool.currentDateTime()));
                case null, default ->
                        log.error("Wrong pending payment type:  {}, cannot send success payment response", e.getEcocashPayment().getPendingPayment());
            }
        } else {
            log.error("Unhandled error (no EcocashPayment): {}: {}", e.getVendorRef(), e.getMessage());
        }
        if (e.getCause() != null) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public EcocashPayment refund(String vendorRef) {
        log.info(">>>>>> Ecocash process refund for vendorRef: {}", vendorRef);
        EcocashPayment ecocashPayment = getEcocashPayment(vendorRef);
        if (ecocashPayment == null) {
            throw new EcocashException("No pending payment to refund for vendorRef: " + vendorRef, EC6007);
        }
        if (ecocashPayment.getRefundRequest() == null) {
            if (ecocashPayment.getEcocashReference() != null) {
                EcocashCallbackPayment refundRequest = EcocashCallbackPayment.createRefund(ecocashPayment.getRequest())
                        .setClientCorrelator(Tool.generateDigits(14, false))
                        .setOriginalEcocashReference(ecocashPayment.getEcocashReference());
                log.debug("  Sending Refund, for {}, request: {}", ecocashPayment.getVendorRef(), refundRequest);
                EcocashCallbackPaymentResponse response = sendRefund(ecocashPayment, refundRequest);
                if (!COMPLETED.equals(response.getTransactionOperationStatus())) {
                    log.error("Error! Refund is {} for vendorRef: {}, originalEcocashReference: {}, response: {}",
                            response.getTransactionOperationStatus(), vendorRef, response.getOriginalEcocashReference(), response);
                    ecocashPayment.setStatus("refundError").setRefundFailed(Boolean.TRUE);
                }
                ecocashPaymentRepository.save(ecocashPayment.setStatus("refunded")
                        .setRefundedTime(Instant.now())
                        .setRefundRequest(refundRequest)
                        .setRefundResponse(response));
            } else {
                log.warn("  Cannot refund! 'EcocashReference' is absent for {}", ecocashPayment.getVendorRef());
            }
        } else {
            log.debug("  refund, but already sent, for {}", ecocashPayment.getVendorRef());
        }
        return ecocashPayment;
    }

    protected EcocashCallbackPaymentResponse sendRefund(EcocashPayment ecocashPayment, EcocashCallbackPayment refundRequest) {
        return ecocashSenderService.refundPayment(refundRequest);
    }

    @Override
    public ReversalStatus manualRefund(String vendorRef) {
        EcocashPayment ecocashPayment = refund(vendorRef);
        ReversalStatus reversalStatus = new ReversalStatus()
                .setRefunded(ecocashPayment.getRefundFailed() != Boolean.TRUE)
                .setOriginalEcocashReference(ecocashPayment.getEcocashReference())
                .setTime(ecocashPayment.getRefundedTime());
        if (ecocashPayment.getRefundRequest() != null) {
            reversalStatus.setClientCorrelator(ecocashPayment.getRefundRequest().getClientCorrelator());
        }
        if (ecocashPayment.getRefundResponse() != null) {
            reversalStatus.setTransactionOperationStatus(ecocashPayment.getRefundResponse().getTransactionOperationStatus());
        }
        log.debug("  reversal status: {}", reversalStatus);
        return reversalStatus;
    }
}

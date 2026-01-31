package cash.ice.zim.api.service;

import cash.ice.common.dto.zim.*;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.utils.Tool;
import cash.ice.zim.api.config.ZimApiProperties;
import cash.ice.zim.api.dto.PaymentResponseZim;
import cash.ice.zim.api.dto.ResponseStatus;
import cash.ice.zim.api.error.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static cash.ice.common.dto.zim.PaymentRequestZim.EXPIRATION_TIME;
import static cash.ice.zim.api.dto.ResponseStatus.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZimPaymentService {
    private final KafkaSender kafkaSender;
    private final ZimLoggerService loggerService;
    private final ZimDataService dataService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ZimApiProperties zimApiProperties;

    public void addNewPayment(PaymentRequestZim request) {
        validatePaymentRequest(request);
        if (zimApiProperties.getPaymentTimeout() != null) {
            request.addToMetaData(EXPIRATION_TIME, Instant.now().toEpochMilli() + zimApiProperties.getPaymentTimeout().toMillis());
        }
        kafkaSender.sendZimPaymentRequest(request.getVendorRef(), request);
    }

    private void validatePaymentRequest(PaymentRequestZim request) {
        if (request.getPaymentId() == null && request.getPaymentCollectionId() == null &&
                (request.getWalletId() == null || request.getTransactionCode() == null || request.getAmount() == null)) {
            throw new ValidationException("'metaData.paymentId' field or 'metaData.paymentCollectionId' field or ('amount', 'metaData.walletId', 'metaData.transactionCode') fields set is required");
        } else if (request.getPaymentId() != null && request.getPaymentCollectionId() != null) {
            throw new ValidationException("Only one of ('metaData.paymentId', 'metaData.paymentCollectionId') fields might be set");
        } else if (!zimApiProperties.getAllowedBanks().contains(request.getBankName())) {
            throw new ValidationException(String.format("Wrong 'bankName' field provided. Allowed banks: %s", zimApiProperties.getAllowedBanks()));
        } else if (dataService.isMpesaMtpRequest(request)) {
            validateNotNull(request.getOrganisation(), "'metaData.organisation' field for mpesa MTP payment is required");
            validateNotNull(request.getCardNumber(), "'metaData.cardNumber' field for mpesa MTP payment is required");
            validateNotNull(request.getAccountFundId(), "'metaData.accountFundId' field for mpesa MTP payment is required");
            validateNotNull(request.getAccountId(), "'metaData.accountId' field for mpesa MTP payment is required");
            validateNotNull(request.getPartnerId(), "'metaData.partnerId' field for mpesa MTP payment is required");
            validateNotNull(request.getChannel(), "'metaData.channel' field for mpesa MTP payment is required");
            validateNotNull(request.getSessionId(), "'metaData.sessionId' field for mpesa MTP payment is required");
            validateNotNull(request.getPaymentDescription(), "'metaData.paymentDescription' field for mpesa MTP payment is required");
        }
    }

    private void validateNotNull(Object data, String message) {
        if (data == null) {
            throw new ValidationException(message);
        }
    }

    public void handleNewPayment(PaymentRequestZim request) {
        try {
            if (loggerService.savePaymentRequest(request.getVendorRef(), request)) {
                dataService.obtainAndValidatePaymentData(request);
                checkPaymentTimeout(request, ErrorCodes.EC1108);
                dataService.milestoneCheck(request, 1);
                sendRequestFurther(request.getBankName(), request.getVendorRef(), request);
            }
        } catch (Exception e) {
            if (e instanceof ICEcashException) {
                log.warn("  Error! {}: {}", e.getClass().getCanonicalName(), e.getMessage());
            } else {
                log.error("  Error! {}: {}", e.getClass().getCanonicalName(), e.getMessage(), e);
            }
            try {
                kafkaSender.sendZimPaymentError(request.getVendorRef(), new PaymentErrorZim(request.getVendorRef(), e.getMessage(),
                        e instanceof ICEcashException ie ? ie.getErrorCode() : ErrorCodes.EC1101, Tool.currentDateTime()));
            } catch (Exception ex) {
                log.error("Cannot send payment error for vendorRef: {}, message: {}", request.getVendorRef(), ex.getMessage(), e);
            }
        }
    }

    private void sendRequestFurther(String providerName, String vendorRef, Object request) {
        switch (providerName) {
            case "POSB" -> kafkaSender.sendPosbService(vendorRef, request);
            case "FBC" -> kafkaSender.sendFbcService(vendorRef, request);
            case "FCB" -> kafkaSender.sendFcbService(vendorRef, request);
            case "mpesa" -> kafkaSender.sendMpesaService(vendorRef, request);
            case "ecocash" -> kafkaSender.sendEcocashService(vendorRef, request);
            default -> throw new ApiValidationException("Wrong 'bankName' field provided");
        }
    }

    public void handleOtpWaitingResult(PaymentOtpWaitingZim paymentOtpWaiting) {
        PaymentResponseZim paymentResponse = getOrCreatePaymentResponse(paymentOtpWaiting.getVendorRef());
        if (paymentResponse.getStatus() == PROCESSING) {
            loggerService.savePaymentResponse(paymentOtpWaiting.getVendorRef(), new PaymentResponseZim()
                    .setVendorRef(paymentResponse.getVendorRef())
                    .setStatus(ResponseStatus.OTP_WAITING)
                    .setMobile(paymentOtpWaiting.getMobile())
                    .setBankName(paymentOtpWaiting.getBankName()));
        }
    }

    public PaymentResponseZim handlePaymentOtp(PaymentOtpRequestZim otpRequest) {
        PaymentRequestZim paymentRequest = loggerService.getRequest(otpRequest.getVendorRef(), PaymentRequestZim.class);
        if (paymentRequest != null) {
            PaymentResponseZim paymentResponse = getOrCreatePaymentResponse(otpRequest.getVendorRef());
            if (paymentResponse.getStatus() == PROCESSING || paymentResponse.getStatus() == OTP_WAITING) {
                loggerService.savePaymentResponse(otpRequest.getVendorRef(), paymentResponse.setStatus(PROCESSING));
                sendRequestFurther(paymentRequest.getBankName(), otpRequest.getVendorRef(), new PaymentOtpRequestZim()
                        .setVendorRef(otpRequest.getVendorRef())
                        .setOtp(otpRequest.getOtp()));
                return paymentResponse;
            } else {
                return new PaymentResponseZim().setMessage("Cannot confirm finished payment. Payment status: " + paymentResponse.getStatus());
            }
        } else {
            throw new NotFoundException(String.format("PaymentResponse with vendorRef: '%s' does not exist", otpRequest.getVendorRef()), ErrorCodes.EC1103);
        }
    }

    public void handlePaymentSuccessResult(PaymentSuccessZim paymentSuccess) {
        PaymentRequestZim paymentRequest = loggerService.getRequest(paymentSuccess.getVendorRef(), PaymentRequestZim.class);
        if (paymentRequest == null) {
            throw new NotFoundException("Wrong logged payment request for vendorRef: " + paymentSuccess.getVendorRef(), ErrorCodes.EC1103);
        }
        PaymentResponseZim paymentResponse = getOrCreatePaymentResponse(paymentSuccess.getVendorRef());
        if (paymentResponse.getStatus() == PROCESSING || paymentResponse.getStatus() == OTP_WAITING) {
            paymentResponse.setExternalTransactionId(paymentSuccess.getTransactionId());
            dataService.milestoneCheck(paymentRequest, 11);
            if (zimApiProperties.isCheckSuccessfulPaymentTimeoutBeforeSp()) {
                checkPaymentTimeout(paymentRequest, ErrorCodes.EC1108);
            }
            try {
                if (zimApiProperties.isSpPollingEnabled()) {
                    spPollingForSuccessPayment(paymentRequest, paymentResponse);
                } else {
                    log.debug("  sp polling disabled, old way approve for vendorRef: {}", paymentSuccess.getVendorRef());
                    dataService.milestoneCheck(paymentRequest, 12);
                    dataService.approveLedgerPayment(paymentRequest, paymentResponse);
                    dataService.milestoneCheck(paymentRequest, 14);
                    if (zimApiProperties.isCheckSuccessfulPaymentTimeoutAfterSp()) {
                        checkPaymentTimeout(paymentRequest, ErrorCodes.EC1108);
                    }
                    dataService.milestoneCheck(paymentRequest, 21);
                    dataService.milestoneCheck(paymentRequest, 22);
                    loggerService.savePaymentResponse(paymentSuccess.getVendorRef(), paymentResponse.setStatus(SUCCESS));
                }
            } catch (Exception e) {
                throw new AfterLedgerException(e, paymentResponse.getSpResult());
            }
        } else {
            log.warn(String.format("Ignoring successful payment result for vendorRef: %s, due to payment status: %s",
                    paymentSuccess.getVendorRef(), paymentResponse.getStatus()));
        }
    }

    public void spPollingForSuccessPayment(PaymentRequestZim paymentRequest, PaymentResponseZim paymentResponse) {
        if (paymentRequest != null) {
            paymentResponse.incrementSpTries();
            try {
                try {
                    log.debug("  SP {}, maxTries={}, vendorRef: {}", paymentResponse.getSpTries() == 1 ? "call" : "polling attempt: " + paymentResponse.getSpTries(),
                            zimApiProperties.getSpMaxTries(), paymentRequest.getVendorRef());
                    if (paymentResponse.getSpTries() == 1 || !dataService.checkIfAlreadyApproved(paymentRequest, paymentResponse)) {
                        dataService.approveLedgerPayment(paymentRequest, paymentResponse.setLastSpTry(Instant.now()));
                    }
                    loggerService.savePaymentResponse(paymentRequest.getVendorRef(), paymentResponse.setStatus(SUCCESS));
                    log.debug("  Payment SUCCESS, vendorRef: {}", paymentRequest.getVendorRef());

                } catch (SpRetryRequireException | SpExecutionException e) {
                    log.warn("  SP error, {}: {}, spReslut: {}, vendorRef: {}", e.getClass().getSimpleName(), e.getMessage(), paymentResponse.getSpResult(), paymentRequest.getVendorRef());
                    if (paymentResponse.getSpTries() < zimApiProperties.getSpMaxTries()) {
                        loggerService.savePaymentResponse(paymentRequest.getVendorRef(), paymentResponse.setStatus(APPROVE_FAILED));
                    } else {            // all attempts failed
                        log.warn("  SP attempts exceeded, vendorRef: {}", paymentRequest.getVendorRef());
                        if (dataService.checkIfAlreadyApproved(paymentRequest, paymentResponse)) {
                            log.debug("  Payment SUCCESS, record was successfully added, vendorRef: {}", paymentRequest.getVendorRef());
                            loggerService.savePaymentResponse(paymentRequest.getVendorRef(), paymentResponse.setStatus(SUCCESS));
                        } else {
                            log.debug("  refunding.. vendorRef: {}", paymentRequest.getVendorRef());
                            refundAndFailPayment(paymentRequest, paymentResponse, ErrorCodes.EC1111, "Approving SP polling attempts exceeded");
                        }
                    }
                } catch (SpReturnedErrorException e) {
                    log.warn("  SP returned ERROR, refunding.. vendorRef: {}, spReslut: {}", paymentRequest.getVendorRef(), paymentResponse.getSpResult());
                    refundAndFailPayment(paymentRequest, paymentResponse, e.getErrorCode(), e.getMessage());
                }
            } catch (Exception e) {
                log.error("Unhandled exception in SP polling method, vendorRef: {}", paymentRequest.getVendorRef(), e);
            }
        } else {
            log.error("SP polling error, Request is absent for vendorRef: {}", paymentResponse.getVendorRef());
        }
    }

    private void refundAndFailPayment(PaymentRequestZim paymentRequest, PaymentResponseZim paymentResponse, String errorCode, String errorMessage) {
        boolean refundSuccess = false;
        try {
            String url = String.format(zimApiProperties.getReversalUrl(), zimApiProperties.getServiceHost().get(paymentRequest.getBankName()), paymentRequest.getVendorRef());
            log.debug("  refund url: {}", url);
            Map<?, ?> refundResult = restTemplate.postForObject(url, null, Map.class);
            refundSuccess = refundResult != null && refundResult.get("refunded") == Boolean.TRUE;
            log.debug("  refund response: {}, refundSuccess: {}, vendorRef: {}", refundResult, refundSuccess, paymentRequest.getVendorRef());
        } catch (Exception e) {
            log.warn("  Error sending refund request: {}: {}, vendorRef: {}", e.getClass().getSimpleName(), e.getMessage(), paymentRequest.getVendorRef());
        }
        loggerService.savePaymentResponse(paymentRequest.getVendorRef(), paymentResponse
                .setStatus(refundSuccess ? ERROR : ERROR_PARTIAL_PAYMENT)
                .setErrorCode(errorCode)
                .setMessage(errorMessage)
                .setSpResult(paymentResponse.getSpResult()));
        if (!refundSuccess) {
            log.warn("  refund failed, payment status: ERROR_PARTIAL_PAYMENT");
        }
    }

    public void updateStatus(PaymentResponseZim response, String status) {
        loggerService.savePaymentResponse(response.getVendorRef(), response.setStatus(SUCCESS));
    }

    public void handlePaymentError(PaymentErrorZim paymentError) {
        PaymentResponseZim paymentResponse = getOrCreatePaymentResponse(paymentError.getVendorRef());
        if (paymentResponse.getStatus() != ERROR || Objects.equals(paymentError.getErrorCode(), paymentResponse.getErrorCode())) {
            loggerService.savePaymentResponse(paymentError.getVendorRef(), paymentResponse
                    .setStatus(ERROR)
                    .setMessage(paymentError.getMessage())
                    .setErrorCode(paymentError.getErrorCode())
                    .setSpResult(paymentError.getSpResult())
                    .setTryingToRefund(Boolean.TRUE));
            PaymentRequestZim paymentRequest = loggerService.getRequest(paymentError.getVendorRef(), PaymentRequestZim.class);
            if (paymentRequest != null) {
                dataService.milestoneCheck(paymentRequest, 21);
                if (paymentError.getSpResult() != null) {
                    dataService.cancelPaymentApprovement(paymentRequest, paymentResponse, paymentError.getSpResult());
                } else {
                    dataService.failLedgerPayment(paymentRequest, paymentResponse);
                }
                try {
                    dataService.milestoneCheck(paymentRequest, 22);
                    loggerService.savePaymentResponse(paymentError.getVendorRef(), paymentResponse.setTryingToRefund(null));
                } catch (Exception e) {
                    log.error("Cannot save final error response: {}: {}, response: {}", e.getClass().getCanonicalName(), e.getMessage(), paymentResponse);
                }
            } else {
                log.warn("Skipping ledger. Cannot locate payment request for vendorRef: {}", paymentError.getVendorRef());
            }
        } else {
            log.error(String.format("Ignoring payment error result for vendorRef: %s, due to payment status: %s",
                    paymentError.getVendorRef(), paymentResponse.getStatus()));
        }
    }

    private void checkPaymentTimeout(PaymentRequestZim paymentRequest, String errorCode) {
        if (paymentRequest.getExpirationTime() != null && Instant.now().toEpochMilli() > paymentRequest.getExpirationTime()) {
            throw new ICEcashException("Payment expired", errorCode);
        }
    }

    private PaymentResponseZim getOrCreatePaymentResponse(String vendorRef) {
        PaymentResponseZim paymentResponse = getPaymentResponse(vendorRef);
        if (paymentResponse == null) {
            paymentResponse = new PaymentResponseZim()
                    .setVendorRef(vendorRef)
                    .setStatus(PROCESSING)
                    .setDate(LocalDateTime.now());
        }
        return paymentResponse;
    }

    public PaymentResponseZim getPaymentResponse(String vendorRef) {
        return loggerService.getResponse(vendorRef, PaymentResponseZim.class);
    }

    public PaymentRequestZim getPaymentRequest(String vendorRef) {
        return loggerService.getRequest(vendorRef, PaymentRequestZim.class);
    }

    public PaymentResponseZim makePaymentSync(PaymentRequestZim request) {
        addNewPayment(request);
        return waitForPaymentResponse(request.getVendorRef());
    }

    public PaymentResponseZim handlePaymentOtpSync(PaymentOtpRequestZim request) {
        handlePaymentOtp(request);
        return waitForPaymentResponse(request.getVendorRef());
    }

    private PaymentResponseZim waitForPaymentResponse(String vendorRef) {
        Instant start = Instant.now();
        while (Duration.between(start, Instant.now()).toMillis() <= zimApiProperties.getPaymentTimeout().toMillis()) {
            PaymentResponseZim response = getPaymentResponse(vendorRef);
            if (response != null && response.getStatus() != PROCESSING && response.getStatus() != APPROVE_FAILED && response.getTryingToRefund() == null) {
                return response;
            }
            Tool.sleep(100);
        }
        log.warn("  request timeout (>{}), vendorRef: {}", zimApiProperties.getPaymentTimeout(), vendorRef);
        return PaymentResponseZim.makeError(vendorRef, "Payment expired", ErrorCodes.EC1108, Tool.currentDateTime());
    }

    public Map<?, ?> performManualRefund(String service, String vendorRef, Boolean useExternalTransactionId) {
        PaymentResponseZim response = getPaymentResponse(vendorRef);
        log.debug("  last payment response: {}", response);
        String transactionId = useExternalTransactionId == Boolean.TRUE ? response.getExternalTransactionId() : vendorRef;
        String url = String.format(zimApiProperties.getReversalUrl(), zimApiProperties.getServiceHost().get(service), transactionId);
        log.debug("  calling: {}", url);
        Map<?, ?> result = restTemplate.postForObject(url, null, Map.class);
        log.debug("  service response: {}", result);
        if (result != null && result.get("refunded") == Boolean.TRUE) {
            log.debug("  calling zim api handler, prev sp result: {}", response != null ? objectMapper.convertValue(response.getSpResult(), Map.class) : null);
            handlePaymentError(new PaymentErrorZim(vendorRef, "Manual refund", ErrorCodes.EC1106,
                    Tool.currentDateTime(), response != null ? objectMapper.convertValue(response.getSpResult(), Map.class) : null));
        } else {
            log.debug("  not refunded, skipping zim api approvement cancelling");
        }
        return result;
    }

    public Map<?, ?> getServicePaymentDebugInfo(String service, String vendorRef, Boolean addScripts, Boolean addProps) {
        Map<String, Object> data = new LinkedHashMap<>();
        PaymentResponseZim response = getPaymentResponse(vendorRef);
        if (response == null) {
            response = loggerService.getResponseByExternalTransactionId(vendorRef, PaymentResponseZim.class);
            log.debug("  response is null, query response by externalTransactionId={}: {}", vendorRef, response);
            if (response != null) {
                log.debug("  using vendorRef: {} for externalTransactionId: {} to query debug info", response.getVendorRef(), vendorRef);
                vendorRef = response.getVendorRef();
            }
        }
        PaymentRequestZim request = getPaymentRequest(vendorRef);
        if (request == null) {
            log.debug("  request is null for vendorRef: {}", vendorRef);
        }
        data.put("_vendorRef", vendorRef);
        if (request != null && request.getExpirationTime() != null) {
            data.put("_requestTime", Instant.ofEpochMilli(request.getExpirationTime() - zimApiProperties.getPaymentTimeout().toMillis()));
            if (response != null && response.getDate() != null) {
                data.put("_responseTime", response.getDate());
            }
            data.put("_expiration", Instant.ofEpochMilli(request.getExpirationTime()));
        }
        data.put("_request", request);
        String url = String.format(zimApiProperties.getDebugInfoUrl(), zimApiProperties.getServiceHost().get(service), vendorRef);
        try {
            data.put("_handler", restTemplate.getForObject(url, Map.class));
        } catch (Exception e) {
            log.info("{}: '{}' occurred while getting debug info for vendorRef: {}", e.getClass().getCanonicalName(), e.getMessage(), vendorRef);
            data.put("_handler", null);
        }
        data.put("_response", response);
        if (addProps == Boolean.TRUE) {
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("zim-api", zimApiProperties);
            if (zimApiProperties.getPropsUrl() != null) {
                props.put(service, restTemplate.getForObject(String.format(zimApiProperties.getPropsUrl(), zimApiProperties.getServiceHost().get(service)), Map.class));
            }
            data.put("_props", props);
        }
        if (addScripts == Boolean.TRUE) {
            data.put("_scripts", dataService.getManualScripts(request, response));
        }
        return data;
    }
}

package cash.ice.onemoney.service.impl;

import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.utils.Tool;
import cash.ice.onemoney.config.OnemoneyProperties;
import cash.ice.onemoney.entity.OnemoneyPayment;
import cash.ice.onemoney.error.OnemoneyException;
import cash.ice.onemoney.repository.OnemoneyPaymentRepository;
import cash.ice.onemoney.service.OnemoneyClient;
import cash.ice.onemoney.service.OnemoneyPaymentService;
import cash.ice.onemoney.util.RequestBuilder;
import cash.ice.onemoney.util.StatusRequestBuilder;
import com.huawei.cps.cpsinterface.api_requestmgr.Request;
import com.huawei.cps.cpsinterface.api_requestmgr.Response;
import com.huawei.cps.cpsinterface.api_resultmgr.Result;
import com.huawei.cps.cpsinterface.result.Body;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.kafka.common.header.Headers;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static cash.ice.common.error.ErrorCodes.*;
import static cash.ice.onemoney.listener.OnemoneyServiceListener.SERVICE_HEADER;

@Service
@Profile(IceCashProfile.PROD)
@Slf4j
@RequiredArgsConstructor
public class OnemoneyPaymentServiceImpl implements OnemoneyPaymentService {
    protected final OnemoneyProperties onemoneyProperties;
    protected final OnemoneyPaymentRepository onemoneyPaymentRepository;
    private final OnemoneyClient onemoneyClient;
    private final KafkaSender kafkaSender;

    @Override
    public void processPayment(FeesData feesData, Headers headers) {
        OnemoneyPayment onemoneyPayment = new OnemoneyPayment()
                .setCreatedTime(Instant.now())
                .setVendorRef(feesData.getVendorRef())
                .setPendingPayment(feesData)
                .setKafkaHeaders(Tool.toKafkaHeaderKeys(headers));

        Request request = new RequestBuilder(onemoneyProperties)
                .addHeader("InitTrans_TwoPartPayment")
                .addInitiator(feesData.getPaymentRequest().getInitiator())
                .addIdentityReceiverParty()
                .addTransactionRequest(
                        feesData.getPaymentRequest().getAmount(),
                        feesData.getCurrencyCode())
                .build();
        onemoneyPayment.setRequest(request).setOriginatorConversationId(request.getHeader().getOriginatorConversationID());

        Response response = sendRequest(request, onemoneyPayment, onemoneyProperties.getPaymentUrl());
        log.debug("  Response: " + ToStringBuilder.reflectionToString(response, new RecursiveToStringStyle()));
        onemoneyPayment.setResponse(response)
                .setNeedCheckStatus(response.getBody().getResponseCode() == 0 ? Boolean.TRUE : null);

        if (response.getBody().getResponseCode() != 0) {
            throw new OnemoneyException(onemoneyPayment, "Response message: " +
                    response.getBody().getResponseDesc(), EC7006);
        }
        onemoneyPaymentRepository.save(onemoneyPayment);
    }

    @Override
    public void callbackResult(Result result) {
        String conversationID = result.getHeader().getOriginatorConversationID();
        String resultDesc = result.getBody().getResultDesc();
        OnemoneyPayment onemoneyPayment = onemoneyPaymentRepository.findByOriginatorConversationId(conversationID).orElse(null);
        if (onemoneyPayment == null) {
            onemoneyPayment = onemoneyPaymentRepository.findByRefundOriginatorConversationId(conversationID).orElse(null);
            if (onemoneyPayment != null) {
                if (result.getBody().getResultCode() != 0) {
                    log.warn("  Refund failed, message: {} for {}", resultDesc, onemoneyPayment.getVendorRef());
                    onemoneyPayment.setRefundFailed(Boolean.TRUE);
                }
                onemoneyPaymentRepository.save(onemoneyPayment.setRefundResult(result));
                return;
            } else {
                throw new OnemoneyException("No pending payment for OriginatorConversationID: " + conversationID, EC7001);
            }
        }
        Body.TransactionResult transactionResult = result.getBody().getTransactionResult();
        onemoneyPayment.setResult(result).setNeedCheckStatus(null).setNeedRecheckStatus(null)
                .setTransactionId(result.getBody().getResultCode() == 0 && transactionResult != null ?
                        transactionResult.getTransactionID() : null)
                .setResultMessage(result.getBody().getResultCode() + " " + resultDesc)
                .setUpdatedTime(Instant.now());

        if (!onemoneyPayment.isFinishedPayment()) {
            long durationSeconds = Duration.between(onemoneyPayment.getCreatedTime(), Instant.now()).getSeconds();
            if (durationSeconds < onemoneyProperties.getPaymentTimeout()) {
                if (result.getBody().getResultCode() == 0) {
                    completePayment(onemoneyPayment);
                } else {
                    throw new OnemoneyException(onemoneyPayment, "Received Failed: " + resultDesc, EC7002);
                }
            } else {            // payment expired
                log.debug("  Expired payment, for {}, onemoneyPayment: {}", onemoneyPayment.getVendorRef(), onemoneyPayment);
                throw new OnemoneyException(onemoneyPayment, "Timed out.", EC7007);
            }

        } else {
            log.info("  callback got result for finished payment, vendorRef: {}", onemoneyPayment.getVendorRef());
            throw new OnemoneyException(onemoneyPayment, onemoneyPayment.getErrorMessage(), onemoneyPayment.getErrorCode());
        }
    }

    @Override
    public void checkStatus(OnemoneyPayment payment, long durationSeconds, boolean recheck) {
        var statusRequest = new StatusRequestBuilder(onemoneyProperties)
                .addHeader("QueryTransactionStatus")
                .addOnemoneyInitiator()
                .addQueryTransactionStatusRequest(payment.getOriginatorConversationId())
                .addRemark()
                .build();
        payment.setStatusRequest(statusRequest);

        var statusResult = sendStatusRequest(statusRequest, payment, onemoneyProperties.getStatusUrl());
        var queryResult = statusResult.getBody().getQueryTransactionStatusResult();
        String transactionStatus = queryResult == null ? null : queryResult.getTransactionStatus();
        log.debug("  StatusResult: " + ToStringBuilder.reflectionToString(statusResult, new RecursiveToStringStyle()));
        if (recheck) {
            boolean success = statusResult.getBody().getResultCode() == 0 && "Completed".equals(transactionStatus);
            onemoneyPaymentRepository.save(payment.setRecheckResult(statusResult)
                    .setRecheckedSuccess(success ? Boolean.TRUE : null)
                    .setTransactionId(queryResult != null ? queryResult.getReceiptNumber() : payment.getTransactionId()));
            if (success) {
                log.info("  Recheck found SUCCESS result for timed out payment: {}, refunding", payment.getVendorRef());
                refund(payment.getVendorRef());
            }
            return;
        }
        payment.setStatusResult(statusResult).setResultMessage(String.format("%s %s (%s)",
                statusResult.getBody().getResultCode(), statusResult.getBody().getResultDesc(), transactionStatus));

        if (statusResult.getBody().getResultCode() != 0) {
            if (statusResult.getBody().getResultCode() == -1 && onemoneyProperties.isExpiredPaymentsRecheck()) {
                payment.setNeedRecheckStatus(Boolean.TRUE);
            }
            throw new OnemoneyException(payment, String.format("Status response: %s",
                    statusResult.getBody().getResultDesc()), EC7006);
        } else if (!"Completed".equals(transactionStatus)) {
            throw new OnemoneyException(payment, String.format("Status response: %s",
                    transactionStatus), EC7009);
        } else {
            payment.setTransactionId(queryResult.getReceiptNumber());
            if (durationSeconds > onemoneyProperties.getPaymentTimeout()) {
                throw new OnemoneyException(payment, "Timed out.", EC7007);
            } else {
                completePayment(payment);
            }
        }
    }

    @Override
    public long getStatusPollInitDelay(OnemoneyPayment payment) {
        return onemoneyProperties.getStatusPollInitDelay();
    }

    @Override
    public long getExpiredPaymentsRecheckAfterTime(OnemoneyPayment payment) {
        return onemoneyProperties.getExpiredPaymentsRecheckAfter();
    }

    private void completePayment(OnemoneyPayment onemoneyPayment) {
        try {
            log.debug("  Completed payment for " + onemoneyPayment.getVendorRef());
            onemoneyPayment.setFinishedPayment(true);
            kafkaSender.sendOnemoneySuccessPayment(onemoneyPayment.getVendorRef(), onemoneyPayment.getPendingPayment(),
                    Tool.toKafkaHeaders(onemoneyPayment.getKafkaHeaders()), SERVICE_HEADER);
            onemoneyPaymentRepository.save(onemoneyPayment);

        } catch (Exception e) {
            throw new OnemoneyException(onemoneyPayment, "Failed to perform after payment action: " +
                    e.getMessage(), EC7003);
        }
    }

    @Override
    public void failPayment(FeesData feesData, String errorCode, String message, Headers headers) {
        OnemoneyPayment onemoneyPayment = getOnemoneyPayment(feesData.getVendorRef());
        if (onemoneyPayment != null) {
            failPayment(onemoneyPayment, errorCode, message, headers);
        } else {
            log.debug("  Failed payment, reason: {}, errorCode: {}, for {}", message, errorCode, feesData.getVendorRef());
            kafkaSender.sendErrorPayment(feesData.getVendorRef(), new ErrorData(feesData, errorCode, message), headers);
        }
    }

    @Override
    public void failPayment(OnemoneyPayment payment, String errorCode, String message, Headers headers) {
        onemoneyPaymentRepository.save(payment
                .setErrorCode(errorCode)
                .setErrorMessage(message)
                .setFinishedPayment(true)
                .setUpdatedTime(Instant.now()));
        log.debug("  Failed payment, reason: {}, errorCode: {}, for {}", message, errorCode, payment.getVendorRef());
        kafkaSender.sendErrorPayment(payment.getVendorRef(),
                new ErrorData(payment.getPendingPayment(), errorCode, message), headers);
        if (payment.getTransactionId() != null) {
            refund(payment.getVendorRef());
        }
    }

    protected Response sendRequest(Request request, OnemoneyPayment onemoneyPayment, String paymentUrl) {
        try {
            return onemoneyClient.sendPayment(request, paymentUrl);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new OnemoneyException(onemoneyPayment, e.getMessage(), EC7005);
        }
    }

    protected com.huawei.cps.synccpsinterface.api_requestmgr.Result sendStatusRequest(com.huawei.cps.synccpsinterface.api_requestmgr.Request request, OnemoneyPayment onemoneyPayment, String statusUrl) {
        try {
            return onemoneyClient.sendStatusRequest(request, statusUrl);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new OnemoneyException(onemoneyPayment, e.getMessage(), EC7005);
        }
    }

    protected Response sendRefundRequest(Request request, OnemoneyPayment onemoneyPayment, String reversalUrl) {
        try {
            return onemoneyClient.sendRefundRequest(request, reversalUrl);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new OnemoneyException(onemoneyPayment, e.getMessage(), EC7005);
        }
    }

    private OnemoneyPayment getOnemoneyPayment(String vendorRef) {
        List<OnemoneyPayment> payments = onemoneyPaymentRepository.findByVendorRef(vendorRef);
        return payments.isEmpty() ? null : payments.get(payments.size() - 1);
    }

    @Override
    public void processRefund(ErrorData errorData) {
        String vendorRef = errorData.getFeesData().getVendorRef();
        log.info(">>>>>> OneMoney process refund for vendorRef: {}", vendorRef);
        refund(vendorRef);
    }

    protected void refund(String vendorRef) {
        OnemoneyPayment payment = getOnemoneyPayment(vendorRef);
        if (payment == null) {
            throw new OnemoneyException("No pending payment to refund for vendorRef: " + vendorRef, EC7008);
        } else if (payment.getTransactionId() == null) {
            throw new OnemoneyException("No TransactionID for payment to refund for vendorRef: " + vendorRef, EC7010);
        }
        if (payment.getRefundRequest() == null) {
            Request refundRequest = new RequestBuilder(onemoneyProperties)
                    .addHeader("RaiseDisputedTxnReversal")
                    .addOnemoneyInitiator()
                    .addRaiseDisputedTxnReversalRequest(
                            payment.getTransactionId(),
                            payment.getPendingPayment().getPaymentRequest().getAmount())
                    .addRemark("Reverse the Transaction.")
                    .build();
            payment.setRefundRequest(refundRequest)
                    .setRefundOriginatorConversationId(refundRequest.getHeader().getOriginatorConversationID());

            Response response = sendRefundRequest(refundRequest, payment, onemoneyProperties.getReversalUrl());
            log.info("  RefundResponse: " + ToStringBuilder.reflectionToString(response, new RecursiveToStringStyle()));
            if (response.getBody().getResponseCode() != 0) {
                log.warn("  Refund failed, message: {} for {}", response.getBody().getResponseDesc(), payment.getVendorRef());
                payment.setRefundFailed(Boolean.TRUE);
            }
            onemoneyPaymentRepository.save(payment.setRefundResponse(response));
        } else {
            log.debug("  refund, but already sent, for {}", payment.getVendorRef());
        }
    }
}

package cash.ice.api.listener;

import cash.ice.api.dto.moz.PaymentResponseMoz;
import cash.ice.api.entity.zim.PaymentStatus;
import cash.ice.api.repository.zim.PaymentRepository;
import cash.ice.api.service.LoggerService;
import cash.ice.api.service.Me60MozService;
import cash.ice.common.constant.PaymentMetaKey;
import cash.ice.common.dto.*;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.TransactionCode;
import cash.ice.sqldb.repository.PaymentLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static cash.ice.common.utils.Tool.headerKeys;
import static cash.ice.common.utils.Tool.headers;

/**
 * <p>{@code PaymentResponseListener} class listens to both <em>ice.cash.payment.SuccessTopic</em> and
 * <em>ice.cash.payment.ErrorTopic</em> kafka topics for success or error payment responses.
 * It checks if the response with the same {@code vendorRef} is already saved in MongoDB, and if not,
 * it saves the response in MongoDB. If the response is already exists in MongoDB, it's just skipped</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentResponseListener {
    private final LoggerService loggerService;
    private final KafkaSender kafkaSender;
    private final Me60MozService me60MozService;
    private final PaymentRepository paymentRepository;
    private final PaymentLineRepository paymentLineRepository;

    @KafkaListener(groupId = "${ice.cash.payment.group}", topics = {"${ice.cash.kafka.topic.success-payment}"})
    void listenToSuccessResponseTopic(ConsumerRecord<String, FeesData> rec) {
        try {
            log.debug(">> payment success ({}, headers: {}) {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                    rec.key(), headerKeys(rec.headers()), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
            PaymentResponse existingResponse = loggerService.getResponse(rec.key(), PaymentResponse.class);
            if (existingResponse == null || existingResponse.getStatus() == ResponseStatus.PROCESSING) {
                loggerService.savePaymentResponse(rec.key(), makeSuccessfulPaymentResponse(rec.key(), rec.value()));
                updatePaymentStatus(PaymentStatus.APPROVED, rec.value().getPaymentRequest(), rec.value().getTransactionId());
                kafkaSender.sendAfterSuccessPayment(rec.key(), rec.value());

            } else if (existingResponse.getStatus() == ResponseStatus.ERROR) {
                log.info("    payment error response already exist: {}, refunding new successful response", rec.key());
                throw new ICEcashException("Need refund for successful payment", ErrorCodes.EC1004);
            } else {
                log.warn("    payment successful response already exist: {}, ignoring new successful response", rec.key());
            }

        } catch (Exception e) {
            ErrorData error = new ErrorData(rec.value(), ErrorCodes.EC1004, e.getMessage());
            kafkaSender.sendErrorPayment(rec.key(), error, rec.headers());
        }
    }

    private PaymentResponse makeSuccessfulPaymentResponse(String vendorRef, FeesData feesData) {
        if (TransactionCode.TSF.equals(feesData.getPaymentRequest().getTx())) {
            Map<String, PaymentResponseMoz.BalanceResponse> accountBalances = me60MozService.getAccountsBalances(
                    feesData.getInitiatorEntityId(),
                    (List<Integer>) feesData.getPaymentRequest().getMeta().get(PaymentMetaKey.RequestBalanceAccountTypes));
            return PaymentResponseMoz.success(vendorRef, feesData.getTransactionId(), accountBalances, feesData.getBalance(),
                    feesData.getSubsidyAccountBalance(), feesData.getPrimaryMsisdn(), feesData.getInitiatorLocale());
        }
        return PaymentResponse.success(vendorRef, feesData.getTransactionId(), feesData.getBalance(),
                feesData.getPrimaryMsisdn(), feesData.getInitiatorLocale());
    }

    @KafkaListener(groupId = "${ice.cash.payment.group}", topics = {"${ice.cash.kafka.topic.sub-payment-result}"})
    void listenToSubResultResponseTopic(ConsumerRecord<String, Map<String, Object>> rec) {
        log.info(">> payment sub result ({}, headers: {}) {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                rec.key(), headerKeys(rec.headers()), "rec.value()", rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        loggerService.savePaymentResponse(rec.key(), PaymentResponse.subResult(rec.key(), rec.value()));
    }

    @KafkaListener(groupId = "${ice.cash.payment.group}", topics = {"${ice.cash.kafka.topic.error-payment}"})
    void listenToErrorsTopic(ErrorData errorData, ConsumerRecord<String, ErrorData> rec) {
        log.debug(">> payment error ({}, headers: {}) {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                rec.key(), headerKeys(rec.headers()), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        if (!loggerService.isResponseExist(rec.key())) {
            loggerService.savePaymentResponse(rec.key(),
                    PaymentResponse.error(rec.key(), errorData.getErrorCode(), errorData.isInternalError() ?
                            "Internal error: " + Tool.substrLast(4, errorData.getErrorCode()) : errorData.getMessage()));
            updatePaymentStatus(PaymentStatus.REJECTED, rec.value().getFeesData().getPaymentRequest(), null);
        } else {
            log.debug("  payment response already exist: {}, ignoring new error response", rec.key());
        }
    }

    @KafkaListener(groupId = "${ice.cash.payment.group}", topics = {"${ice.cash.kafka.topic.error-payment-dlt}"})
    void listenToErrorsDltTopic(ConsumerRecord<String, ?> rec, @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String message) {
        log.error(">> DLT: ({}) {}, partition: {}, offset: {}, timestamp: {}, topic: {}, message: {}",
                rec.key(), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), message);
    }

    private void updatePaymentStatus(PaymentStatus paymentStatus, PaymentRequest paymentRequest, String transactionId) {
        if (paymentRequest != null && paymentRequest.getMeta() != null) {
            Integer paymentId = (Integer) paymentRequest.getMeta().get("paymentId");
            if (paymentId != null) {
                paymentRepository.findById(paymentId).ifPresent(payment -> {
                    paymentRepository.save(payment.setStatus(paymentStatus));
                    Integer paymentLineId = (Integer) paymentRequest.getMeta().get("paymentLineId");
                    if (transactionId != null && paymentLineId != null) {
                        paymentLineRepository.findById(paymentLineId).ifPresent(paymentLine ->
                                paymentLineRepository.save(paymentLine.setTransactionId(Integer.valueOf(transactionId))));
                    }
                });
            }
        }
    }
}

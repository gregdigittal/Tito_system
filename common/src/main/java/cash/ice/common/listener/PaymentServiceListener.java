package cash.ice.common.listener;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.error.ICEcashWrongBalanceException;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.transaction.TransactionException;

import java.util.Objects;

import static cash.ice.common.utils.Tool.headerKeys;
import static cash.ice.common.utils.Tool.headers;

@Slf4j
public abstract class PaymentServiceListener {
    public static final String SERVICE_PREFIX = "ICEcash-";

    protected final PaymentService paymentService;
    protected final KafkaSender kafkaSender;

    protected PaymentServiceListener(PaymentService paymentService, KafkaSender kafkaSender) {
        this.paymentService = paymentService;
        this.kafkaSender = kafkaSender;
    }

    @KafkaHandler(isDefault = true)
    void listenToRequestTopicUnknown(ConsumerRecord<String, ?> rec) {
        log.error(">> {} unknown request ({}) {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                getClass().getSimpleName(), rec.key(), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
    }

    protected void listenToRequestTopic(FeesData feesData, ConsumerRecord<String, FeesData> rec, String errorCode) {
        log.debug(">> {} payment ({}, headers: {}) {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                getClass().getSimpleName(), rec.key(), headerKeys(rec.headers()), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        String vendorRef = rec.key();
        Objects.requireNonNull(vendorRef, "vendorRef must not be null!");
        try {
            paymentService.processPayment(feesData, rec.headers());
            try {
                sendSuccessPayment(vendorRef, feesData, rec.headers());
            } catch (Exception e) {
                log.error("[{}] exception for vendorRef: {}, message: {}, refunding", getClass().getSimpleName(), vendorRef, e.getMessage(), e);
                paymentService.processRefund(new ErrorData(feesData, errorCode, e.getMessage()));
                throw e;
            }

        } catch (TransactionException | DataAccessResourceFailureException e) {
            throw e;
        } catch (ICEcashWrongBalanceException e) {
            ErrorData error = new ErrorData(rec.value(), e.getErrorCode(), e.getMessage());
            kafkaSender.sendErrorPayment(vendorRef, error, rec.headers());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            kafkaSender.sendErrorPayment(vendorRef, new ErrorData(rec.value(), errorCode, e.getMessage()));
        }
    }

    protected abstract void sendSuccessPayment(String vendorRef, FeesData feesData, Headers headers);

    protected void listenToRequestDltTopic(ConsumerRecord<String, FeesData> rec, String message, String errorCode) {
        log.warn(">> DLT: {} payment ({}, headers: {}) {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                getClass().getSimpleName(), rec.key(), headerKeys(rec.headers()), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        ErrorData error = new ErrorData(rec.value(), errorCode, message);
        kafkaSender.sendErrorPayment(rec.key(), error, rec.headers());
    }

    protected void listenToErrorTopic(ErrorData errorData, ConsumerRecord<String, ErrorData> rec, String serviceHeader) {
        log.debug(">> {} error ({}, serviceHeader: {}, headers: {}) {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                getClass().getSimpleName(), rec.key(), serviceHeader, headerKeys(rec.headers()), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        if (serviceHeader != null) {
            log.info("{} performing refund for vendorRef: {}, {}", getClass().getSimpleName(), rec.key(), rec.value());
            paymentService.processRefund(errorData);
        }
    }
}

package cash.ice.fee.listener;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.service.KafkaSender;
import cash.ice.fee.service.FeeService;
import cash.ice.fee.service.TransactionLimitCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

import java.util.Objects;

import static cash.ice.common.utils.Tool.headerKeys;
import static cash.ice.common.utils.Tool.headers;

/**
 * <p>{@code FeePaymentListener} class listens to <em>ice.cash.payment.InputTopic</em> kafka topic for fees coming
 * from log-service. It takes fees and charges from MySQL database and forms {@code FeesData} with {@code FeeEntry}
 * list. Then it uses PaymentServiceSelector to select a payment service kafka topic, needed to process the request
 * according to initiator type provided with the request, and sends {@code FeesData} to it for the further processing.
 *
 * <p>{@code FeePaymentListener} also contains DLT (dead letter topic) <em>ice.cash.payment.InputTopic.DLT</em>
 * where the messages are coming in case of any exception while it's handling and all retries are failed. In this case
 * it immediately sends an error response to <em>ice.cash.payment.ErrorTopic</em>.</p>
 */
@Component
@Slf4j
@KafkaListener(groupId = "${ice.cash.fee.group}", topics = {"${ice.cash.kafka.topic.fee-service}"})
@RequiredArgsConstructor
public class FeeServiceListener {
    private final FeeService feeService;
    private final TransactionLimitCheckService transactionLimitCheckService;
    private final KafkaSender kafkaSender;

    @KafkaHandler(isDefault = true)
    void listenToRequestTopicUnknown(ConsumerRecord<String, ?> rec) {
        log.error(">> unknown request ({}) {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                rec.key(), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
    }

    @KafkaHandler
    void listenToPaymentTopic(PaymentRequest paymentRequest, ConsumerRecord<String, PaymentRequest> rec) {
        log.info(">> ({}) {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.key(), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        String vendorRef = rec.key();
        Objects.requireNonNull(vendorRef, "vendorRef must not be null!");
        try {
            FeesData feesData = feeService.process(paymentRequest);
            log.debug("  ({}) handled fees: {}, limits: {}, sending to payment selector",
                    rec.key(), feesData.getFeeEntries().size(), feesData.getLimitData() != null ? feesData.getLimitData().size() : 0);
            kafkaSender.sendPaymentSelector(vendorRef, feesData);

        } catch (TransactionException | DataAccessResourceFailureException e) {
            throw e;
        } catch (ICEcashException e) {
            log.warn("[{}], message: {}, code: {}", e.getClass().getSimpleName(), e.getMessage(), e.getErrorCode());
            kafkaSender.sendErrorPayment(vendorRef, new ErrorData(new FeesData().setVendorRef(vendorRef).setPaymentRequest(paymentRequest),
                    e.getErrorCode(), e.getMessage()).setInternalError(e.isInternalError()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            kafkaSender.sendErrorPayment(vendorRef, new ErrorData(new FeesData().setVendorRef(vendorRef).setPaymentRequest(paymentRequest),
                    ErrorCodes.EC3005, e.getMessage()).setInternalError(true));
        }
    }

    @KafkaListener(groupId = "${ice.cash.fee.group}", topics = {"${ice.cash.kafka.topic.fee-service-dlt}"})
    void listenToPaymentDltTopic(ConsumerRecord<String, PaymentRequest> rec, @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String message) {
        log.warn(">> DLT: ({}) {}, partition: {}, offset: {}, timestamp: {}, topic: {}",
                rec.key(), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic());
        ErrorData error = new ErrorData(new FeesData().setVendorRef(rec.key()), ErrorCodes.EC3005, message);
        kafkaSender.sendErrorPayment(rec.key(), error);
    }

    @KafkaListener(groupId = "${ice.cash.fee.group}", topics = {"${ice.cash.kafka.topic.error-payment}"})
    void listenToErrorsTopic(ErrorData errorData, ConsumerRecord<String, ErrorData> rec) {
        log.debug(">> {} error ({}, headers: {}) {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                getClass().getSimpleName(), rec.key(), headerKeys(rec.headers()), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        transactionLimitCheckService.rollbackLimitDataIfNeed(errorData.getFeesData());
    }
}

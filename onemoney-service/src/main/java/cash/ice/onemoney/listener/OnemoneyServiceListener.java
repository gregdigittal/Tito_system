package cash.ice.onemoney.listener;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.listener.PaymentServiceListener;
import cash.ice.onemoney.error.OnemoneyException;
import cash.ice.onemoney.service.OnemoneyPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static cash.ice.common.error.ErrorCodes.EC7004;
import static cash.ice.common.utils.Tool.headerKeys;
import static cash.ice.common.utils.Tool.headers;

@Component
@Slf4j
@KafkaListener(groupId = "${ice.cash.onemoney.group}", topics = {"${ice.cash.kafka.topic.onemoney-service}"})
@RequiredArgsConstructor
public class OnemoneyServiceListener {
    public static final String SERVICE_HEADER = PaymentServiceListener.SERVICE_PREFIX + "OneMoney";

    private final OnemoneyPaymentService onemoneyPaymentService;

    @KafkaHandler(isDefault = true)
    void listenToRequestTopicUnknown(ConsumerRecord<String, ?> rec) {
        log.error("Got unknown request (vendorRef: {}) request: {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                rec.key(), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
    }

    @KafkaHandler
    void listenToPaymentTopic(FeesData feesData, ConsumerRecord<String, FeesData> rec) {
        log.info("Got payment (vendorRef: {}, headers: {}) request: {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                rec.key(), headerKeys(rec.headers()), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        try {
            onemoneyPaymentService.processPayment(feesData, rec.headers());

        } catch (Exception e) {
            String errorCode = e instanceof OnemoneyException ? ((OnemoneyException) e).getErrorCode() : EC7004;
            if (e instanceof OnemoneyException && ((OnemoneyException) e).getOnemoneyPayment() != null) {
                onemoneyPaymentService.failPayment(((OnemoneyException) e).getOnemoneyPayment(),
                        errorCode, e.getMessage(), rec.headers());
            } else {
                onemoneyPaymentService.failPayment(feesData, errorCode, e.getMessage(), rec.headers());
                log.error(e.getMessage(), e);
            }
        }
    }

    @KafkaListener(groupId = "${ice.cash.onemoney.group}", topics = {"${ice.cash.kafka.topic.error-payment}"})
    void listenToErrorsTopic(ErrorData errorData, ConsumerRecord<String, ErrorData> rec,
                             @Header(name = SERVICE_HEADER, required = false) String serviceHeader) {
        log.debug("Got error (vendorRef: {}, serviceHeader: {}, headers: {}) request: {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                rec.key(), serviceHeader, headerKeys(rec.headers()), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        if (serviceHeader != null) {
            log.info("{} performing refund for vendorRef: {}, {}", getClass().getSimpleName(), rec.key(), rec.value());
            onemoneyPaymentService.processRefund(errorData);
        }
    }
}

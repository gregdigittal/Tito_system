package cash.ice.zim.api.listener;

import cash.ice.common.dto.zim.PaymentErrorZim;
import cash.ice.zim.api.service.ZimPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static cash.ice.common.utils.Tool.headers;

@Component
@RequiredArgsConstructor
@KafkaListener(groupId = "${ice.cash.zim.api.kafka-group}", topics = {"${ice.cash.kafka.topic.zim-payment-error}"})
@Slf4j
public class ZimPaymentErrorListener {
    private final ZimPaymentService paymentService;

    @KafkaHandler(isDefault = true)
    void listenToUnknownRequest(ConsumerRecord<String, ?> rec) {
        log.info(">> unknown request: {}, partition: {}, offset: {}, timestamp: {}, topic: {}, key: {}, headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), rec.key(), headers(rec.headers()));
    }

    @KafkaHandler
    void listenToPaymentError(PaymentErrorZim paymentError, ConsumerRecord<String, PaymentErrorZim> rec) {
        log.info(">> {}, partition: {}, offset: {}, timestamp: {}, topic: {}, key: {}, headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), rec.key(), headers(rec.headers()));
        paymentService.handlePaymentError(paymentError);
    }

    @KafkaListener(groupId = "${ice.cash.zim.api.kafka-group}", topics = {"${ice.cash.kafka.topic.zim-payment-error-dlt}"})
    void listenToRequestDltTopic(ConsumerRecord<String, PaymentErrorZim> rec,
                                 @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String message) {
        log.info(">> DLT: {}, partition: {}, offset: {}, timestamp: {}, topic: {}, key: {}, headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), rec.key(), headers(rec.headers()));
        log.error("Error got to DLT: {}", rec.value());
    }
}

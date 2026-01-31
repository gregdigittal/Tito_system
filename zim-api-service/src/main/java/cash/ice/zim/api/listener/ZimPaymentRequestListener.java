package cash.ice.zim.api.listener;

import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.zim.api.service.ZimPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static cash.ice.common.utils.Tool.headers;

@Component
@Slf4j
@RequiredArgsConstructor
public class ZimPaymentRequestListener {
    private final ZimPaymentService paymentService;

    @KafkaListener(groupId = "${ice.cash.zim.api.kafka-group}", topics = {"${ice.cash.kafka.topic.zim-payment-request}"})
    void listenToRequestTopic(ConsumerRecord<String, PaymentRequestZim> rec) {
        log.info(">> {}, partition: {}, offset: {}, timestamp: {}, topic: {}, key: {}, headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), rec.key(), headers(rec.headers()));
        paymentService.handleNewPayment(rec.value());
    }
}

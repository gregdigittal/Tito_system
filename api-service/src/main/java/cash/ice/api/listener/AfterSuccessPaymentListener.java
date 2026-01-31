package cash.ice.api.listener;

import cash.ice.api.service.TransactionStatisticsService;
import cash.ice.common.dto.fee.FeesData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static cash.ice.common.utils.Tool.headers;
import static cash.ice.sqldb.entity.TransactionCode.TSF;

/**
 * <p>{@code AfterSuccessPaymentListener} class listens to both <em>ice.cash.payment.AfterSuccessTopic</em> kafka topic,
 * it logic needed to be executed after successful payment, eg. payment statistics calculation.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AfterSuccessPaymentListener {
    private final TransactionStatisticsService transactionStatisticsService;

    @KafkaListener(groupId = "${ice.cash.payment.group}", topics = {"${ice.cash.kafka.topic.after-success-payment}"})
    void listenToAfterSuccessResponseTopic(ConsumerRecord<String, FeesData> rec) {
        try {
            if (TSF.equals(rec.value().getPaymentRequest().getTx())) {
                log.debug(">> after payment success ({}) {} partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                        rec.key(), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
                transactionStatisticsService.addPayment(rec.value());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @KafkaListener(groupId = "${ice.cash.payment.group}", topics = {"${ice.cash.kafka.topic.after-success-payment-dlt}"})
    void listenToAfterSuccessResponseDltTopic(ConsumerRecord<String, ?> rec, @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String message) {
        log.error(">> DLT: ({}) {}, partition: {}, offset: {}, timestamp: {}, topic: {}, message: {}",
                rec.key(), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), message);
    }
}

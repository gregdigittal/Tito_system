package cash.ice.ledger.listener;

import cash.ice.ledger.service.AccountBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static cash.ice.common.utils.Tool.headers;

@Component
@Slf4j
@RequiredArgsConstructor
public class BalanceAuditListener {
    private final AccountBalanceService accountBalanceService;

    @KafkaListener(groupId = "${ice.cash.ledger.group}", topics = {"${ice.cash.kafka.topic.balance-audit}"})
    void listenToBalanceAuditTopic(ConsumerRecord<String, Integer> rec) {
        log.debug(">> {} balance audit ({}) {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                getClass().getSimpleName(), rec.key(), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        accountBalanceService.checkBalanceCorrectness(rec.value());
    }
}

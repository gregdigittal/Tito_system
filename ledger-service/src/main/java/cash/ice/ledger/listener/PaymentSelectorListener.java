package cash.ice.ledger.listener;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.service.KafkaSender;
import cash.ice.ledger.service.AccountBalanceService;
import cash.ice.ledger.service.PaymentServiceSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

import java.util.Objects;

import static cash.ice.common.utils.Tool.headers;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentSelectorListener {
    private final AccountBalanceService accountBalanceService;
    private final PaymentServiceSelector paymentServiceSelector;
    private final KafkaSender kafkaSender;

    @KafkaListener(groupId = "${ice.cash.ledger.group}", topics = {"${ice.cash.kafka.topic.payment-selector}"})
    void listenToPaymentSelectorTopic(FeesData feesData, ConsumerRecord<String, FeesData> rec) {
        log.info(">> selector ({}) {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.key(), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        String vendorRef = rec.key();
        Objects.requireNonNull(vendorRef, "vendorRef must not be null!");
        try {
            accountBalanceService.checkAccountBalanceAffordability(feesData);
            paymentServiceSelector.selectAndSendToServiceTopic(feesData.getInitiatorTypeDescription(), vendorRef, feesData);

        } catch (TransactionException | DataAccessResourceFailureException e) {
            throw e;
        } catch (ICEcashException e) {
            log.warn(e.getMessage(), e);
            kafkaSender.sendErrorPayment(vendorRef, new ErrorData(rec.value(), e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            kafkaSender.sendErrorPayment(vendorRef, new ErrorData(rec.value(), ErrorCodes.EC4001, e.getMessage()));
        }
    }

    @KafkaListener(groupId = "${ice.cash.ledger.group}", topics = {"${ice.cash.kafka.topic.payment-selector-dlt}"})
    void listenToSelectorDltTopic(ConsumerRecord<String, FeesData> rec, @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String message) {
        log.warn(">> DLT: ({}) {}, partition: {}, offset: {}, timestamp: {}, topic: {}",
                rec.key(), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic());
        kafkaSender.sendErrorPayment(rec.key(), new ErrorData(rec.value(), ErrorCodes.EC4001, message));
    }
}

package cash.ice.ledger.listener;

import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.listener.PaymentServiceListener;
import cash.ice.common.service.KafkaSender;
import cash.ice.ledger.service.impl.LedgerPaymentService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(groupId = "${ice.cash.ledger.group}", topics = {"${ice.cash.kafka.topic.flexcube-ledger-service}"})
public class FlexcubeLedgerServiceListener extends PaymentServiceListener {

    public FlexcubeLedgerServiceListener(LedgerPaymentService paymentService, KafkaSender kafkaSender) {
        super(paymentService, kafkaSender);
    }

    @KafkaHandler
    void listenToLedgerTopic(FeesData feesData, ConsumerRecord<String, FeesData> rec) {
        super.listenToRequestTopic(feesData, rec, ErrorCodes.EC4003);
    }

    @KafkaListener(groupId = "${ice.cash.ledger.group}", topics = {"${ice.cash.kafka.topic.flexcube-ledger-service-dlt}"})
    void listenToLedgerDltTopic(ConsumerRecord<String, FeesData> rec, @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String message) {
        super.listenToRequestDltTopic(rec, message, ErrorCodes.EC4003);
    }

    @Override
    protected void sendSuccessPayment(String vendorRef, FeesData feesData, Headers headers) {
        kafkaSender.sendFlexcubeLedgerSuccessPayment(vendorRef, feesData, headers, LedgerServiceListener.SERVICE_HEADER);
    }
}

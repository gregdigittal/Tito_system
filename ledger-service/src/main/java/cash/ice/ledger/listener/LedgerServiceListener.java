package cash.ice.ledger.listener;

import cash.ice.common.dto.ErrorData;
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

/**
 * <p>{@code LedgerListener} class listens to the following kafka topics:<br/>
 * <ul><li><em>ice.cash.payment.LedgerTopic</em> for fees coming from fee-service and uses {@code LedgerPaymentService}
 * to process them. It creates and stores two transaction lines for every fee entry, and one transaction entity
 * to MongoDB. Then it sends {@code FeesData} to <em>ice.cash.payment.SuccessTopic</em> kafka topic, to save
 * success payment response, adding <em>'ICEcash-Ledger'</em> header.</li>
 * <li><em>ice.cash.payment.ErrorTopic</em> for errors. It checks if the message contains <em>'ICEcash-Ledger'</em>
 * header, and if it so, makes a refund. In this case it means that payment is done by {@code Ledger} payment service
 * but exception was thrown somewhere else.<br/><br/>
 *
 * <p>{@code LedgerListener} also contains DLT (dead letter topic) <em>ice.cash.payment.LedgerTopic.DLT</em>
 * where the messages are coming in case of any exception while it's handling and all retries are failed. In this case
 * it immediately sends an error to <em>ice.cash.payment.ErrorTopic</em>.</p>
 */
@Component
@KafkaListener(groupId = "${ice.cash.ledger.group}", topics = {"${ice.cash.kafka.topic.ledger-service}"})
public class LedgerServiceListener extends PaymentServiceListener {
    public static final String SERVICE_HEADER = SERVICE_PREFIX + "Ledger";

    public LedgerServiceListener(LedgerPaymentService paymentService, KafkaSender kafkaSender) {
        super(paymentService, kafkaSender);
    }

    @KafkaHandler
    void listenToLedgerTopic(FeesData feesData, ConsumerRecord<String, FeesData> rec) {
        super.listenToRequestTopic(feesData, rec, ErrorCodes.EC4003);
    }

    @KafkaListener(groupId = "${ice.cash.ledger.group}", topics = {"${ice.cash.kafka.topic.ledger-service-dlt}"})
    void listenToLedgerDltTopic(ConsumerRecord<String, FeesData> rec, @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String message) {
        super.listenToRequestDltTopic(rec, message, ErrorCodes.EC4003);
    }

    @Override
    protected void sendSuccessPayment(String vendorRef, FeesData feesData, Headers headers) {
        kafkaSender.sendSuccessPayment(vendorRef, feesData, headers, SERVICE_HEADER);
    }

    @KafkaListener(groupId = "${ice.cash.ledger.group}", topics = {"${ice.cash.kafka.topic.error-payment}"})
    void listenToErrorsTopic(ErrorData errorData, ConsumerRecord<String, ErrorData> rec,
                             @Header(name = SERVICE_HEADER, required = false) String serviceHeader) {
        super.listenToErrorTopic(errorData, rec, serviceHeader);
    }
}

package cash.ice.fee.listener;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.listener.PaymentServiceListener;
import cash.ice.common.service.KafkaSender;
import cash.ice.fee.service.impl.payment.ZimSwitchPaymentService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * <p>{@code ZimSwitchListener} class listens to the following kafka topics:<br/>
 * <ul><li><em>ice.cash.payment.ZimSwitchTopic</em> for fees coming from fee-service and uses {@code ZimSwitchPaymentService}
 * to process them. Then it sends {@code FeesData} to the next kafka topic, for the further processing, adding
 * <em>'ICEcash-ZimSwitch'</em> header.</li>
 * <li><em>ice.cash.payment.ErrorTopic</em> for errors. It checks if the message contains <em>'ICEcash-ZimSwitch'</em>
 * header, and if it so, makes a refund. In this case it means that payment is done by {@code ZimSwitch} payment service
 * but exception was thrown somewhere else.<br/><br/>
 *
 * <p>{@code ZimSwitchListener} also contains DLT (dead letter topic) <em>ice.cash.payment.ZimSwitchTopic.DLT</em>
 * where the messages are coming in case of any exception while it's handling and all retries are failed. In this case
 * it immediately sends an error to <em>ice.cash.payment.ErrorTopic</em>.</p>
 */
@Component
@KafkaListener(groupId = "${ice.cash.zim-switch.group}", topics = {"${ice.cash.kafka.topic.zim-switch-service}"})
public class ZimSwitchServiceListener extends PaymentServiceListener {
    public static final String SERVICE_HEADER = SERVICE_PREFIX + "ZimSwitch";

    public ZimSwitchServiceListener(ZimSwitchPaymentService paymentService, KafkaSender kafkaSender) {
        super(paymentService, kafkaSender);
    }

    @KafkaHandler
    void listenToZimSwitchTopic(FeesData feesData, ConsumerRecord<String, FeesData> rec) {
        super.listenToRequestTopic(feesData, rec, ErrorCodes.EC3017);
    }

    @KafkaListener(groupId = "${ice.cash.zim-switch.group}", topics = {"${ice.cash.kafka.topic.zim-switch-service-dlt}"})
    void listenToZimSwitchDltTopic(ConsumerRecord<String, FeesData> rec, @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String message) {
        super.listenToRequestDltTopic(rec, message, ErrorCodes.EC3017);
    }

    @Override
    protected void sendSuccessPayment(String vendorRef, FeesData feesData, Headers headers) {
        kafkaSender.sendZimSwitchSuccessPayment(vendorRef, feesData, headers, SERVICE_HEADER);
    }

    @KafkaListener(groupId = "${ice.cash.zim-switch.group}", topics = {"${ice.cash.kafka.topic.error-payment}"})
    void listenToErrorsTopic(ErrorData errorData, ConsumerRecord<String, ErrorData> rec,
                             @Header(name = SERVICE_HEADER, required = false) String serviceHeader) {
        super.listenToErrorTopic(errorData, rec, serviceHeader);
    }
}

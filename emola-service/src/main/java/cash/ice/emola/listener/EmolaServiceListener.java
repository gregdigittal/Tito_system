package cash.ice.emola.listener;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.listener.PaymentServiceListener;
import cash.ice.common.service.KafkaSender;
import cash.ice.emola.error.EmolaException;
import cash.ice.emola.service.EmolaPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static cash.ice.common.error.ErrorCodes.EC9101;
import static cash.ice.common.utils.Tool.headerKeys;
import static cash.ice.common.utils.Tool.headers;

/**
 * <p>{@code EmolaListener} class listens to the following kafka topics:<br/>
 * <ul><li><em>ice.cash.payment.EmolaTopic</em> for fees coming from fee-service and uses {@code EmolaPaymentService}
 * to process them. Then it sends {@code FeesData} to the next kafka topic, for the further processing, adding
 * <em>'ICEcash-Emola'</em> header.</li>
 * <li><em>ice.cash.payment.ErrorTopic</em> for errors. It checks if the message contains <em>'ICEcash-Emola'</em>
 * header, and if it so, makes a refund. In this case it means that payment is done by {@code Emola} payment service
 * but exception was thrown somewhere else.<br/><br/>
 *
 * <p>{@code EmolaListener} also contains DLT (dead letter topic) <em>ice.cash.payment.EmolaTopic.DLT</em>
 * where the messages are coming in case of any exception while it's handling and all retries are failed. In this case
 * it immediately sends an error to <em>ice.cash.payment.ErrorTopic</em>.</p>
 */
@Component
@KafkaListener(groupId = "${ice.cash.emola.group}", topics = {"${ice.cash.kafka.topic.emola-service}"})
@Slf4j
public class EmolaServiceListener extends PaymentServiceListener {
    public static final String SERVICE_HEADER = SERVICE_PREFIX + "Emola";

    public EmolaServiceListener(EmolaPaymentService paymentService, KafkaSender kafkaSender) {
        super(paymentService, kafkaSender);
    }

    @KafkaHandler
    void listenToEmolaTopic(FeesData feesData, ConsumerRecord<String, FeesData> rec) {
        log.debug("Got payment (vendorRef: {}, headers: {}) request: {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                rec.key(), headerKeys(rec.headers()), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        try {
            paymentService.processPayment(feesData, rec.headers());

        } catch (Exception e) {
            String errorCode = e instanceof EmolaException ? ((EmolaException) e).getErrorCode() : EC9101;
            if (e instanceof EmolaException) {
                ((EmolaPaymentService) paymentService).failPayment(((EmolaException) e).getEmolaPayment(),
                        feesData, errorCode, e.getMessage(), rec.headers());
            } else {
                ((EmolaPaymentService) paymentService).failPayment(feesData, errorCode, e.getMessage(), rec.headers());
                log.error(e.getMessage(), e);
            }
        }
    }

    @KafkaListener(groupId = "${ice.cash.emola.group}", topics = {"${ice.cash.kafka.topic.emola-service-dlt}"})
    void listenToEmolaDltTopic(ConsumerRecord<String, FeesData> rec, @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String message) {
        super.listenToRequestDltTopic(rec, message, EC9101);
    }

    @Override
    protected void sendSuccessPayment(String vendorRef, FeesData feesData, Headers headers) {
        kafkaSender.sendEmolaSuccessPayment(vendorRef, feesData, headers, SERVICE_HEADER);
    }

    @KafkaListener(groupId = "${ice.cash.emola.group}", topics = {"${ice.cash.kafka.topic.error-payment}"})
    void listenToErrorsTopic(ErrorData errorData, ConsumerRecord<String, ErrorData> rec,
                             @Header(name = SERVICE_HEADER, required = false) String serviceHeader) {
        super.listenToErrorTopic(errorData, rec, serviceHeader);
    }
}

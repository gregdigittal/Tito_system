package cash.ice.ecocash.listener;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.dto.zim.PaymentErrorZim;
import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.common.listener.PaymentServiceListener;
import cash.ice.common.service.KafkaSender;
import cash.ice.ecocash.dto.Payment;
import cash.ice.ecocash.entity.EcocashPayment;
import cash.ice.ecocash.service.EcocashPaymentService;
import error.EcocashException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static cash.ice.common.error.ErrorCodes.EC6001;
import static cash.ice.common.utils.Tool.headerKeys;
import static cash.ice.common.utils.Tool.headers;

/**
 * <p>{@code EcocashListener} class listens to the following kafka topics:<br/>
 * <ul><li><em>ice.cash.payment.EcocashTopic</em> for fees coming from fee-service and uses {@code EcocashPaymentService}
 * to process them. Then it sends {@code FeesData} to the next kafka topic, for the further processing, adding
 * <em>'ICEcash-Ecocash'</em> header.</li>
 * <li><em>ice.cash.payment.ErrorTopic</em> for errors. It checks if the message contains <em>'ICEcash-Ecocash'</em>
 * header, and if it so, makes a refund. In this case it means that payment is done by {@code Ecocash} payment service
 * but exception was thrown somewhere else.<br/><br/>
 *
 * <p>{@code EcocashListener} also contains DLT (dead letter topic) <em>ice.cash.payment.EcocashTopic.DLT</em>
 * where the messages are coming in case of any exception while it's handling and all retries are failed. In this case
 * it immediately sends an error to <em>ice.cash.payment.ErrorTopic</em>.</p>
 */
@Component
@KafkaListener(groupId = "${ice.cash.ecocash.group}", topics = {"${ice.cash.kafka.topic.ecocash-service}"})
@RequiredArgsConstructor
@Slf4j
public class EcocashServiceListener {
    public static final String SERVICE_HEADER = PaymentServiceListener.SERVICE_PREFIX + "Ecocash";

    private final EcocashPaymentService paymentService;
    private final KafkaSender kafkaSender;

    @KafkaHandler
    void feesDataHandler(FeesData feesData, ConsumerRecord<String, FeesData> rec) {
        log.debug(">> FeesData: {}, key: {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                rec.value(), rec.key(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        try {
            paymentService.processPayment(new Payment()
                    .setVendorRef(feesData.getVendorRef())
                    .setTx(feesData.getPaymentRequest().getTx())
                    .setCurrencyCode(feesData.getCurrencyCode())
                    .setInitiator(feesData.getPaymentRequest().getInitiator())
                    .setAmount(feesData.getPaymentRequest().getAmount())
                    .setPartnerId(feesData.getPaymentRequest().getPartnerId())
                    .setPendingRequest(feesData)
                    .setMetaData(feesData.getPaymentRequest().getMetaData()), rec.headers());

        } catch (EcocashException e) {
            paymentService.processError(e);
        }
    }

    @KafkaHandler
    void zimPaymentRequestHandler(PaymentRequestZim paymentRequest, ConsumerRecord<String, PaymentRequestZim> rec) {
        log.debug(">> PaymentRequestZim: {}, key: {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                rec.value(), rec.key(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        try {
            paymentService.processPayment(new Payment()
                    .setVendorRef(paymentRequest.getVendorRef())
                    .setTx(paymentRequest.getTransactionCode())
                    .setCurrencyCode(paymentRequest.getCurrencyCode())
                    .setInitiator(paymentRequest.getAccountNumber())
                    .setAmount(paymentRequest.getAmount())
                    .setPartnerId(paymentRequest.getPartnerId() != null ? paymentRequest.getPartnerId().toString() : "")
                    .setPendingRequest(paymentRequest)
                    .setMetaData(paymentRequest.getMetaData()), rec.headers());

        } catch (EcocashException e) {
            paymentService.processError(e);
        }
    }

    @KafkaListener(groupId = "${ice.cash.ecocash.group}", topics = {"${ice.cash.kafka.topic.ecocash-service-dlt}"})
    void listenToEcocashDltTopic(ConsumerRecord<String, Object> rec, @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String message) {
        log.warn(">> DLT: {}, key: {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                rec.value(), rec.key(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        switch (rec.value()) {
            case FeesData feesData -> paymentService.processError(new EcocashException(
                    new EcocashPayment().setVendorRef(rec.key()).setPendingPayment(feesData), message, EC6001));
            case PaymentRequestZim paymentRequest -> paymentService.processError(new EcocashException(
                    new EcocashPayment().setVendorRef(rec.key()).setPendingPayment(paymentRequest), message, EC6001));
            default -> log.warn("Error, unknown request type got to ecocash DLT topic: " + rec.value());
        }
    }

    @KafkaListener(groupId = "${ice.cash.ecocash.group}", topics = {"${ice.cash.kafka.topic.error-payment}", "${ice.cash.kafka.topic.zim-payment-error}"})
    void errorTopicHandler(ConsumerRecord<String, Object> rec,
                           @Header(name = SERVICE_HEADER, required = false) String serviceHeader) {
        log.debug(">> EcocashServiceListener error [{}], headers: {} (serviceHeader: {}), {}, partition: {}, offset: {}, timestamp: {}, topic: {}, all headers: {}",
                rec.key(), headerKeys(rec.headers()), serviceHeader != null, rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        if (serviceHeader != null) {
            switch (rec.value()) {
                case ErrorData errorData -> paymentService.refund(errorData.getFeesData().getVendorRef());
                case PaymentErrorZim errorData -> paymentService.refund(errorData.getVendorRef());
                default ->
                        log.warn("Error, unknown request type got to ErrorTopic, {}, value: {}", rec.value().getClass(), rec.value());
            }
        }
    }
}

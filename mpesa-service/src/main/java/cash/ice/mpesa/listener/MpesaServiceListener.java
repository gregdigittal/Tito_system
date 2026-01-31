package cash.ice.mpesa.listener;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.dto.zim.PaymentErrorZim;
import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.common.dto.zim.PaymentSuccessZim;
import cash.ice.common.listener.PaymentServiceListener;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.utils.Tool;
import cash.ice.mpesa.dto.Payment;
import cash.ice.mpesa.entity.MpesaPayment;
import cash.ice.mpesa.error.MpesaException;
import cash.ice.mpesa.service.MpesaPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static cash.ice.common.error.ErrorCodes.EC9005;
import static cash.ice.common.utils.Tool.headerKeys;
import static cash.ice.common.utils.Tool.headers;

/**
 * <p>{@code MpesaListener} class listens to the following kafka topics:<br/>
 * <ul><li><em>ice.cash.payment.MpesaTopic</em> for fees coming from fee-service and uses {@code MpesaPaymentService}
 * to process them. Then it sends {@code FeesData} to the next kafka topic, for the further processing, adding
 * <em>'ICEcash-Mpesa'</em> header.</li>
 * <li><em>ice.cash.payment.ErrorTopic</em> for errors. It checks if the message contains <em>'ICEcash-Mpesa'</em>
 * header, and if it so, makes a refund. In this case it means that payment is done by {@code Mpesa} payment service
 * but exception was thrown somewhere else.<br/><br/>
 *
 * <p>{@code MpesaListener} also contains DLT (dead letter topic) <em>ice.cash.payment.MpesaTopic.DLT</em>
 * where the messages are coming in case of any exception while it's handling and all retries are failed. In this case
 * it immediately sends an error to <em>ice.cash.payment.ErrorTopic</em>.</p>
 */
@Component
@KafkaListener(groupId = "${ice.cash.mpesa.group}", topics = {"${ice.cash.kafka.topic.mpesa-service}"})
@RequiredArgsConstructor
@Slf4j
public class MpesaServiceListener {
    public static final String SERVICE_HEADER = PaymentServiceListener.SERVICE_PREFIX + "Mpesa";
    public static final String MPESA = "MPESA";

    private final MpesaPaymentService paymentService;
    private final KafkaSender kafkaSender;

    @KafkaHandler
    void feesDataHandler(FeesData feesData, ConsumerRecord<String, FeesData> rec) {
        log.debug(">> FeesData: {}, key: {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                rec.value(), rec.key(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        try {
            paymentService.processPayment(new Payment()
                    .setVendorRef(feesData.getVendorRef())
                    .setPaymentType("MPI".equals(feesData.getPaymentRequest().getTx()) ? Payment.Type.Inbound :
                            "MPO".equals(feesData.getPaymentRequest().getTx()) ? Payment.Type.Outbound : null)
                    .setMsisdn(feesData.getPaymentRequest().getInitiator())
                    .setAmount(feesData.getPaymentRequest().getAmount())
                    .setMetaData(feesData.getPaymentRequest().getMetaData()));
            kafkaSender.sendMpesaSuccessPayment(feesData.getVendorRef(), feesData, rec.headers(), SERVICE_HEADER);

        } catch (MpesaException e) {
            paymentService.processError(e);
            kafkaSender.sendErrorPayment(feesData.getVendorRef(), new ErrorData(feesData, e.getErrorCode(), e.getMessage()), rec.headers());
        }
    }

    @KafkaHandler
    void zimPaymentRequestHandler(PaymentRequestZim paymentRequest, ConsumerRecord<String, PaymentRequestZim> rec) {
        log.debug(">> PaymentRequestZim: {}, key: {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                rec.value(), rec.key(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        try {
            MpesaPayment mpesaPayment = paymentService.processPayment(new Payment()
                    .setVendorRef(paymentRequest.getVendorRef())
//                    .setPaymentType(paymentRequest.getTx())                               // todo
                    .setPaymentType(Payment.Type.Inbound)
                    .setMsisdn(paymentRequest.getAccountNumber())
                    .setAmount(paymentRequest.getAmount())
                    .setMetaData(paymentRequest.getMetaData()));
            kafkaSender.sendZimPaymentResult(paymentRequest.getVendorRef(), new PaymentSuccessZim(
                    paymentRequest.getVendorRef(), MPESA, mpesaPayment.getTransactionId()), rec.headers(), SERVICE_HEADER);

        } catch (MpesaException e) {
            paymentService.processError(e);
            kafkaSender.sendZimPaymentError(e.getVendorRef(), new PaymentErrorZim(e.getVendorRef(), e.getMessage(), e.getErrorCode(), Tool.currentDateTime()));
        }
    }

    @KafkaListener(groupId = "${ice.cash.mpesa.group}", topics = {"${ice.cash.kafka.topic.mpesa-service-dlt}"})
    void requestDltHandler(ConsumerRecord<String, Object> rec, @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String message) {
        log.warn(">> DLT: {}, key: {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                rec.value(), rec.key(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        switch (rec.value()) {
            case FeesData feesData -> {
                paymentService.processError(new MpesaException(rec.key(), message, EC9005));
                kafkaSender.sendErrorPayment(feesData.getVendorRef(), new ErrorData(feesData, EC9005, message), rec.headers());
            }
            case PaymentRequestZim paymentRequest -> {
                paymentService.processError(new MpesaException(new MpesaPayment().setVendorRef(rec.key()), message, EC9005));
                kafkaSender.sendZimPaymentError(paymentRequest.getVendorRef(), new PaymentErrorZim(rec.key(), message, EC9005, Tool.currentDateTime()));
            }
            default -> log.warn("Error, unknown request type got to mpesa DLT topic: " + rec.value());
        }
    }

    @KafkaListener(groupId = "${ice.cash.mpesa.group}", topics = {"${ice.cash.kafka.topic.error-payment}", "${ice.cash.kafka.topic.zim-payment-error}"})
    void errorTopicHandler(ConsumerRecord<String, Object> rec,
                           @Header(name = SERVICE_HEADER, required = false) String serviceHeader) {
        log.debug(">> MpesaServiceListener error [{}], headers: {} (serviceHeader: {}), {}, partition: {}, offset: {}, timestamp: {}, topic: {}, all headers: {}",
                rec.key(), headerKeys(rec.headers()), serviceHeader != null, rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        if (serviceHeader != null) {
            switch (rec.value()) {
                case ErrorData errorData -> paymentService.processRefund(errorData.getFeesData().getVendorRef());
                case PaymentErrorZim errorData -> paymentService.processRefund(errorData.getVendorRef());
                default ->
                        log.warn("Error, unknown request type got to ErrorTopic, {}, value: {}", rec.value().getClass(), rec.value());
            }
        }
    }
}

package cash.ice.paygo.listener;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.listener.PaymentServiceListener;
import cash.ice.common.service.KafkaSender;
import cash.ice.paygo.error.PaygoException;
import cash.ice.common.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static cash.ice.common.error.ErrorCodes.EC5004;
import static cash.ice.common.utils.Tool.headerKeys;
import static cash.ice.common.utils.Tool.headers;

@Component
@Slf4j
@KafkaListener(groupId = "${ice.cash.paygo.group}", topics = {"${ice.cash.kafka.topic.paygo-service}"})
@RequiredArgsConstructor
public class PaygoServiceListener {
    public static final String SERVICE_HEADER = PaymentServiceListener.SERVICE_PREFIX + "PayGo";

    private final PaymentService paymentService;
    private final KafkaSender kafkaSender;

    @KafkaHandler(isDefault = true)
    void listenToRequestTopicUnknown(ConsumerRecord<String, ?> rec) {
        log.error("Got unknown request (vendorRef: {}) request: {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                rec.key(), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
    }

    @KafkaHandler
    void listenToPaymentTopic(FeesData feesData, ConsumerRecord<String, FeesData> rec) {
        log.info("Got PaymentRequest (vendorRef: {}) request: {}; partition: {}; offset: {}; timestamp: {}; topic: {}; headers: {}",
                rec.key(), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        try {
            paymentService.processPayment(feesData, rec.headers());

        } catch (PaygoException e) {
            String errorCode = e.getCause() instanceof ICEcashException ? ((ICEcashException) e.getCause()).getErrorCode() : EC5004;
            String paygoData = String.format(" (DeviceReference: %s, PayGoId: %s)", e.getDeviceReference(), e.getPayGoId());
            kafkaSender.sendErrorPayment(feesData.getVendorRef(),
                    new ErrorData(feesData, errorCode, e.getMessage() + paygoData),
                    rec.headers());
            log.error(e.getMessage(), e);
        }
    }

    @KafkaListener(groupId = "${ice.cash.paygo.group}", topics = {"${ice.cash.kafka.topic.error-payment}"})
    void listenToErrorsTopic(ErrorData errorData, ConsumerRecord<String, ErrorData> rec,
                             @Header(name = SERVICE_HEADER, required = false) String serviceHeader) {
        log.debug("Got error (vendorRef: {}, serviceHeader: {}, headers: {}) request: {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                rec.key(), serviceHeader, headerKeys(rec.headers()), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        if (serviceHeader != null) {
            log.info("{} performing refund for vendorRef: {}, {}", getClass().getSimpleName(), rec.key(), rec.value());
            paymentService.processRefund(errorData);
        }
    }
}

package cash.ice.zim.api.listener;

import cash.ice.common.dto.zim.PaymentErrorZim;
import cash.ice.common.dto.zim.PaymentOtpWaitingZim;
import cash.ice.common.dto.zim.PaymentSuccessZim;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.utils.Tool;
import cash.ice.zim.api.error.AfterLedgerException;
import cash.ice.zim.api.service.ZimPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static cash.ice.common.utils.Tool.headers;

@Component
@RequiredArgsConstructor
@KafkaListener(groupId = "${ice.cash.zim.api.kafka-group}", topics = {"${ice.cash.kafka.topic.zim-payment-result}"})
@Slf4j
public class ZimPaymentResultListener {
    private final ZimPaymentService paymentService;
    private final KafkaSender kafkaSender;

    @KafkaHandler(isDefault = true)
    void listenToUnknownResult(ConsumerRecord<String, ?> rec) {
        log.error(">> unknown result: {}, partition: {}, offset: {}, timestamp: {}, topic: {}, key: {}, headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), rec.key(), headers(rec.headers()));
    }

    @KafkaHandler
    void listenToOtpWaiting(PaymentOtpWaitingZim paymentOtpWaiting, ConsumerRecord<String, PaymentOtpWaitingZim> rec) {
        log.info(">> {}, partition: {}, offset: {}, timestamp: {}, topic: {}, key: {}, headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), rec.key(), headers(rec.headers()));
        paymentService.handleOtpWaitingResult(paymentOtpWaiting);
    }

    @KafkaHandler
    void listenToPaymentSuccess(PaymentSuccessZim paymentSuccess, ConsumerRecord<String, PaymentSuccessZim> rec) {
        log.info(">> {}, partition: {}, offset: {}, timestamp: {}, topic: {}, key: {}, headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), rec.key(), headers(rec.headers()));
        try {
            paymentService.handlePaymentSuccessResult(paymentSuccess);
        } catch (Throwable e) {
            Object spResult = null;
            if (e instanceof AfterLedgerException ale) {
                spResult = ale.getSpResult();
                e = ale.getCause();
            }
            log.warn("  Failed payment (need refund), {}: {}, spResult: {}", e.getClass().getCanonicalName(), e.getMessage(), spResult);
            try {
                kafkaSender.sendZimPaymentError(rec.key(), new PaymentErrorZim(paymentSuccess.getVendorRef(), e.getMessage(),
                        e instanceof ICEcashException ie ? ie.getErrorCode() : ErrorCodes.EC1101, Tool.currentDateTime(), spResult), rec.headers());
            } catch (Exception ex) {
                log.error("Cannot send payment error for vendorRef: {}, message: {}", paymentSuccess.getVendorRef(), ex.getMessage(), e);
            }
        }
    }

    @KafkaListener(groupId = "${ice.cash.zim.api.kafka-group}", topics = {"${ice.cash.kafka.topic.zim-payment-result-dlt}"})
    void listenToResultDltTopic(ConsumerRecord<String, Object> rec,
                                @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String message) {
        log.info(">> DLT: {}, partition: {}, offset: {}, timestamp: {}, topic: {}, key: {}, headers: {}",
                rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), rec.key(), headers(rec.headers()));
        PaymentErrorZim error = new PaymentErrorZim().setVendorRef(rec.key()).setMessage(message);
        kafkaSender.sendZimPaymentError(rec.key(), error);
    }
}

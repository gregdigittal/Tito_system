package cash.ice.fbc.listener;

import cash.ice.common.dto.zim.PaymentErrorZim;
import cash.ice.common.dto.zim.PaymentOtpRequestZim;
import cash.ice.common.dto.zim.PaymentRefundRequestZim;
import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.fbc.error.FbcException;
import cash.ice.fbc.service.FbcPaymentService;
import cash.ice.fbc.service.impl.FbcPaymentServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static cash.ice.common.utils.Tool.headerKeys;
import static cash.ice.common.utils.Tool.headers;

@Component
@RequiredArgsConstructor
@KafkaListener(groupId = "${ice.cash.fbc.group}", topics = {"${ice.cash.kafka.topic.fbc-service}"})
@Slf4j
public class FbcServiceListener {
    private final FbcPaymentService paymentService;

    @KafkaHandler
    void receivePayment(PaymentRequestZim paymentRequest, ConsumerRecord<String, PaymentRequestZim> rec) {
        log.debug(">> PaymentRequestZim: {}, key: {}", paymentRequest, rec.key());
        try {
            paymentService.processPayment(paymentRequest);
        } catch (FbcException e) {
            paymentService.processError(e);
        }
    }

    @KafkaHandler
    void receivePaymentOtp(PaymentOtpRequestZim otpRequest, ConsumerRecord<String, PaymentOtpRequestZim> rec) {
        log.debug(">> PaymentOtpRequestZim: {}, key: {}", otpRequest, rec.key());
        try {
            paymentService.processOtp(otpRequest, rec.headers());
        } catch (FbcException e) {
            paymentService.processError(e);
        }
    }

    @KafkaHandler
    void receivePaymentRefund(PaymentRefundRequestZim refundRequest, ConsumerRecord<String, PaymentRefundRequestZim> rec) {
        log.debug(">> PaymentRefundRequestZim: {}, key: {}", refundRequest, rec.key());
        paymentService.processRefund(refundRequest.getVendorRef());
    }

    @KafkaListener(groupId = "${ice.cash.fbc.group}", topics = {"${ice.cash.kafka.topic.fbc-service-dlt}"})
    void paymentDlt(ConsumerRecord<String, Object> rec, @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String message) {
        log.debug(">> DLT: {}, key: {}", rec.value(), rec.key());
        paymentService.processError(new FbcException(rec.key(), message));
    }

    @KafkaListener(groupId = "${ice.cash.fbc.group}", topics = {"${ice.cash.kafka.topic.zim-payment-error}"})
    void paymentErrorTopic(PaymentErrorZim errorData, ConsumerRecord<String, PaymentErrorZim> rec,
                           @Header(name = FbcPaymentServiceImpl.SERVICE_HEADER, required = false) String serviceHeader) {
        log.debug(">> {} error ({}, serviceHeader: {}, headers: {}) {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                getClass().getSimpleName(), rec.key(), serviceHeader, headerKeys(rec.headers()), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        if (serviceHeader != null) {
            log.info("{} performing refund for vendorRef: {}, {}", getClass().getSimpleName(), rec.key(), rec.value());
            paymentService.processRefund(errorData.getVendorRef());
        }
    }
}

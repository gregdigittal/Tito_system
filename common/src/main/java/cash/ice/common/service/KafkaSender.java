package cash.ice.common.service;

import cash.ice.common.config.KafkaTopicsProperties;
import cash.ice.common.dto.EmailRequest;
import cash.ice.common.dto.SmsMessage;
import cash.ice.common.listener.PaymentServiceListener;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static cash.ice.common.utils.Tool.headers;

@Component
@RequiredArgsConstructor
@Accessors(chain = true)
@Slf4j
public class KafkaSender {
    private final KafkaTopicsProperties topics;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPaymentRequest(String vendorRef, Object value) {
        sendMessage(topics.getPaymentRequest(), vendorRef, value);
    }

    public void sendFeeService(String vendorRef, Object value) {
        sendMessage(topics.getFeeService(), vendorRef, value);
    }

    public void sendPaymentSelector(String vendorRef, Object value) {
        sendMessage(topics.getPaymentSelector(), vendorRef, value);
    }

    public void sendSubPaymentResult(String vendorRef, Object value) {
        sendMessage(topics.getSubPaymentResult(), vendorRef, value);
    }

    public void sendPaygoService(String vendorRef, Object value) {
        sendMessage(topics.getPaygoService(), vendorRef, value);
    }

    public void sendPaygoSuccessPayment(String vendorRef, Object value, Headers headers, String serviceHeader) {
        sendMessageWithHeaders(topics.getPaygoSuccessPayment(), vendorRef, value, headers, serviceHeader);
    }

    public void sendEcocashService(String vendorRef, Object value) {
        sendMessage(topics.getEcocashService(), vendorRef, value);
    }

    public void sendEcocashSuccessPayment(String vendorRef, Object value, Headers headers, String serviceHeader) {
        sendMessageWithHeaders(topics.getEcocashSuccessPayment(), vendorRef, value, headers, serviceHeader);
    }

    public void sendOnemoneyService(String vendorRef, Object value) {
        sendMessage(topics.getOnemoneyService(), vendorRef, value);
    }

    public void sendOnemoneySuccessPayment(String vendorRef, Object value, Headers headers, String serviceHeader) {
        sendMessageWithHeaders(topics.getOnemoneySuccessPayment(), vendorRef, value, headers, serviceHeader);
    }

    public void sendFlexcubeLedgerService(String vendorRef, Object value) {
        sendMessage(topics.getFlexcubeLedgerService(), vendorRef, value);
    }

    public void sendFlexcubeLedgerSuccessPayment(String vendorRef, Object value, Headers headers, String serviceHeader) {       // RtgsService
        sendMessageWithHeaders(topics.getFlexcubeLedgerSuccessPayment(), vendorRef, value, headers, serviceHeader);
    }

    public void sendRtgsService(String vendorRef, Object value) {
        sendMessage(topics.getRtgsService(), vendorRef, value);
    }

    public void sendRtgsSuccessPayment(String vendorRef, Object value, Headers headers, String serviceHeader) {
        sendMessageWithHeaders(topics.getRtgsSuccessPayment(), vendorRef, value, headers, serviceHeader);
    }

    public void sendZipitService(String vendorRef, Object value) {
        sendMessage(topics.getZipitService(), vendorRef, value);
    }

    public void sendZipitSuccessPayment(String vendorRef, Object value, Headers headers, String serviceHeader) {
        sendMessageWithHeaders(topics.getZipitSuccessPayment(), vendorRef, value, headers, serviceHeader);
    }

    public void sendZimSwitchService(String vendorRef, Object value) {
        sendMessage(topics.getZimSwitchService(), vendorRef, value);
    }

    public void sendZimSwitchSuccessPayment(String vendorRef, Object value, Headers headers, String serviceHeader) {
        sendMessageWithHeaders(topics.getZimSwitchSuccessPayment(), vendorRef, value, headers, serviceHeader);
    }

    public void sendMpesaService(String vendorRef, Object value) {
        sendMessage(topics.getMpesaService(), vendorRef, value);
    }

    public void sendMpesaSuccessPayment(String vendorRef, Object value, Headers headers, String serviceHeader) {
        sendMessageWithHeaders(topics.getMpesaSuccessPayment(), vendorRef, value, headers, serviceHeader);
    }

    public void sendEmolaService(String vendorRef, Object value) {
        sendMessage(topics.getEmolaService(), vendorRef, value);
    }

    public void sendEmolaSuccessPayment(String vendorRef, Object value, Headers headers, String serviceHeader) {
        sendMessageWithHeaders(topics.getEmolaSuccessPayment(), vendorRef, value, headers, serviceHeader);
    }

    public void sendZimPaymentRequest(String vendorRef, Object value) {
        sendMessage(topics.getZimPaymentRequest(), vendorRef, value);
    }

    public void sendZimPaymentResult(String vendorRef, Object value) {
        sendMessage(topics.getZimPaymentResult(), vendorRef, value);
    }

    public void sendZimPaymentResult(String vendorRef, Object value, Headers headers, String serviceHeader) {
        sendMessageWithHeaders(topics.getZimPaymentResult(), vendorRef, value, headers, serviceHeader);
    }

    public void sendZimPaymentError(String vendorRef, Object value) {
        sendMessage(topics.getZimPaymentError(), vendorRef, value);
    }

    public void sendZimPaymentError(String vendorRef, Object value, Headers headers) {
        sendMessageWithHeaders(topics.getZimPaymentError(), vendorRef, value, headers);
    }

    public void sendPosbService(String vendorRef, Object value) {
        sendMessage(topics.getPosbService(), vendorRef, value);
    }

    public void sendFbcService(String vendorRef, Object value) {
        sendMessage(topics.getFbcService(), vendorRef, value);
    }

    public void sendFcbService(String vendorRef, Object value) {
        sendMessage(topics.getFcbService(), vendorRef, value);
    }

    public void sendLedgerService(String vendorRef, Object value) {
        sendMessage(topics.getLedgerService(), vendorRef, value);
    }

    public void sendSuccessPayment(String vendorRef, Object value, Headers headers, String serviceHeader) {
        sendMessageWithHeaders(topics.getSuccessPayment(), vendorRef, value, headers, serviceHeader);
    }

    public void sendErrorPayment(String vendorRef, Object value) {
        sendMessage(topics.getErrorPayment(), vendorRef, value);
    }

    public void sendErrorPayment(String vendorRef, Object value, Headers headers) {
        sendMessageWithHeaders(topics.getErrorPayment(), vendorRef, value, headers);
    }

    public void sendAfterSuccessPayment(String vendorRef, Object value) {
        sendMessage(topics.getAfterSuccessPayment(), vendorRef, value);
    }

    public void sendBalanceAudit(Integer accountId) {
        sendMessage(topics.getBalanceAudit(), null, accountId);
    }

    public void sendEmailNotification(EmailRequest emailRequest) {
        sendMessage(topics.getEmailNotification(), null, emailRequest);
    }

    public void sendSmsNotification(SmsMessage smsMessage) {
        sendMessage(topics.getSmsNotification(), null, smsMessage);
    }

    public void sendInvalidateCache(List<String> cacheNames) {
        sendMessage(topics.getInvalidateCache(), null, cacheNames);
    }

    private void sendMessage(String topicName, String vendorRef, Object value) {
        ProducerRecord<String, Object> rec = new ProducerRecord<>(topicName, vendorRef, value);
        kafkaTemplate.send(rec);
        log.debug("  Sent to [{}] [{}] {}",
                rec.topic(), rec.key(), rec.value() == null ? null : rec.value().getClass().getSimpleName());
    }

    private void sendMessageWithHeaders(String topicName, String vendorRef, Object value, Headers headers) {
        sendMessageWithHeaders(topicName, vendorRef, value, headers, null);
    }

    private void sendMessageWithHeaders(String topicName, String vendorRef, Object value, Headers headers, String addHeader) {
        ProducerRecord<String, Object> rec = new ProducerRecord<>(topicName, vendorRef, value);
        if (headers != null) {
            Arrays.stream(headers.toArray()).filter(header -> header.key().startsWith(PaymentServiceListener.SERVICE_PREFIX))
                    .forEach(header -> rec.headers().add(header));
        }
        if (addHeader != null) {
            rec.headers().add(new RecordHeader(addHeader, "".getBytes()));
        }
        kafkaTemplate.send(rec);
        log.debug("  Sent to [{}] [{}] {},{} headers: {}", rec.topic(), rec.key(), rec.value() == null ? null : rec.value().getClass().getSimpleName(),
                addHeader != null ? " AddHeader: " + addHeader : "", headers(rec.headers()));
    }
}

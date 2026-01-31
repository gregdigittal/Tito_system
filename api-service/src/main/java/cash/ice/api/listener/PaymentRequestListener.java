package cash.ice.api.listener;

import cash.ice.api.service.LoggerService;
import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.service.KafkaSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static cash.ice.common.utils.Tool.headers;

/**
 * <p>{@code PaymentRequestListener} class listens to <em>ice.cash.payment.RequestTopic</em> kafka topic for payment
 * requests. It checks if the request with the same {@code vendorRef} is already saved in MongoDB, and if not, it saves
 * the request in MongoDB and sends it to <em>ice.cash.payment.InputTopic</em> kafka topic to be handled by
 * fee-service. Thus only unique requests will be sent for the further handling. If the request is already exists in
 * MongoDB, it's just skipped</p>
 *
 * <p>{@code PaymentRequestListener} also contains DLT (dead letter topic) <em>ice.cash.payment.RequestTopic.DLT</em>
 * where the messages are coming in case of any exception while it's handling and all retries are failed. In this case
 * it immediately saves an error response to MongoDB.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentRequestListener {
    private final LoggerService loggerService;
    private final KafkaSender kafkaSender;

    @KafkaListener(groupId = "${ice.cash.payment.group}", topics = {"${ice.cash.kafka.topic.payment-request}"})
    void listenToRequestTopic(ConsumerRecord<String, PaymentRequest> rec) {
        log.info(">> ({}) {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                rec.key(), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        if (loggerService.savePaymentRequest(rec.key(), rec.value())) {
            if (rec.value().getAmount().compareTo(BigDecimal.ZERO) == 0) {
                log.debug("  Amount is 0, simulating SUCCESS response");
                loggerService.savePaymentResponse(rec.key(), PaymentResponse.success(rec.key(), null, null, null, null));
            } else {
                kafkaSender.sendFeeService(rec.key(), rec.value());
            }
        }
    }

    @KafkaListener(groupId = "${ice.cash.payment.group}", topics = {"${ice.cash.kafka.topic.payment-request-dlt}"})
    void listenToRequestDltTopic(ConsumerRecord<String, PaymentRequest> rec,
                                 @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String message) {
        log.warn(">> DLT: ({}) {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                rec.key(), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        ErrorData error = new ErrorData(new FeesData().setPaymentRequest(rec.value()).setVendorRef(rec.key()), ErrorCodes.EC1003, message);
        kafkaSender.sendErrorPayment(rec.key(), error);
    }
}

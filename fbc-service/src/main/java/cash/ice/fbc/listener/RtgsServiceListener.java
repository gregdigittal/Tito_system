package cash.ice.fbc.listener;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.listener.PaymentServiceListener;
import cash.ice.common.service.KafkaSender;
import cash.ice.fbc.error.FlexcubeException;
import cash.ice.fbc.error.FlexcubeTimeoutException;
import cash.ice.fbc.service.FlexcubeService;
import cash.ice.fbc.service.impl.FlexcubeServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static cash.ice.common.error.ErrorCodes.EC8001;
import static cash.ice.common.utils.Tool.headerKeys;
import static cash.ice.common.utils.Tool.headers;

@Component
@KafkaListener(groupId = "${ice.cash.fbc.group}", topics = {"${ice.cash.kafka.topic.rtgs-service}"})
@Slf4j
public class RtgsServiceListener extends PaymentServiceListener {
    public static final String SERVICE_HEADER = SERVICE_PREFIX + "Rtgs";

    public RtgsServiceListener(FlexcubeServiceImpl paymentService, KafkaSender kafkaSender) {
        super(paymentService, kafkaSender);
    }

    @KafkaHandler
    void listenToRtgsTopic(FeesData feesData, ConsumerRecord<String, FeesData> rec) {
        log.debug("Got payment (vendorRef: {}, headers: {}) request: {}, partition: {}, offset: {}, timestamp: {}, topic: {}, headers: {}",
                rec.key(), headerKeys(rec.headers()), rec.value(), rec.partition(), rec.offset(), rec.timestamp(), rec.topic(), headers(rec.headers()));
        try {
            paymentService.processPayment(feesData, rec.headers());

        } catch (FlexcubeTimeoutException e) {
            log.info("Timeout for vendorRef: {}, referenceId: {}", feesData.getVendorRef(), e.getReferenceId());

        } catch (Exception e) {
            String errorCode = e instanceof FlexcubeException ? ((FlexcubeException) e).getErrorCode() : EC8001;
            if (e instanceof FlexcubeException && ((FlexcubeException) e).getFlexcubePayment() != null) {
                ((FlexcubeService) paymentService).failPayment(((FlexcubeException) e).getFlexcubePayment(),
                        errorCode, e.getMessage(), rec.headers());
            } else {
                ((FlexcubeService) paymentService).failPayment(feesData, errorCode, e.getMessage(), rec.headers());
                log.error(e.getMessage(), e);
            }
        }
    }

    @KafkaListener(groupId = "${ice.cash.fbc.group}", topics = {"${ice.cash.kafka.topic.rtgs-service-dlt}"})
    void listenToFbcDltTopic(ConsumerRecord<String, FeesData> rec, @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String message) {
        super.listenToRequestDltTopic(rec, message, ErrorCodes.EC6001);
    }

    @Override
    protected void sendSuccessPayment(String vendorRef, FeesData feesData, Headers headers) {
        kafkaSender.sendRtgsSuccessPayment(vendorRef, feesData, headers, SERVICE_HEADER);
    }

    @KafkaListener(groupId = "${ice.cash.fbc.group}", topics = {"${ice.cash.kafka.topic.error-payment}"})
    void listenToErrorsTopic(ErrorData errorData, ConsumerRecord<String, ErrorData> rec,
                             @Header(name = SERVICE_HEADER, required = false) String serviceHeader) {
        super.listenToErrorTopic(errorData, rec, serviceHeader);
    }
}

package cash.ice.posb.kafka;

import cash.ice.posb.error.PosbRetryableException;
import cash.ice.posb.service.PosbPaymentService;
import cash.ice.common.dto.zim.PaymentErrorZim;
import cash.ice.common.dto.zim.PaymentOtpRequestZim;
import cash.ice.common.dto.zim.PaymentRefundRequestZim;
import cash.ice.common.dto.zim.PaymentRequestZim;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.configuration.kafka.annotation.*;
import io.micronaut.configuration.kafka.exceptions.KafkaListenerException;
import io.micronaut.configuration.kafka.exceptions.KafkaListenerExceptionHandler;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.messaging.annotation.MessageHeader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static cash.ice.posb.service.impl.PosbPaymentServiceImpl.SERVICE_HEADER;
import static cash.ice.posb.service.impl.PosbPaymentServiceImpl.TYPE_ID_HEADER;

@RequiredArgsConstructor
@KafkaListener(
        value = "${ice.cash.kafka.group}",
        offsetReset = OffsetReset.EARLIEST,
        errorStrategy = @ErrorStrategy(
                value = ErrorStrategyValue.RETRY_EXPONENTIALLY_ON_ERROR,
                retryDelay = "${ice.cash.kafka.retry-delay}",
                retryCountValue = "${ice.cash.kafka.retry-count}",
                exceptionTypes = {PosbRetryableException.class}
        )
)
@Slf4j
public class PosbPaymentListener implements KafkaListenerExceptionHandler {
    private final PosbPaymentService paymentService;
    private final ObjectMapper objectMapper;

    @Topic("${ice.cash.kafka.request-topic}")
    public void receivePayment(@KafkaKey String id,
                               Map<String, Object> request,
                               @MessageHeader("__TypeId__") String typeHeader) {

        log.info(">> {} {}, key: {}", typeHeader, request, id);
        switch (typeHeader) {
            case "cash.ice.common.dto.zim.PaymentRequestZim" ->
                    paymentService.processPayment(objectMapper.convertValue(request, PaymentRequestZim.class));

            case "cash.ice.common.dto.zim.PaymentOtpRequestZim" ->
                    paymentService.processOtp(objectMapper.convertValue(request, PaymentOtpRequestZim.class));

            case "cash.ice.common.dto.zim.PaymentRefundRequestZim" ->
                    paymentService.processRefund(objectMapper.convertValue(request, PaymentRefundRequestZim.class));

            default -> throw new RuntimeException("Unknown request type: " + typeHeader);
        }
    }

    @Override
    public void handle(KafkaListenerException e) {
        paymentService.processError(e.getCause());
    }

    @Topic("${ice.cash.kafka.error-topic}")
    public void receiveError(@KafkaKey String id,
                             PaymentErrorZim errorData,
                             @MessageHeader(name = TYPE_ID_HEADER) @Nullable String typeHeader,
                             @MessageHeader(name = SERVICE_HEADER) @Nullable String serviceHeader) {

        log.info(">> {}, key: {}, serviceHeader: {}, typeHeader: {}", errorData, id, serviceHeader, typeHeader);
        if (serviceHeader != null) {
            log.info("{} performing refund for vendorRef: {}, {}", getClass().getSimpleName(), id, errorData);
            paymentService.processRefund(new PaymentRefundRequestZim(errorData.getVendorRef()));
        }
    }
}

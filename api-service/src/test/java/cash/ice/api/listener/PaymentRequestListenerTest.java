package cash.ice.api.listener;

import cash.ice.api.service.LoggerService;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.service.KafkaSender;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRequestListenerTest {
    public static final String VENDOR_REF = "testVendor";
    private static final String INPUT_TOPIC_NAME = "inputTopic";

    @Mock
    private LoggerService loggerService;
    @Mock
    private KafkaSender kafkaSender;
    @Mock
    private MongoTemplate mongoTemplate;

    @Test
    void testListenToRequestTopic() {
        PaymentRequestListener listener = new PaymentRequestListener(loggerService, kafkaSender);
        PaymentRequest testRequest = new PaymentRequest().setAmount(BigDecimal.TEN);
        ConsumerRecord<String, PaymentRequest> record = new ConsumerRecord<>(INPUT_TOPIC_NAME, 1, 1, VENDOR_REF, testRequest);

        when(loggerService.savePaymentRequest(VENDOR_REF, testRequest)).thenReturn(true);
        listener.listenToRequestTopic(record);
        verify(kafkaSender).sendFeeService(VENDOR_REF, testRequest);
    }

    @Test
    void testListenToRequestTopicIfDuplicate() {
        PaymentRequestListener listener = new PaymentRequestListener(loggerService, kafkaSender);
        PaymentRequest testRequest = new PaymentRequest().setAmount(BigDecimal.TEN);
        ConsumerRecord<String, PaymentRequest> record = new ConsumerRecord<>(INPUT_TOPIC_NAME, 1, 1, VENDOR_REF, testRequest);

        when(loggerService.savePaymentRequest(VENDOR_REF, testRequest)).thenReturn(false);
        listener.listenToRequestTopic(record);
        verify(mongoTemplate, never()).save(any(), any());
    }
}
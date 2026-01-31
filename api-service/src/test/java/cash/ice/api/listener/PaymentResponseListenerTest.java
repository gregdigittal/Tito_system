package cash.ice.api.listener;

import cash.ice.api.repository.zim.PaymentRepository;
import cash.ice.api.service.LoggerService;
import cash.ice.api.service.Me60MozService;
import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;
import cash.ice.common.dto.ResponseStatus;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.service.KafkaSender;
import cash.ice.sqldb.repository.PaymentLineRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentResponseListenerTest {
    public static final String VENDOR_REF = "testVendor";

    @Mock
    private LoggerService loggerService;
    @Mock
    private KafkaSender kafkaSender;
    @Mock
    private Me60MozService me60MozService;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentLineRepository paymentLineRepository;
    @Captor
    private ArgumentCaptor<PaymentResponse> paymentResponseArgumentCaptor;
    @InjectMocks
    private PaymentResponseListener listener;

    @Test
    void listenToSuccessResponseTopic() {
        String transactionId = "3";
        BigDecimal balance = new BigDecimal("100.0");
        ConsumerRecord<String, FeesData> record = new ConsumerRecord<>("topic", 1, 1, VENDOR_REF,
                new FeesData().setTransactionId(transactionId).setBalance(balance).setPaymentRequest(new PaymentRequest()));

        listener.listenToSuccessResponseTopic(record);

        verify(loggerService).savePaymentResponse(eq(VENDOR_REF), paymentResponseArgumentCaptor.capture());
        PaymentResponse actualPaymentResponse = paymentResponseArgumentCaptor.getValue();
        assertThat(actualPaymentResponse.getVendorRef()).isEqualTo(VENDOR_REF);
        assertThat(actualPaymentResponse.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(actualPaymentResponse.getTransactionId()).isEqualTo(transactionId);
        assertThat(actualPaymentResponse.getBalance()).isEqualTo(balance);
    }

    @Test
    void listenToErrorResponseTopic() {
        String errorCode = "someErrorCode";
        String errorMessage = "someErrorMessage";
        ErrorData errorData = new ErrorData(new FeesData().setPaymentRequest(new PaymentRequest()), errorCode, errorMessage);
        ConsumerRecord<String, ErrorData> record = new ConsumerRecord<>("topic", 1, 1, VENDOR_REF, errorData);

        listener.listenToErrorsTopic(errorData, record);

        verify(loggerService).savePaymentResponse(eq(VENDOR_REF), paymentResponseArgumentCaptor.capture());
        PaymentResponse actualPaymentResponse = paymentResponseArgumentCaptor.getValue();
        assertThat(actualPaymentResponse.getVendorRef()).isEqualTo(VENDOR_REF);
        assertThat(actualPaymentResponse.getStatus()).isEqualTo(ResponseStatus.ERROR);
        assertThat(actualPaymentResponse.getTransactionId()).isNull();
        assertThat(actualPaymentResponse.getBalance()).isNull();
    }
}
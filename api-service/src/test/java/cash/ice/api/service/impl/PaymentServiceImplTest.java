package cash.ice.api.service.impl;

import cash.ice.api.errors.ApiPaymentException;
import cash.ice.api.service.LoggerService;
import cash.ice.api.service.TicketService;
import cash.ice.common.constant.PaymentMetaKey;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;
import cash.ice.api.dto.moz.PaymentResponseMoz;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.service.KafkaSender;
import cash.ice.sqldb.entity.TransactionCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
    private static final String TRANSACTION_ID = "10";
    private static final String VENDOR_REF = "DEV112345678901";
    private static final String VENDOR_REF2 = "DEV112345678902";
    private static final String VENDOR_REF3 = "DEV112345678903";

    @Mock
    private LoggerService loggerService;
    @Mock
    private TicketService ticketService;
    @Mock
    private KafkaSender kafkaSender;

    private PaymentServiceImpl service;

    @BeforeEach
    void init() {
        service = new PaymentServiceImpl(loggerService, ticketService, kafkaSender);
    }

    @Test
    void testAddPayment() {
        PaymentRequest paymentRequest = createPaymentRequest();
        service.addPayment(paymentRequest);
        verify(kafkaSender).sendPaymentRequest(VENDOR_REF, paymentRequest);
    }

    @Test
    void testAddPaymentException() {
        PaymentRequest paymentRequest = createPaymentRequest();
        doThrow(new RuntimeException()).when(kafkaSender).sendPaymentRequest(VENDOR_REF, paymentRequest);

        ApiPaymentException exception = assertThrows(ApiPaymentException.class,
                () -> service.addPayment(paymentRequest));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.EC1002);
    }

    @Test
    void testGetPaymentRequest() {
        PaymentRequest paymentRequest = createPaymentRequest();
        when(loggerService.getRequest(VENDOR_REF, PaymentRequest.class)).thenReturn(paymentRequest);
        PaymentRequest actualPaymentRequest = service.getPaymentRequest(VENDOR_REF);
        assertThat(actualPaymentRequest).isEqualTo(paymentRequest);
    }

    @Test
    void testGetPaymentResponse() {
        PaymentResponse paymentResponse = PaymentResponse.processing(VENDOR_REF);
        when(loggerService.getResponse(VENDOR_REF, PaymentResponse.class)).thenReturn(paymentResponse);
        PaymentResponse actualPaymentResponse = service.getPaymentResponse(VENDOR_REF);
        assertThat(actualPaymentResponse).isEqualTo(paymentResponse);
    }

    @Test
    void testGetPaymentResponseWithNoRequest() {
        PaymentResponse actualPaymentResponse = service.getPaymentResponse(VENDOR_REF);
        assertThat(actualPaymentResponse).isNotNull();
        assertThat(actualPaymentResponse.getErrorCode()).isEqualTo(ErrorCodes.EC1005);
    }

    @Test
    void testMakePaymentSynchronous() {
        PaymentRequest request = new PaymentRequest().setVendorRef(VENDOR_REF).setTx(TransactionCode.TSF);
        PaymentResponseMoz response = PaymentResponseMoz.success(VENDOR_REF, TRANSACTION_ID, null, new BigDecimal("1000"), new BigDecimal("10"), null, null);

        when(loggerService.getResponse(VENDOR_REF, PaymentResponse.class)).thenReturn(null);
        when(loggerService.waitForResponse(eq(VENDOR_REF), any(), eq(Duration.ofSeconds(60)))).thenReturn(response);

        PaymentResponse actualPaymentResponse = service.makePaymentSynchronous(request);
        assertThat(actualPaymentResponse).isEqualTo(response);
        verify(kafkaSender).sendPaymentRequest(VENDOR_REF, request);
    }

    @Test
    void testMakeBulkPayment() {
        PaymentRequest request1 = new PaymentRequest().setVendorRef(VENDOR_REF);        // new payment
        PaymentRequest request2 = new PaymentRequest().setVendorRef(VENDOR_REF2);       // existing successful payment
        PaymentRequest request3 = new PaymentRequest().setVendorRef(VENDOR_REF3);       // existing error payment
        PaymentResponseMoz response1 = PaymentResponseMoz.success(VENDOR_REF, TRANSACTION_ID, null, new BigDecimal("1000"), new BigDecimal("10"), null, null);
        PaymentResponseMoz response2 = PaymentResponseMoz.success(VENDOR_REF2, "tr2", null, new BigDecimal("100"), new BigDecimal("5"), null, null);
        PaymentResponse responseInitial3 = PaymentResponseMoz.error(VENDOR_REF3, "someErrorCode", "someErrorMessage");
        PaymentResponse response3 = PaymentResponseMoz.error(VENDOR_REF3, "someErrorCode", "someErrorMessage");

        when(loggerService.getResponse(VENDOR_REF, PaymentResponse.class))
                .thenReturn(null);
        when(loggerService.getResponse(VENDOR_REF2, PaymentResponse.class))
                .thenReturn(response2.setDate(LocalDateTime.of(2023, 3, 10, 14, 0)));    // existing earlier SUCCESS response
        when(loggerService.getResponse(VENDOR_REF3, PaymentResponse.class))
                .thenReturn(responseInitial3.setDate(LocalDateTime.of(2023, 3, 10, 14, 30)));      // existing earlier ERROR response
        when(loggerService.waitForResponse(eq(VENDOR_REF), any(), any()))
                .thenReturn(response1.setDate(LocalDateTime.of(2023, 3, 10, 15, 30)));
        when(loggerService.waitForResponse(eq(VENDOR_REF3), any(), any()))
                .thenReturn(response3.setDate(LocalDateTime.of(2023, 3, 10, 15, 31)));

        List<PaymentResponse> actualResponse = service.makeBulkPaymentSynchronous(List.of(request1, request2, request3), false, Duration.ofSeconds(60), (req, res) -> {
        });
        assertThat(actualResponse).isEqualTo(List.of(response2, response1, response3));

        verify(kafkaSender).sendPaymentRequest(VENDOR_REF, request1);
        verify(kafkaSender).sendPaymentRequest(VENDOR_REF3, request3);
        assertThat(request1.getMetaData().get(PaymentMetaKey.OffloadTransaction)).isNotNull();
        assertThat(request3.getMetaData().get(PaymentMetaKey.OffloadTransaction)).isNotNull();
    }

    private PaymentRequest createPaymentRequest() {
        return new PaymentRequest().setVendorRef(VENDOR_REF).setCurrency("ZWL");
    }
}
package cash.ice.zim.api.service;

import cash.ice.common.dto.zim.PaymentErrorZim;
import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.common.dto.zim.PaymentSuccessZim;
import cash.ice.common.service.KafkaSender;
import cash.ice.zim.api.config.ZimApiProperties;
import cash.ice.zim.api.dto.PaymentResponseZim;
import cash.ice.zim.api.dto.ResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cash.ice.zim.api.dto.ResponseStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZimPaymentServiceTest {
    private static final String VENDOR_REF = "testVendorRef";
    private static final String TRANSACTION_ID = "87p1xjr88jft";

    @Mock
    private KafkaSender kafkaSender;
    @Mock
    private ZimLoggerService loggerService;
    @Mock
    private ZimDataService dataService;
    @Mock
    private ZimApiProperties zimApiProperties;
    @InjectMocks
    private ZimPaymentService service;

    @Test
    void testAddNewPayment() {
        PaymentRequestZim request = new PaymentRequestZim().setVendorRef(VENDOR_REF).setAmount(BigDecimal.TEN).setBankName("mpesa")
                .setMetaData(new HashMap<>(Map.of("walletId", 99, "transactionCode", "MTP", "organisation", "Revimo",
                        "cardNumber", "8811060000010302", "accountFundId", 20078466, "accountId", 20528257,
                        "partnerId", 0, "channel", "WEB", "sessionId", 912984, "paymentDescription", "Fund card")));
        PaymentResponseZim response = new PaymentResponseZim().setVendorRef(VENDOR_REF).setStatus(ResponseStatus.SUCCESS);

        when(zimApiProperties.getPaymentTimeout()).thenReturn(Duration.ofSeconds(90));
        when(zimApiProperties.getAllowedBanks()).thenReturn(List.of("mpesa"));
        when(dataService.isMpesaMtpRequest(request)).thenReturn(true);
        when(loggerService.getResponse(VENDOR_REF, PaymentResponseZim.class))
                .thenReturn(null)
                .thenReturn(new PaymentResponseZim().setStatus(ResponseStatus.PROCESSING))
                .thenReturn(response);

        PaymentResponseZim actualResponse = service.makePaymentSync(request);
        assertThat(actualResponse).isEqualTo(response);
        assertThat(request.getExpirationTime()).isNotNull();
        verify(kafkaSender).sendZimPaymentRequest(VENDOR_REF, request);
    }

    @Test
    void testHandleNewPayment() {
        PaymentRequestZim request = new PaymentRequestZim().setVendorRef(VENDOR_REF).setBankName("mpesa");
        when(loggerService.savePaymentRequest(VENDOR_REF, request)).thenReturn(true);

        service.handleNewPayment(request);
        verify(dataService).obtainAndValidatePaymentData(request);
        verify(kafkaSender).sendMpesaService(VENDOR_REF, request);
    }

    @Test
    void testHandleNewPaymentException() {
        PaymentRequestZim request = new PaymentRequestZim().setVendorRef(VENDOR_REF).setBankName("mpesa");
        when(loggerService.savePaymentRequest(VENDOR_REF, request)).thenReturn(true);
        doThrow(new RuntimeException("test exception")).when(dataService).obtainAndValidatePaymentData(request);

        service.handleNewPayment(request);
        verify(kafkaSender).sendZimPaymentError(eq(VENDOR_REF), any(PaymentErrorZim.class));
    }

    @Test
    void testHandlePaymentSuccessResult() {
        PaymentSuccessZim request = new PaymentSuccessZim().setVendorRef(VENDOR_REF).setBankName("mpesa").setTransactionId(TRANSACTION_ID);
        PaymentRequestZim paymentRequest = new PaymentRequestZim().setVendorRef(VENDOR_REF);
        PaymentResponseZim paymentResponse = new PaymentResponseZim().setVendorRef(VENDOR_REF).setStatus(PROCESSING);
        when(loggerService.getRequest(VENDOR_REF, PaymentRequestZim.class)).thenReturn(paymentRequest);
        when(loggerService.getResponse(VENDOR_REF, PaymentResponseZim.class)).thenReturn(paymentResponse);

        service.handlePaymentSuccessResult(request);
        assertThat(paymentResponse.getStatus()).isEqualTo(SUCCESS);
        assertThat(paymentResponse.getExternalTransactionId()).isEqualTo(TRANSACTION_ID);
        verify(dataService).approveLedgerPayment(paymentRequest, paymentResponse);
        verify(loggerService).savePaymentResponse(VENDOR_REF, paymentResponse);
    }

    @Test
    void testHandlePaymentError() {
        PaymentErrorZim request = new PaymentErrorZim().setVendorRef(VENDOR_REF).setMessage("some error").setErrorCode("some code");
        PaymentRequestZim paymentRequest = new PaymentRequestZim().setVendorRef(VENDOR_REF);
        PaymentResponseZim paymentResponse = new PaymentResponseZim().setVendorRef(VENDOR_REF).setStatus(PROCESSING);
        when(loggerService.getResponse(VENDOR_REF, PaymentResponseZim.class)).thenReturn(paymentResponse);
        when(loggerService.getRequest(VENDOR_REF, PaymentRequestZim.class)).thenReturn(paymentRequest);

        service.handlePaymentError(request);
        assertThat(paymentResponse.getStatus()).isEqualTo(ERROR);
        assertThat(paymentResponse.getMessage()).isEqualTo("some error");
        assertThat(paymentResponse.getErrorCode()).isEqualTo("some code");
        assertThat(paymentResponse.getTryingToRefund()).isNull();
        verify(loggerService, times(2)).savePaymentResponse(VENDOR_REF, paymentResponse);
        verify(dataService).failLedgerPayment(paymentRequest, paymentResponse);
    }

    @Test
    void testGetPaymentRequest() {
        PaymentRequestZim request = new PaymentRequestZim().setVendorRef(VENDOR_REF);
        when(loggerService.getRequest(VENDOR_REF, PaymentRequestZim.class)).thenReturn(request);

        PaymentRequestZim actualRequest = service.getPaymentRequest(VENDOR_REF);
        assertThat(actualRequest).isEqualTo(request);
    }

    @Test
    void testGetPaymentResponse() {
        PaymentResponseZim response = new PaymentResponseZim().setVendorRef(VENDOR_REF);
        when(loggerService.getResponse(VENDOR_REF, PaymentResponseZim.class)).thenReturn(response);

        PaymentResponseZim actualResponse = service.getPaymentResponse(VENDOR_REF);
        assertThat(actualResponse).isEqualTo(response);
    }
}
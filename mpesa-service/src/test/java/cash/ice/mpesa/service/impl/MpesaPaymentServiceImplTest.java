package cash.ice.mpesa.service.impl;

import cash.ice.mpesa.config.MpesaProperties;
import cash.ice.mpesa.dto.Payment;
import cash.ice.mpesa.entity.MpesaPayment;
import cash.ice.mpesa.repository.MpesaPaymentRepository;
import cash.ice.mpesa.service.MpesaSenderService;
import com.fc.sdk.APIResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MpesaPaymentServiceImplTest {
    private static final String VENDOR_REF = "testVendorRef";
    private static final String MSISDN = "771234567";
    private static final BigDecimal AMOUNT = new BigDecimal("10.0");
    private static final String TRANSACTION_ID = "87p1xjr88jft";

    @Mock
    private MpesaSenderService mpesaSenderService;
    @Mock
    private MpesaPaymentRepository mpesaPaymentRepository;
    @Mock
    private MpesaProperties mpesaProperties;
    @InjectMocks
    private MpesaPaymentServiceImpl service;

    @Test
    void testInboundPayment() {
        Payment payment = createPaymentRequest(Payment.Type.Inbound);

        when(mpesaPaymentRepository.save(any(MpesaPayment.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
        when(mpesaSenderService.sendInboundRequest(VENDOR_REF, MSISDN, AMOUNT)).thenReturn(createResponse());
        when(mpesaProperties.isSendStatusQuery()).thenReturn(true);
        when(mpesaSenderService.sendQueryTransactionStatusRequest(VENDOR_REF)).thenReturn(createResponse());

        service.processPayment(payment);
    }

    @Test
    void testOutboundPayment() {
        Payment payment = createPaymentRequest(Payment.Type.Outbound);

        when(mpesaPaymentRepository.save(any(MpesaPayment.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
        when(mpesaSenderService.sendOutboundRequest(VENDOR_REF, MSISDN, AMOUNT)).thenReturn(createResponse());
        when(mpesaProperties.isSendStatusQuery()).thenReturn(true);
        when(mpesaSenderService.sendQueryTransactionStatusRequest(VENDOR_REF)).thenReturn(createResponse());

        service.processPayment(payment);
    }

    @Test
    void testRefund() {
        MpesaPayment mpesaPayment = new MpesaPayment().setResponseCode("INS-0").setTransactionId(TRANSACTION_ID).setPayment(new Payment().setMetaData(Map.of()));

        when(mpesaPaymentRepository.findByVendorRef(VENDOR_REF)).thenReturn(List.of(mpesaPayment));
        when(mpesaSenderService.sendReversalRequest(TRANSACTION_ID, null)).thenReturn(createResponse());
        when(mpesaProperties.isSendStatusQuery()).thenReturn(true);
        when(mpesaSenderService.sendQueryTransactionStatusRequest(TRANSACTION_ID)).thenReturn(createResponse());

        service.processRefund(VENDOR_REF);
        verify(mpesaPaymentRepository).save(mpesaPayment);
        Assertions.assertThat(mpesaPayment.isRefunded()).isTrue();
    }

    private Payment createPaymentRequest(Payment.Type paymentType) {
        return new Payment().setVendorRef(VENDOR_REF).setMsisdn(MSISDN).setPaymentType(paymentType)
                .setAmount(AMOUNT).setMetaData(Map.of("accountNumber", "34987654321"));
    }

    private APIResponse createResponse() {
        APIResponse response = new APIResponse();
        response.setStatusCode(201);
        response.setReason("Created");
        response.setParameters(Map.of(
                "output_ResponseCode", "INS-0",
                "output_ResponseDesc", "Request processed successfully",
                "output_TransactionID", TRANSACTION_ID,
                "output_ConversationID", "f40929d63386410080ae5073859fd0ef",
                "output_ThirdPartyReference", "5XSXY2"));
        return response;
    }
}
package cash.ice.posb.service.impl;

import cash.ice.common.dto.zim.*;
import cash.ice.posb.PosbRestClient;
import cash.ice.posb.dto.PosbPayment;
import cash.ice.posb.dto.posb.*;
import cash.ice.posb.error.PosbException;
import cash.ice.posb.kafka.PosbKafkaProducer;
import cash.ice.posb.repository.PosbPaymentRepository;
import cash.ice.posb.service.PosbValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PosbPaymentServiceImplTest {
    private static final String VENDOR_REF = "vendorRef1";
    private static final String ACCOUNT_NUMBER = "12345678";
    private static final BigDecimal AMOUNT = BigDecimal.TEN;
    private static final String CURRENCY_CODE = "ZWL";
    private static final String PAYMENT_DESCRIPTION = "Payment description";
    private static final String OTP = "1234";
    private static final String CONFIRMATION_STATUS = "status1";
    private static final String ERROR_MESSAGE = "Some message";

    @Mock
    private PosbValidationService validationService;
    @Mock
    private PosbPaymentRepository paymentRepository;
    @Mock
    private PosbRestClient posbRestClient;
    @Mock
    private PosbKafkaProducer posbKafkaProducer;
    @InjectMocks
    private PosbPaymentServiceImpl service;
    @Captor
    private ArgumentCaptor<PosbPayment> posbPaymentCaptor;

    @Test
    void testProcessPayment() {
        PaymentRequestZim request = new PaymentRequestZim().setVendorRef(VENDOR_REF).setAccountNumber(ACCOUNT_NUMBER).setAmount(AMOUNT)
                .setMetaData(Map.of("currencyCode", CURRENCY_CODE, "paymentDescription", PAYMENT_DESCRIPTION));
        PosbInstructionResponse posbInstructionResponse = new PosbInstructionResponse();

        when(paymentRepository.findByVendorRef(VENDOR_REF)).thenReturn(Optional.empty());
        when(posbRestClient.sendInstruction(any(PosbInstructionRequest.class), any(String.class))).thenReturn(posbInstructionResponse);
        service.processPayment(request);

        verify(validationService).validatePaymentRequest(request);
        verify(posbKafkaProducer).sendPaymentResponse(eq(VENDOR_REF), any(PaymentOtpWaitingZim.class), eq(false));
        verify(paymentRepository).save(posbPaymentCaptor.capture());
        PosbPayment actualPosbPayment = posbPaymentCaptor.getValue();
        assertThat(actualPosbPayment.getCreatedTime()).isNotNull();
        assertThat(actualPosbPayment.getVendorRef()).isEqualTo(VENDOR_REF);
        assertThat(actualPosbPayment.getPaymentRequest()).isEqualTo(request);
        assertThat(actualPosbPayment.getTraceId()).isNotEmpty();
        assertThat(actualPosbPayment.getStatus()).isEqualTo("otp");
        assertThat(actualPosbPayment.getInstructionRequest()).isNotNull();
        assertThat(actualPosbPayment.getInstructionRequest().getPaymentReference()).isEqualTo(VENDOR_REF);
        assertThat(actualPosbPayment.getInstructionRequest().getCustomerAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
        assertThat(actualPosbPayment.getInstructionRequest().getAmount()).isEqualTo(AMOUNT);
        assertThat(actualPosbPayment.getInstructionRequest().getCurrency()).isEqualTo(CURRENCY_CODE);
        assertThat(actualPosbPayment.getInstructionRequest().getDescription()).isEqualTo(PAYMENT_DESCRIPTION);
        assertThat(actualPosbPayment.getInstructionResponse()).isEqualTo(posbInstructionResponse);
    }

    @Test
    void testProcessOtp() {
        PaymentOtpRequestZim request = new PaymentOtpRequestZim().setVendorRef(VENDOR_REF).setOtp(OTP);
        PosbPayment posbPayment = new PosbPayment().setTraceId("traceId1");
        PosbConfirmationResponse confirmationResponse = new PosbConfirmationResponse().setStatus(CONFIRMATION_STATUS);
        PosbStatusResponse statusResponse = new PosbStatusResponse().setStatus(CONFIRMATION_STATUS);

        when(paymentRepository.findByVendorRef(VENDOR_REF)).thenReturn(Optional.of(posbPayment));
        when(posbRestClient.sendConfirmation(any(PosbConfirmationRequest.class), any(String.class))).thenReturn(confirmationResponse);
        when(posbRestClient.getPaymentStatus(VENDOR_REF)).thenReturn(statusResponse);
        service.processOtp(request);

        verify(validationService).validateOtpRequest(request);
        verify(validationService, times(2)).checkSuccessfulStatus(CONFIRMATION_STATUS);

        verify(posbKafkaProducer).sendPaymentResponse(eq(VENDOR_REF), any(PaymentSuccessZim.class), eq(true));
        verify(paymentRepository).update(posbPayment);
        assertThat(posbPayment.getStatus()).isEqualTo("success");
        assertThat(posbPayment.getUpdatedTime()).isNotNull();
        assertThat(posbPayment.getConfirmationRequest()).isNotNull();
        assertThat(posbPayment.getConfirmationRequest().getPaymentReference()).isEqualTo(VENDOR_REF);
        assertThat(posbPayment.getConfirmationRequest().getOtp()).isEqualTo(OTP);
        assertThat(posbPayment.getConfirmationResponse()).isEqualTo(confirmationResponse);
        assertThat(posbPayment.getStatusResponse()).isEqualTo(statusResponse);
    }

    @Test
    void testProcessRefund() {
        PaymentRefundRequestZim request = new PaymentRefundRequestZim().setVendorRef(VENDOR_REF);
        PosbPayment posbPayment = new PosbPayment().setPaymentRequest(new PaymentRequestZim().setAccountNumber(ACCOUNT_NUMBER))
                .setTraceId("traceId1").setConfirmationResponse(new PosbConfirmationResponse().setStatus("SUCCESSFUL"));
        PosbReversalResponse reversalResponse = new PosbReversalResponse().setReversalStatus(CONFIRMATION_STATUS);

        when(paymentRepository.findByVendorRef(VENDOR_REF)).thenReturn(Optional.of(posbPayment));
        when(posbRestClient.sendReversal(any(PosbReversalRequest.class), any(String.class))).thenReturn(reversalResponse);

        service.processRefund(request);

        verify(validationService).checkSuccessfulStatus(CONFIRMATION_STATUS);
        verify(paymentRepository).update(posbPayment);
        assertThat(posbPayment.getStatus()).isEqualTo("refunded");
        assertThat(posbPayment.getRefundedTime()).isNotNull();
        assertThat(posbPayment.getReversalResponse()).isEqualTo(reversalResponse);
    }

    @Test
    void testProcessError() {
        PosbPayment posbPayment = new PosbPayment().setVendorRef(VENDOR_REF);
        PosbException exception = new PosbException(posbPayment, ERROR_MESSAGE, new RuntimeException());

        when(paymentRepository.findByVendorRef(VENDOR_REF)).thenReturn(Optional.empty());
        service.processError(exception);

        verify(paymentRepository).save(posbPayment);
        verify(posbKafkaProducer).sendPaymentError(VENDOR_REF, new PaymentErrorZim().setVendorRef(VENDOR_REF).setMessage(ERROR_MESSAGE));
        assertThat(posbPayment.getStatus()).isEqualTo("error");
        assertThat(posbPayment.getUpdatedTime()).isNotNull();
        assertThat(posbPayment.getReason()).isEqualTo(ERROR_MESSAGE);
    }
}
package cash.ice.paygo.service.impl;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.service.KafkaSender;
import cash.ice.paygo.dto.AdditionalData;
import cash.ice.paygo.dto.DirectoryService;
import cash.ice.paygo.dto.PaygoCallbackRequest;
import cash.ice.paygo.dto.PaygoCallbackResponse;
import cash.ice.paygo.dto.admin.Credential;
import cash.ice.paygo.dto.admin.Merchant;
import cash.ice.paygo.entity.PaygoPayment;
import cash.ice.paygo.listener.PaygoServiceListener;
import cash.ice.paygo.repository.PaygoPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static cash.ice.common.error.ErrorCodes.EC5012;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaygoCallbackServiceImplTest {
    private static final String PAYGO_ID = "1234567";
    private static final String VENDOR_REF = "testRef";
    private static final String MERCHANT_ID = "merchantId";
    private static final String CREDENTIAL_ID = "credentialId";
    private static final BigDecimal AMOUNT = new BigDecimal("10.0");
    private static final String ZWL = "ZWL";
    private static final String NARRATION = "desc";
    private static final int EXPIRY_SECONDS = 600;
    private static final String MERCHANT_NAME = "merchantName";
    private static final String DEVICE_REFERENCE = "deviceReference1";

    @Mock
    private PaygoPaymentRepository paygoPaymentRepository;
    @Mock
    private KafkaSender kafkaSender;
    @Captor
    private ArgumentCaptor<ErrorData> errorDataCaptor;

    private PaygoCallbackServiceImpl service;

    @BeforeEach
    void init() {
        service = new PaygoCallbackServiceImpl(paygoPaymentRepository, kafkaSender);
    }

    @Test
    void handleAuthRequest() {
        PaygoCallbackRequest request = new PaygoCallbackRequest().setMessageType("AUTH").setPayee(PAYGO_ID)
                .setAdditionalData(new AdditionalData());

        when(paygoPaymentRepository.findByPayGoId(PAYGO_ID)).thenReturn(Optional.of(createMockPaygoPayment()));

        PaygoCallbackResponse response = service.handleRequest(request);
        DirectoryService directoryService = response.getAdditionalData().getDirectoryService();
        assertThat(directoryService).isNotNull();
        assertThat(directoryService.getMerchantId()).isEqualTo(MERCHANT_ID);
        assertThat(directoryService.getAuthorizedCredentialId()).isEqualTo(CREDENTIAL_ID);
        assertThat(directoryService.getRequestedPayment()).isNotNull();
        assertThat(directoryService.getRequestedPayment().getAmount()).isEqualTo(AMOUNT);
        assertThat(directoryService.getRequestedPayment().getCurrencyCode()).isEqualTo(ZWL);
        assertThat(directoryService.getRequestedPayment().getNarration()).isEqualTo(NARRATION);
        assertThat(directoryService.getRequestedPayment().getExpirySeconds()).isEqualTo(EXPIRY_SECONDS);
        assertThat(directoryService.getRequestedPayment().getInitiator()).isEqualTo(MERCHANT_NAME);
        assertThat(response.getDeviceReference()).isEqualTo(DEVICE_REFERENCE);
        assertThat(response.getResponseCode()).isEqualTo("000");
        assertThat(response.getResponseDescription()).isEqualTo("APPROVED");
    }

    @Test
    void handleAdviceRequest() {
        PaygoCallbackRequest request = new PaygoCallbackRequest().setMessageType("ADVICE").setResponseCode("000")
                .setPayee(PAYGO_ID).setAdditionalData(new AdditionalData());
        PaygoPayment paygoPayment = createMockPaygoPayment();

        when(paygoPaymentRepository.findByPayGoId(PAYGO_ID)).thenReturn(Optional.of(paygoPayment));

        PaygoCallbackResponse response = service.handleRequest(request);
        verify(paygoPaymentRepository).delete(paygoPayment);
        verify(kafkaSender).sendPaygoSuccessPayment(VENDOR_REF, paygoPayment.getPendingPayment(), null, PaygoServiceListener.SERVICE_HEADER);
        assertThat(response.getResponseCode()).isEqualTo("000");
    }

    @Test
    void handleAdviceRequestWithErrorResponse() {
        PaygoCallbackRequest request = new PaygoCallbackRequest().setMessageType("ADVICE").setResponseCode("001")
                .setPayee(PAYGO_ID).setAdditionalData(new AdditionalData());
        PaygoPayment paygoPayment = createMockPaygoPayment();

        when(paygoPaymentRepository.findByPayGoId(PAYGO_ID)).thenReturn(Optional.of(paygoPayment));

        PaygoCallbackResponse response = service.handleRequest(request);
        verify(paygoPaymentRepository).delete(paygoPayment);
        verify(kafkaSender).sendErrorPayment(eq(VENDOR_REF), errorDataCaptor.capture());
        ErrorData actualErrorData = errorDataCaptor.getValue();
        assertThat(actualErrorData.getFeesData()).isEqualTo(paygoPayment.getPendingPayment());
        assertThat(actualErrorData.getErrorCode()).isEqualTo(EC5012);
        assertThat(actualErrorData.getMessage()).isNotBlank();
        assertThat(response.getResponseCode()).isEqualTo("000");
    }

    private PaygoPayment createMockPaygoPayment() {
        return new PaygoPayment()
                .setPendingPayment(
                        new FeesData().setPaymentRequest(
                                new PaymentRequest().setVendorRef(VENDOR_REF).setAmount(AMOUNT).setCurrency(ZWL)
                                        .setMeta(Map.of("description", NARRATION))))
                .setMerchant((Merchant) new Merchant().setId(MERCHANT_ID).setName(MERCHANT_NAME))
                .setCredential(new Credential().setId(CREDENTIAL_ID))
                .setExpirySeconds(EXPIRY_SECONDS)
                .setDeviceReference(DEVICE_REFERENCE);
    }
}
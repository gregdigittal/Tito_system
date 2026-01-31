package cash.ice.ecocash.service;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.utils.Tool;
import cash.ice.ecocash.config.EcocashProperties;
import cash.ice.ecocash.dto.*;
import cash.ice.ecocash.entity.EcocashMerchant;
import cash.ice.ecocash.entity.EcocashPayment;
import cash.ice.ecocash.listener.EcocashServiceListener;
import cash.ice.ecocash.repository.EcocashMerchantRepository;
import cash.ice.ecocash.repository.EcocashPaymentRepository;
import cash.ice.ecocash.service.impl.EcocashPaymentServiceImpl;
import error.EcocashException;
import org.apache.kafka.common.header.Headers;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.internal.verification.AtLeast;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static cash.ice.common.error.ErrorCodes.EC6008;
import static cash.ice.ecocash.dto.EcocashCallbackPaymentResponse.createSimulatedResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EcocashPaymentServiceTest {
    private static final String ZWL = "ZWL";
    private static final String LPP = "LPP";
    private static final String VENDOR_REF = "testVendorRef";
    private static final String MERCHANT_CODE = "123456";
    private static final String MERCHANT_PIN = "1234";
    private static final String MERCHANT_NUMBER = "123456789";
    private static final String INITIATOR = "771234567";
    private static final String PAYMENT_ID = "654321";
    private static final String END_USER = "777654321";
    private static final String CORRELATOR = "12341234";
    private static final String ERROR_CODE = "someErrorCode";
    private static final String ERROR_REASON = "SomeErrorReason";
    private static final BigDecimal AMOUNT = new BigDecimal("10.0");

    @Mock
    private EcocashProperties ecocashProperties;
    @Mock
    private EcocashPaymentRepository ecocashPaymentRepository;
    @Mock
    private EcocashMerchantRepository ecocashMerchantRepository;
    @Mock
    private EcocashSenderService ecocashSenderService;
    @Mock
    private KafkaSender kafkaSender;
    @Captor
    private ArgumentCaptor<EcocashPayment> paymentArgumentCaptor;

    private EcocashPaymentService service;

    @BeforeEach
    void init() {
        service = new EcocashPaymentServiceImpl(ecocashProperties, ecocashPaymentRepository, ecocashMerchantRepository,
                ecocashSenderService, kafkaSender);
    }

    @Test
    void processPayment() {
        FeesData feesData = createFeesData();
        Payment payment = createPayment(feesData);
        when(ecocashMerchantRepository.findByTransactionCodesIn(LPP)).thenReturn(List.of(
                new EcocashMerchant().setCode(MERCHANT_CODE).setPin(MERCHANT_PIN).setNumber(MERCHANT_NUMBER)));
        when(ecocashProperties.getPhoneExpression()).thenReturn("(\\+?263|0)?(77|78)\\d{7}");
        when(ecocashSenderService.sendPayment(any())).thenAnswer(invocation ->
                createSimulatedResponse(invocation.getArgument(0), "COMPLETED"));
        when(ecocashPaymentRepository.save(any(EcocashPayment.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

        service.processPayment(payment, Tool.toKafkaHeaders(List.of()));
        verify(kafkaSender).sendEcocashSuccessPayment(VENDOR_REF, feesData, Tool.toKafkaHeaders(List.of()), EcocashServiceListener.SERVICE_HEADER);
    }

    @Test
    void checkStatus() {
        EcocashCallbackPaymentResponse response = createCallbackResponse("PENDING SUBSCRIBER VALIDTAION");

        when(ecocashSenderService.requestPaymentStatus(END_USER, CORRELATOR)).thenReturn(response);
        when(ecocashProperties.getStatusPollTimeout()).thenReturn(35);

        service.checkStatus(createEcocashPayment(), 10);
        verify(ecocashPaymentRepository).save(paymentArgumentCaptor.capture());
        EcocashPayment savedPayment = paymentArgumentCaptor.getValue();
        assertThat(savedPayment.getFinalResponse()).isEqualTo(response);
        assertThat(savedPayment.getTransactionOperationStatus()).isEqualTo(response.getTransactionOperationStatus());
        assertThat(savedPayment.isFinishedPayment()).isFalse();
        assertThat(savedPayment.getUpdatedTime()).isNotNull();
    }

    @Test
    void checkStatusTimeout() {
        when(ecocashSenderService.requestPaymentStatus(END_USER, CORRELATOR)).thenReturn(
                createCallbackResponse("COMPLETED"));
        when(ecocashProperties.getStatusPollTimeout()).thenReturn(35);

        EcocashException ecocashException = assertThrows(EcocashException.class,
                () -> service.checkStatus(createEcocashPayment(), 50));
        AssertionsForClassTypes.assertThat(ecocashException.getErrorCode()).isEqualTo(EC6008);
    }

    private EcocashCallbackPaymentResponse createCallbackResponse(String transactionOperationStatus) {
        return (EcocashCallbackPaymentResponse) new EcocashCallbackPaymentResponse()
                .setTransactionOperationStatus(transactionOperationStatus).setEndUserId(END_USER)
                .setClientCorrelator(CORRELATOR).setPaymentAmount(new PaymentAmount().setTotalAmountCharged(AMOUNT));
    }

    @Test
    void checkFailPayment() {
        Headers headers = null;
        EcocashPayment ecocashPayment = createEcocashPayment();
        ecocashPayment.setFinalResponse(createSimulatedResponse(ecocashPayment.getRequest(),
                "COMPLETED"));
        ecocashPayment.setEcocashReference(ecocashPayment.getFinalResponse().getEcocashReference());

        when(ecocashPaymentRepository.findByVendorRef(VENDOR_REF)).thenReturn(List.of(ecocashPayment));
        when(ecocashSenderService.refundPayment(any(EcocashCallbackPayment.class))).thenAnswer(invocation -> createSimulatedResponse(
                (EcocashCallbackPayment) invocation.getArguments()[0], "COMPLETED"));

        service.processError(new EcocashException(ecocashPayment, ERROR_REASON, ERROR_CODE));
        verify(kafkaSender).sendErrorPayment(VENDOR_REF, new ErrorData((FeesData) ecocashPayment.getPendingPayment(), ERROR_CODE, ERROR_REASON), headers);
        verify(ecocashPaymentRepository, new AtLeast(1)).save(paymentArgumentCaptor.capture());
        EcocashPayment savedPayment = paymentArgumentCaptor.getValue();
        assertThat(savedPayment.getRefundRequest()).isNotNull();
        assertThat(savedPayment.getRefundResponse()).isNotNull();
    }

    private FeesData createFeesData() {
        return new FeesData().setVendorRef(VENDOR_REF).setCurrencyCode(ZWL)
                .setTransactionCode(LPP).setPaymentRequest(new PaymentRequest()
                        .setInitiator(INITIATOR).setTx(LPP).setMeta(Map.of("description", "desc")).setAmount(AMOUNT));
    }

    private Payment createPayment(Object pendingPayment) {
        return new Payment().setVendorRef(VENDOR_REF).setCurrencyCode(ZWL)
                .setTx(LPP).setInitiator(INITIATOR).setMetaData(Map.of("description", "desc"))
                .setAmount(AMOUNT).setPendingRequest(pendingPayment);
    }

    private EcocashPayment createEcocashPayment() {
        return new EcocashPayment().setVendorRef(VENDOR_REF).setEndUserId(END_USER).setClientCorrelator(CORRELATOR)
                .setRequest(new EcocashCallbackPayment().setEndUserId(END_USER).setClientCorrelator(CORRELATOR)
                        .setPaymentAmount(new PaymentAmount().setCharginginformation(
                                new ChargingInformation().setAmount(AMOUNT)).setChargeMetaData(new ChargeMetaData())))
                .setPendingPayment(new FeesData());
    }
}
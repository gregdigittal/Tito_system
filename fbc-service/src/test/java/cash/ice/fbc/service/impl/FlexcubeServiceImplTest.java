package cash.ice.fbc.service.impl;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.utils.Tool;
import cash.ice.fbc.config.FlexcubeProperties;
import cash.ice.fbc.dto.flexcube.FlexcubeBalanceResponse;
import cash.ice.fbc.dto.flexcube.FlexcubePaymentRequest;
import cash.ice.fbc.dto.flexcube.FlexcubeResponse;
import cash.ice.fbc.dto.flexcube.FlexcubeStatusRequest;
import cash.ice.fbc.entity.FlexcubeAccount;
import cash.ice.fbc.entity.FlexcubePayment;
import cash.ice.fbc.error.FlexcubeException;
import cash.ice.fbc.listener.RtgsServiceListener;
import cash.ice.fbc.repository.FlexcubeAccountRepository;
import cash.ice.fbc.repository.FlexcubePaymentRepository;
import cash.ice.fbc.service.FlexcubeBalanceService;
import cash.ice.fbc.service.FlexcubeSenderService;
import cash.ice.fbc.service.FlexcubeService;
import org.apache.kafka.common.header.Headers;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static cash.ice.common.error.ErrorCodes.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlexcubeServiceImplTest {
    private static final String VENDOR_REF = "testVendor1";
    private static final String ZWL = "ZWL";
    private static final String TRN = "TRN";
    private static final BigDecimal AMOUNT = new BigDecimal("1.00");
    private static final String POOL_ACCOUNT = "1234567890";
    private static final String POOL_ACCOUNT_BRANCH = "003";
    private static final int REFERENCE_ID = 123456;

    @Mock
    private FlexcubeProperties flexcubeProperties;
    @Mock
    private FlexcubeBalanceService flexcubeBalanceService;
    @Mock
    private FlexcubeSenderService flexcubeSenderService;
    @Mock
    private FlexcubeAccountRepository flexcubeAccountRepository;
    @Mock
    private FlexcubePaymentRepository flexcubePaymentRepository;
    @Mock
    private KafkaSender kafkaSender;
    @Captor
    private ArgumentCaptor<FlexcubePayment> paymentArgumentCaptor;
    @Captor
    private ArgumentCaptor<FlexcubePaymentRequest> requestArgumentCaptor;
    @Captor
    private ArgumentCaptor<FlexcubeStatusRequest> statusArgumentCaptor;

    private FlexcubeService service;

    @BeforeEach
    void init() {
        service = new FlexcubeServiceImpl(flexcubeProperties, flexcubeBalanceService, flexcubeSenderService,
                flexcubeAccountRepository, flexcubePaymentRepository, kafkaSender);
    }

    @Test
    void testPayment() {
        FeesData feesData = createFeesData();
        FlexcubeAccount fbcAccount = new FlexcubeAccount().setDebitPoolAccount(POOL_ACCOUNT).setDebitPoolAccountBranch(POOL_ACCOUNT_BRANCH);
        Headers headers = Tool.toKafkaHeaders(List.of("Ledger-header"));
        FlexcubeResponse flexcubeResponse = new FlexcubeResponse().setResultCode("00");

        when(flexcubeAccountRepository.findByTransactionCodesIn(TRN)).thenReturn(List.of(fbcAccount));
        when(flexcubeBalanceService.checkBalance(eq(fbcAccount), eq(AMOUNT), any())).thenReturn(new FlexcubeBalanceResponse());
        when(flexcubeSenderService.sendPayment(requestArgumentCaptor.capture())).thenReturn(flexcubeResponse);

        service.processPayment(feesData, headers);
        FlexcubePaymentRequest request = requestArgumentCaptor.getValue();
        assertThat(request.getDebitAccount()).isEqualTo(POOL_ACCOUNT);
        assertThat(request.getDebitBranch()).isEqualTo(POOL_ACCOUNT_BRANCH);
        assertThat(request.getCreditCurrency()).isEqualTo(ZWL);
        assertThat(request.getDebitCurrency()).isEqualTo(ZWL);
        assertThat(request.getDebitAmount()).isEqualTo(AMOUNT.toString());
        verify(flexcubePaymentRepository, times(2)).save(paymentArgumentCaptor.capture());

        FlexcubePayment payment = paymentArgumentCaptor.getValue();
        assertThat(payment.getRequest()).isEqualTo(request);
        assertThat(payment.getResponse()).isEqualTo(flexcubeResponse);
        assertThat(payment.isFinishedPayment()).isTrue();
        verify(kafkaSender).sendRtgsSuccessPayment(VENDOR_REF, feesData, headers, RtgsServiceListener.SERVICE_HEADER);
    }

    @Test
    void testNoFlexcubeAccount() {
        FlexcubeException flexcubeException = assertThrows(FlexcubeException.class,
                () -> service.processPayment(createFeesData(), Tool.toKafkaHeaders(List.of("Ledger-header"))));
        AssertionsForClassTypes.assertThat(flexcubeException.getErrorCode()).isEqualTo(EC8003);
    }

    @Test
    void testPaymentFailed() {
        FeesData feesData = createFeesData();
        FlexcubeAccount fbcAccount = new FlexcubeAccount().setDebitPoolAccount(POOL_ACCOUNT).setDebitPoolAccountBranch(POOL_ACCOUNT_BRANCH);
        Headers headers = Tool.toKafkaHeaders(List.of("Ledger-header"));
        when(flexcubeAccountRepository.findByTransactionCodesIn(TRN)).thenReturn(List.of(fbcAccount));
        when(flexcubeBalanceService.checkBalance(eq(fbcAccount), eq(AMOUNT), any())).thenReturn(new FlexcubeBalanceResponse());
        when(flexcubeSenderService.sendPayment(requestArgumentCaptor.capture())).thenReturn(new FlexcubeResponse()
                .setResultCode("01"));

        FlexcubeException flexcubeException = assertThrows(FlexcubeException.class,
                () -> service.processPayment(feesData, headers));
        AssertionsForClassTypes.assertThat(flexcubeException.getErrorCode()).isEqualTo(EC8002);
        verify(flexcubePaymentRepository, times(1)).save(any(FlexcubePayment.class));
    }

    @Test
    void testCheckStatus() {
        FlexcubePayment payment = new FlexcubePayment().setVendorRef(VENDOR_REF).setReferenceId(REFERENCE_ID)
                .setKafkaHeaders(List.of("Ledger-header"))
                .setPendingPayment(new FeesData().setTransactionCode(TRN));

        FlexcubeAccount fbcAccount = new FlexcubeAccount().setDebitPoolAccount(POOL_ACCOUNT).setDebitPoolAccountBranch(POOL_ACCOUNT_BRANCH);
        Headers headers = Tool.toKafkaHeaders(List.of("Ledger-header"));
        FlexcubeResponse flexcubeResponse = new FlexcubeResponse().setResultCode("00");

        when(flexcubeSenderService.sendCheck(statusArgumentCaptor.capture())).thenReturn(flexcubeResponse);
        when(flexcubeAccountRepository.findByTransactionCodesIn(TRN)).thenReturn(List.of(fbcAccount));

        service.checkStatus(payment);
        FlexcubeStatusRequest statusRequest = statusArgumentCaptor.getValue();
        assertThat(statusRequest.getIcecashReference()).isEqualTo(String.valueOf(REFERENCE_ID));
        verify(kafkaSender).sendRtgsSuccessPayment(VENDOR_REF, payment.getPendingPayment(), headers, RtgsServiceListener.SERVICE_HEADER);
        verify(flexcubePaymentRepository).save(payment);
        verify(flexcubeSenderService).evictBalance(POOL_ACCOUNT, POOL_ACCOUNT_BRANCH);

        assertThat(payment.getStatusRequest()).isEqualTo(statusRequest);
        assertThat(payment.getResponse()).isEqualTo(flexcubeResponse);
        assertThat(payment.isFinishedPayment()).isTrue();
    }

    @Test
    void testFailPayment() {
        FeesData feesData = createFeesData();
        Headers headers = Tool.toKafkaHeaders(List.of("Ledger-header"));
        FlexcubePayment payment = new FlexcubePayment().setVendorRef(VENDOR_REF).setReferenceId(REFERENCE_ID)
                .setPendingPayment(feesData).setKafkaHeaders(List.of("Ledger-header"));
        String errorMessage = "Some error";
        when(flexcubePaymentRepository.findByVendorRef(VENDOR_REF)).thenReturn(List.of(payment));

        service.failPayment(feesData, EC8001, errorMessage, headers);
        verify(kafkaSender).sendErrorPayment(VENDOR_REF, new ErrorData(feesData, EC8001, errorMessage), headers);
        verify(flexcubePaymentRepository).save(payment);

        assertThat(payment.getErrorCode()).isEqualTo(EC8001);
        assertThat(payment.getReason()).isEqualTo(errorMessage);
        assertThat(payment.isFinishedPayment()).isTrue();
    }

    private FeesData createFeesData() {
        return new FeesData().setVendorRef(VENDOR_REF).setCurrencyCode(ZWL)
                .setTransactionCode(TRN).setPaymentRequest(new PaymentRequest()
                        .setTx(TRN).setMeta(Map.of("description", "desc")).setAmount(AMOUNT));
    }
}
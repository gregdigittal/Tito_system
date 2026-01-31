package cash.ice.onemoney.service.impl;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.utils.Tool;
import cash.ice.onemoney.config.OnemoneyProperties;
import cash.ice.onemoney.entity.OnemoneyPayment;
import cash.ice.onemoney.error.OnemoneyException;
import cash.ice.onemoney.listener.OnemoneyServiceListener;
import cash.ice.onemoney.repository.OnemoneyPaymentRepository;
import cash.ice.onemoney.service.OnemoneyClient;
import cash.ice.onemoney.service.OnemoneyPaymentService;
import com.huawei.cps.cpsinterface.api_requestmgr.Request;
import com.huawei.cps.cpsinterface.api_requestmgr.Response;
import com.huawei.cps.cpsinterface.api_resultmgr.Result;
import com.huawei.cps.cpsinterface.response.Body;
import com.huawei.cps.cpsinterface.response.Header;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static cash.ice.common.error.ErrorCodes.EC7002;
import static cash.ice.common.error.ErrorCodes.EC7007;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnemoneyPaymentServiceImplTest {
    private static final String OPAYG = "OPAYG";
    private static final String ZWL = "ZWL";
    private static final String LPP = "LPP";
    private static final String VENDOR_REF = "testVendorRef";
    private static final String INITIATOR = "771234567";
    private static final BigDecimal AMOUNT = new BigDecimal("10.0");
    private static final String ORIGINATOR_CONVERSATION_ID = "1234";
    private static final String TRANSACTION_ID = "234";
    private static final String CREDENTIAL = "cred";
    private static final String CONVERSATION_ID = "conversationId";

    @Mock
    private OnemoneyPaymentRepository onemoneyPaymentRepository;
    @Mock
    private OnemoneyClient onemoneyClient;
    @Mock
    private OnemoneyProperties onemoneyProperties;
    @Mock
    private KafkaSender kafkaSender;
    @Captor
    private ArgumentCaptor<Request> requestArgumentCaptor;
    @Captor
    private ArgumentCaptor<com.huawei.cps.synccpsinterface.api_requestmgr.Request> statusRequestArgumentCaptor;
    @Captor
    private ArgumentCaptor<OnemoneyPayment> paymentArgumentCaptor;

    private OnemoneyPaymentService service;

    @BeforeEach
    void init() {
        service = new OnemoneyPaymentServiceImpl(onemoneyProperties, onemoneyPaymentRepository, onemoneyClient, kafkaSender);
    }

    @Test
    void testProcessPayment() {
        FeesData feesData = createFeesData();
        Response response = createResponse();
        when(onemoneyProperties.getRequest()).thenReturn(new OnemoneyProperties.Request().setTimestampPattern("yyyyMMddHHmmss"));
        when(onemoneyProperties.getResult()).thenReturn(new OnemoneyProperties.Result());
        when(onemoneyClient.sendPayment(requestArgumentCaptor.capture(), any())).thenReturn(response);

        service.processPayment(feesData, Tool.toKafkaHeaders(List.of()));
        verify(onemoneyPaymentRepository).save(paymentArgumentCaptor.capture());
        Request request = requestArgumentCaptor.getValue();
        assertThat(request.getHeader().getCommandID()).isEqualTo("InitTrans_TwoPartPayment");
        assertThat(request.getBody().getTransactionRequest().getParameters().getAmount()).isEqualTo(AMOUNT);
        assertThat(request.getBody().getTransactionRequest().getParameters().getCurrency()).isEqualTo(ZWL);

        OnemoneyPayment payment = paymentArgumentCaptor.getValue();
        assertThat(payment.getCreatedTime()).isNotNull();
        assertThat(payment.getVendorRef()).isEqualTo(feesData.getVendorRef());
        assertThat(payment.getPendingPayment()).isEqualTo(feesData);
        assertThat(payment.getRequest()).isEqualTo(request);
        assertThat(payment.getOriginatorConversationId()).isEqualTo(request.getHeader().getOriginatorConversationID());
        assertThat(payment.getResponse()).isEqualTo(response);
        assertThat(payment.getNeedCheckStatus()).isEqualTo(Boolean.TRUE);
    }

    @Test
    void testPaymentCallbackResult() {
        Result result = createResult(0, "Process service request successfully.");
        OnemoneyPayment payment = createOnemoneyPayment();

        when(onemoneyPaymentRepository.findByOriginatorConversationId(ORIGINATOR_CONVERSATION_ID))
                .thenReturn(Optional.of(payment));
        when(onemoneyProperties.getPaymentTimeout()).thenReturn(35);

        service.callbackResult(result);
        verify(kafkaSender).sendOnemoneySuccessPayment(VENDOR_REF, payment.getPendingPayment(), null, OnemoneyServiceListener.SERVICE_HEADER);
        verify(onemoneyPaymentRepository).save(paymentArgumentCaptor.capture());
        OnemoneyPayment actualPayment = paymentArgumentCaptor.getValue();
        assertThat(actualPayment.getResult()).isEqualTo(result);
        assertThat(actualPayment.getNeedCheckStatus()).isNull();
        assertThat(actualPayment.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(actualPayment.getUpdatedTime()).isNotNull();
        assertThat(actualPayment.isFinishedPayment()).isTrue();
    }

    @Test
    void testPaymentCallbackBadResult() {
        Result result = createResult(1000, "Something went wrong.");
        OnemoneyPayment payment = createOnemoneyPayment();

        when(onemoneyPaymentRepository.findByOriginatorConversationId(ORIGINATOR_CONVERSATION_ID))
                .thenReturn(Optional.of(payment));
        when(onemoneyProperties.getPaymentTimeout()).thenReturn(35);

        OnemoneyException onemoneyException = assertThrows(OnemoneyException.class,
                () -> service.callbackResult(result));
        AssertionsForClassTypes.assertThat(onemoneyException.getErrorCode()).isEqualTo(EC7002);
        OnemoneyPayment actualPayment = onemoneyException.getOnemoneyPayment();
        assertThat(actualPayment.getResult()).isEqualTo(result);
        assertThat(actualPayment.getNeedCheckStatus()).isNull();
        assertThat(actualPayment.getUpdatedTime()).isNotNull();
    }

    @Test
    void testLatePaymentCallbackResult() {
        Result result = createResult(0, "Process service request successfully.");
        OnemoneyPayment payment = createOnemoneyPayment()
                .setCreatedTime(Instant.ofEpochMilli(System.currentTimeMillis() - 40000));

        when(onemoneyPaymentRepository.findByOriginatorConversationId(ORIGINATOR_CONVERSATION_ID))
                .thenReturn(Optional.of(payment));
        when(onemoneyProperties.getPaymentTimeout()).thenReturn(35);

        OnemoneyException onemoneyException = assertThrows(OnemoneyException.class,
                () -> service.callbackResult(result));
        AssertionsForClassTypes.assertThat(onemoneyException.getErrorCode()).isEqualTo(EC7007);
        OnemoneyPayment actualPayment = onemoneyException.getOnemoneyPayment();
        assertThat(actualPayment.getResult()).isEqualTo(result);
        assertThat(actualPayment.getNeedCheckStatus()).isNull();
        assertThat(actualPayment.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(actualPayment.getUpdatedTime()).isNotNull();
    }

    @Test
    void testRefundCallbackResult() {
        Result result = createResult(0, "Process service request successfully.");
        OnemoneyPayment payment = createOnemoneyPayment();

        when(onemoneyPaymentRepository.findByRefundOriginatorConversationId(ORIGINATOR_CONVERSATION_ID))
                .thenReturn(Optional.of(payment));

        service.callbackResult(result);
        verify(onemoneyPaymentRepository).save(paymentArgumentCaptor.capture());
        OnemoneyPayment actualPayment = paymentArgumentCaptor.getValue();
        assertThat(actualPayment.getRefundResult()).isEqualTo(result);
    }

    @Test
    void testCheckStatus() {
        OnemoneyPayment payment = createOnemoneyPayment();
        var result = createStatusResult(0, "Completed");

        when(onemoneyClient.sendStatusRequest(statusRequestArgumentCaptor.capture(), any())).thenReturn(result);
        when(onemoneyProperties.getRequest()).thenReturn(new OnemoneyProperties.Request().setOnemoneySecurityCredential(CREDENTIAL).setTimestampPattern("yyyyMMddHHmmss"));
        when(onemoneyProperties.getResult()).thenReturn(new OnemoneyProperties.Result());

        OnemoneyException onemoneyException = assertThrows(OnemoneyException.class,
                () -> service.checkStatus(payment, 40, false));
        AssertionsForClassTypes.assertThat(onemoneyException.getErrorCode()).isEqualTo(EC7007);
        var actualRequest = statusRequestArgumentCaptor.getValue();
        assertThat(payment.getStatusRequest()).isEqualTo(actualRequest);
        assertThat(payment.getStatusResult()).isEqualTo(result);
        assertThat(actualRequest.getHeader().getCommandID()).isEqualTo("QueryTransactionStatus");
        assertThat(actualRequest.getBody().getIdentity().getInitiator().getSecurityCredential()).isEqualTo(CREDENTIAL);
        assertThat(actualRequest.getBody().getQueryTransactionStatusRequest().getOriginalConversationID()).isEqualTo(ORIGINATOR_CONVERSATION_ID);
    }

    @Test
    void testRefund() {
        ErrorData errorData = new ErrorData(createFeesData(), EC7007, "Timed out");
        OnemoneyPayment payment = createOnemoneyPayment().setTransactionId(TRANSACTION_ID);
        Response response = createResponse();

        when(onemoneyPaymentRepository.findByVendorRef(VENDOR_REF)).thenReturn(List.of(payment));
        when(onemoneyProperties.getRequest()).thenReturn(new OnemoneyProperties.Request().setOnemoneySecurityCredential(CREDENTIAL).setTimestampPattern("yyyyMMddHHmmss"));
        when(onemoneyProperties.getResult()).thenReturn(new OnemoneyProperties.Result());
        when(onemoneyClient.sendRefundRequest(requestArgumentCaptor.capture(), any())).thenReturn(response);

        service.processRefund(errorData);
        Request actualRequest = requestArgumentCaptor.getValue();
        assertThat(actualRequest.getHeader().getCommandID()).isEqualTo("RaiseDisputedTxnReversal");
        assertThat(actualRequest.getBody().getIdentity().getInitiator().getSecurityCredential()).isEqualTo(CREDENTIAL);
        assertThat(actualRequest.getBody().getRaiseDisputedTxnReversalRequest().getOriginalReceiptNumber()).isEqualTo(TRANSACTION_ID);
        assertThat(actualRequest.getBody().getRaiseDisputedTxnReversalRequest().getAmount()).isEqualTo(AMOUNT);

        verify(onemoneyPaymentRepository).save(paymentArgumentCaptor.capture());
        OnemoneyPayment actualPayment = paymentArgumentCaptor.getValue();
        assertThat(actualPayment.getRefundRequest()).isEqualTo(actualRequest);
        assertThat(actualPayment.getRefundOriginatorConversationId()).isEqualTo(actualRequest.getHeader().getOriginatorConversationID());
        assertThat(actualPayment.getRefundResponse()).isEqualTo(response);
    }

    @Test
    void testFailPayment() {
        String message = "Timed out";
        OnemoneyPayment payment = createOnemoneyPayment();

        service.failPayment(payment, EC7007, message, null);
        verify(onemoneyPaymentRepository).save(paymentArgumentCaptor.capture());
        OnemoneyPayment actualPayment = paymentArgumentCaptor.getValue();
        assertThat(actualPayment.getErrorCode()).isEqualTo(EC7007);
        assertThat(actualPayment.getErrorMessage()).isEqualTo(message);
        assertThat(actualPayment.isFinishedPayment()).isTrue();
        assertThat(actualPayment.getUpdatedTime()).isNotNull();

        verify(kafkaSender).sendErrorPayment(VENDOR_REF,
                new ErrorData(payment.getPendingPayment(), EC7007, message), null);
    }

    private OnemoneyPayment createOnemoneyPayment() {
        FeesData feesData = new FeesData().setTransactionCode(OPAYG)
                .setPaymentRequest(new PaymentRequest().setAmount(AMOUNT));
        return new OnemoneyPayment().setVendorRef(VENDOR_REF).setNeedCheckStatus(true).setFinishedPayment(false)
                .setOriginatorConversationId(ORIGINATOR_CONVERSATION_ID).setCreatedTime(Instant.now()).setPendingPayment(feesData);
    }

    private com.huawei.cps.synccpsinterface.api_requestmgr.Result createStatusResult(int resultCode, String transactionStatus) {
        var result = new com.huawei.cps.synccpsinterface.api_requestmgr.Result();
        var body = new com.huawei.cps.synccpsinterface.result.Body();
        body.setResultCode(resultCode);
        if (transactionStatus != null) {
            var queryTransactionStatusResult = new com.huawei.cps.synccpsinterface.result.Body.QueryTransactionStatusResult();
            queryTransactionStatusResult.setTransactionStatus(transactionStatus);
            queryTransactionStatusResult.setReceiptNumber(TRANSACTION_ID);
            body.setQueryTransactionStatusResult(queryTransactionStatusResult);
        }
        result.setBody(body);
        return result;
    }

    private Result createResult(int resultCode, String resultDesc) {
        Result result = new Result();
        com.huawei.cps.cpsinterface.result.Header header = new com.huawei.cps.cpsinterface.result.Header();
        header.setOriginatorConversationID(ORIGINATOR_CONVERSATION_ID);
        result.setHeader(header);
        com.huawei.cps.cpsinterface.result.Body body = new com.huawei.cps.cpsinterface.result.Body();
        body.setResultCode(resultCode);
        body.setResultDesc(resultDesc);
        com.huawei.cps.cpsinterface.result.Body.TransactionResult transactionResult = new com.huawei.cps.cpsinterface.result.Body.TransactionResult();
        transactionResult.setTransactionID(TRANSACTION_ID);
        body.setTransactionResult(transactionResult);
        result.setBody(body);
        return result;
    }

    private Response createResponse() {
        Response response = new Response();
        Header header = new Header();
        header.setConversationID(CONVERSATION_ID);
        response.setHeader(header);
        response.setBody(new Body());
        return response;
    }

    private FeesData createFeesData() {
        return new FeesData().setVendorRef(VENDOR_REF).setCurrencyCode(ZWL)
                .setTransactionCode(LPP).setPaymentRequest(new PaymentRequest().setInitiator(INITIATOR).setTx(LPP)
                        .setMeta(Map.of("description", "desc")).setAmount(AMOUNT));
    }
}
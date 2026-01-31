package cash.ice.onemoney.service.impl;

import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.utils.Tool;
import cash.ice.onemoney.config.OnemoneyProperties;
import cash.ice.onemoney.endpoint.ResultEndpoint;
import cash.ice.onemoney.entity.OnemoneyPayment;
import cash.ice.onemoney.repository.OnemoneyPaymentRepository;
import cash.ice.onemoney.service.OnemoneyClient;
import com.huawei.cps.cpsinterface.api_requestmgr.Request;
import com.huawei.cps.cpsinterface.api_requestmgr.Response;
import com.huawei.cps.cpsinterface.api_resultmgr.Result;
import com.huawei.cps.cpsinterface.response.Body;
import com.huawei.cps.cpsinterface.response.Header;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@Profile(IceCashProfile.NOT_PROD)
@Slf4j
public class OnemoneyPaymentServiceUatImpl extends OnemoneyPaymentServiceImpl {
    private final ResultEndpoint resultEndpoint;

    public OnemoneyPaymentServiceUatImpl(@Lazy ResultEndpoint resultEndpoint, OnemoneyProperties onemoneyProperties, OnemoneyPaymentRepository onemoneyPaymentRepository, OnemoneyClient onemoneyClient, KafkaSender kafkaSender) {
        super(onemoneyProperties, onemoneyPaymentRepository, onemoneyClient, kafkaSender);
        this.resultEndpoint = resultEndpoint;
    }

    @Override
    public void processPayment(FeesData feesData, Headers headers) {
        String refundTransactionId = (String) feesData.getPaymentRequest().getMeta().get("performRefund");
        if (refundTransactionId != null) {
            String vendorRef = (String) feesData.getPaymentRequest().getMeta().get("overrideVendorRef");
            OnemoneyPayment payment = onemoneyPaymentRepository.save(new OnemoneyPayment()
                    .setCreatedTime(Instant.now())
                    .setVendorRef(vendorRef != null ? vendorRef : feesData.getVendorRef())
                    .setPendingPayment(feesData)
                    .setTransactionId(refundTransactionId));
            refund(payment.getVendorRef());
        } else {
            super.processPayment(feesData, headers);
        }
    }

    @Override
    public long getStatusPollInitDelay(OnemoneyPayment payment) {
        Integer statusPollInitDelay = (Integer) payment.getPendingPayment().getPaymentRequest().getMeta().get("statusPollInitDelay");
        return statusPollInitDelay != null ? statusPollInitDelay : super.getStatusPollInitDelay(payment);
    }

    @Override
    public long getExpiredPaymentsRecheckAfterTime(OnemoneyPayment payment) {
        Integer expiredPaymentsRecheckAfterTime = (Integer) payment.getPendingPayment().getPaymentRequest().getMeta().get("expiredPaymentsRecheckAfterTime");
        return expiredPaymentsRecheckAfterTime != null ? expiredPaymentsRecheckAfterTime : super.getExpiredPaymentsRecheckAfterTime(payment);
    }

    @Override
    protected Response sendRequest(Request request, OnemoneyPayment onemoneyPayment, String paymentUrl) {
        Map<String, String> simulateResponseMap = onemoneyProperties.getSimulateResponse();
        if (simulateResponseMap.containsKey(getInitiator(onemoneyPayment))) {
            return simulatePaymentResponse(request, simulateResponseMap.get(getInitiator(onemoneyPayment)));
        } else {
            return super.sendRequest(request, onemoneyPayment,
                    onemoneyProperties.isUseMockServer() ? onemoneyProperties.getMockServerUrl() : paymentUrl);
        }
    }

    @Override
    protected com.huawei.cps.synccpsinterface.api_requestmgr.Result sendStatusRequest(com.huawei.cps.synccpsinterface.api_requestmgr.Request request, OnemoneyPayment onemoneyPayment, String statusUrl) {
        Map<String, String> simulateResponseMap = onemoneyProperties.getSimulateResponse();
        if (simulateResponseMap.containsKey(getInitiator(onemoneyPayment))) {
            return simulateStatusResult(request, simulateResponseMap.get(getInitiator(onemoneyPayment)));
        } else {
            return super.sendStatusRequest(request, onemoneyPayment,
                    onemoneyProperties.isUseMockServer() ? onemoneyProperties.getMockServerUrl() : statusUrl);
        }
    }

    @Override
    protected Response sendRefundRequest(Request request, OnemoneyPayment onemoneyPayment, String reversalUrl) {
        Map<String, String> simulateResponseMap = onemoneyProperties.getSimulateResponse();
        if (simulateResponseMap.containsKey(getInitiator(onemoneyPayment))) {
            return simulateRefundResponse(request, simulateResponseMap.get(getInitiator(onemoneyPayment)));
        } else {
            return super.sendRefundRequest(request, onemoneyPayment,
                    onemoneyProperties.isUseMockServer() ? onemoneyProperties.getMockServerUrl() : reversalUrl);
        }
    }

    private String getInitiator(OnemoneyPayment onemoneyPayment) {
        return onemoneyPayment.getPendingPayment().getPaymentRequest().getInitiator();
    }

    private Response simulateResponse(Request request, int responseCode, String responseDesc) {
        Response response = new Response();
        Header header = new Header();
        header.setVersion(request.getHeader().getVersion());
        header.setOriginatorConversationID(request.getHeader().getOriginatorConversationID());
        header.setConversationID("SIMULATED." + UUID.randomUUID().toString().substring(25));
        response.setHeader(header);
        Body body = new Body();
        body.setResponseCode(responseCode);
        body.setResponseDesc(responseDesc);
        body.setServiceStatus(0);
        response.setBody(body);
        return response;
    }

    private Response simulatePaymentResponse(Request request, String responseType) {
        if ("RESPONSE_FAILED".equals(responseType)) {
            return simulateResponse(request, -1, "Something went wrong.");
        } else {
            if (!"STATUS_FAILED".equals(responseType) && !"STATUS_SUCCESS".equals(responseType)
                    && !"REFUND_RESPONSE_FAILED".equals(responseType) && !"REFUND_RESULT_FAILED".equals(responseType)) {
                new Thread(() -> simulateResult(request, "RESULT_TIMED_OUT".equals(responseType),
                        "RESULT_FAILED".equals(responseType))).start();
            }
            return simulateResponse(request, 0, "Accept the service request successfully.");
        }
    }

    private com.huawei.cps.synccpsinterface.api_requestmgr.Result simulateStatusResult(com.huawei.cps.synccpsinterface.api_requestmgr.Request request, String responseType) {
        if ("STATUS_FAILED".equals(responseType)) {
            return createStatusResult(request, true);
        } else {
            return createStatusResult(request, false);
        }
    }

    private com.huawei.cps.synccpsinterface.api_requestmgr.Result createStatusResult(com.huawei.cps.synccpsinterface.api_requestmgr.Request request, boolean resultFailed) {
        var result = new com.huawei.cps.synccpsinterface.api_requestmgr.Result();
        var header = new com.huawei.cps.synccpsinterface.result.Header();
        header.setVersion(request.getHeader().getVersion());
        header.setOriginatorConversationID(request.getHeader().getOriginatorConversationID());
        header.setConversationID("SIMULATED." + UUID.randomUUID().toString().substring(25));
        result.setHeader(header);

        var body = new com.huawei.cps.synccpsinterface.result.Body();
        body.setResultType(0);
        if (resultFailed) {
            body.setResultCode(-1);
            body.setResultDesc("Something went wrong.");
        } else {
            body.setResultCode(0);
            body.setResultDesc("Process service request successfully.");

            var queryTransactionStatusResult = new com.huawei.cps.synccpsinterface.result.Body.QueryTransactionStatusResult();
            queryTransactionStatusResult.setTransactionStatus("Completed");
            queryTransactionStatusResult.setReceiptNumber("SIMULATED." + Tool.generateDigits(12, false));
            body.setQueryTransactionStatusResult(queryTransactionStatusResult);
        }
        result.setBody(body);
        return result;
    }

    private Response simulateRefundResponse(Request request, String responseType) {
        if ("REFUND_RESPONSE_FAILED".equals(responseType)) {
            return simulateResponse(request, -1, "Something went wrong.");
        } else {
            new Thread(() -> simulateResult(request, false, "REFUND_RESULT_FAILED".equals(responseType))).start();
            return simulateResponse(request, 0, "Accept the service request successfully.");
        }
    }

    private void simulateResult(Request request, boolean timeout, boolean resultFailed) {
        Tool.sleep(timeout ? (onemoneyProperties.getPaymentTimeout() + 5) * 1000L : 5000);
        Result result = new Result();
        com.huawei.cps.cpsinterface.result.Header header = new com.huawei.cps.cpsinterface.result.Header();
        header.setVersion(request.getHeader().getVersion());
        header.setOriginatorConversationID(request.getHeader().getOriginatorConversationID());
        header.setConversationID("SIMULATED." + UUID.randomUUID().toString().substring(25));
        result.setHeader(header);

        com.huawei.cps.cpsinterface.result.Body body = new com.huawei.cps.cpsinterface.result.Body();
        if (resultFailed) {
            body.setResultType(1000);
            body.setResultCode(-1);
            body.setResultDesc("Something went wrong.");
        } else {
            body.setResultType(0);
            body.setResultCode(0);
            body.setResultDesc("Process service request successfully.");

            com.huawei.cps.cpsinterface.result.Body.TransactionResult transactionResult = new com.huawei.cps.cpsinterface.result.Body.TransactionResult();
            transactionResult.setTransactionID("SIMULATED." + Tool.generateDigits(12, false));
            com.huawei.cps.cpsinterface.result.Body.TransactionResult.ResultParameters resultParameters = new com.huawei.cps.cpsinterface.result.Body.TransactionResult.ResultParameters();
            com.huawei.cps.cpsinterface.result.Body.TransactionResult.ResultParameters.ResultParameter resultParameter = new com.huawei.cps.cpsinterface.result.Body.TransactionResult.ResultParameters.ResultParameter();
            resultParameter.setKey("DebitBalance");
            resultParameter.setValue("{\"list\":[{\"accountno\":\"100000000158490415\",\"accounttypename\":\"E-money Account\",\"amount\":\"0.00\",\"currency\":\"USD\"},{\"accountno\":\"100000000158490407\",\"accounttypename\":\"E-money Account\",\"amount\":\"7124.39\",\"currency\":\"ZWL\"},{\"accountno\":\"100000000158490423\",\"accounttypename\":\"E-money Account\",\"amount\":\"0.00\",\"currency\":\"GBP\"},{\"accountno\":\"100000000158490449\",\"accounttypename\":\"E-money Account\",\"amount\":\"0.00\",\"currency\":\"ZAR\"},{\"accountno\":\"100000000158490431\",\"accounttypename\":\"E-money Account\",\"amount\":\"0.00\",\"currency\":\"EUR\"}],\"total\":[{\"amount\":\"0.00\",\"currency\":\"EUR\"},{\"amount\":\"0.00\",\"currency\":\"GBP\"},{\"amount\":\"0.00\",\"currency\":\"USD\"},{\"amount\":\"0.00\",\"currency\":\"ZAR\"},{\"amount\":\"7124.39\",\"currency\":\"ZWL\"}]}");
            resultParameters.getResultParameter().add(resultParameter);
            com.huawei.cps.cpsinterface.result.Body.TransactionResult.ResultParameters.ResultParameter resultParameter2 = new com.huawei.cps.cpsinterface.result.Body.TransactionResult.ResultParameters.ResultParameter();
            resultParameter2.setKey("CreditBalance");
            resultParameters.getResultParameter().add(resultParameter2);
            transactionResult.setResultParameters(resultParameters);
            body.setTransactionResult(transactionResult);
        }
        result.setBody(body);
        resultEndpoint.paymentResult(result);
    }
}

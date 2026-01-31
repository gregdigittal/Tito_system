package cash.ice.mpesa.service.impl;

import cash.ice.common.constant.EntityMetaKey;
import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.utils.Tool;
import cash.ice.mpesa.config.MpesaProperties;
import cash.ice.mpesa.dto.Payment;
import cash.ice.mpesa.entity.MpesaPayment;
import cash.ice.mpesa.error.MpesaException;
import cash.ice.mpesa.repository.MpesaPaymentRepository;
import cash.ice.mpesa.service.MpesaSenderService;
import com.fc.sdk.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Profile(IceCashProfile.NOT_PROD)
@Slf4j
public class MpesaPaymentServiceUatImpl extends MpesaPaymentServiceImpl {

    public MpesaPaymentServiceUatImpl(MpesaSenderService mpesaSenderService, MpesaPaymentRepository mpesaPaymentRepository, MpesaProperties mpesaProperties) {
        super(mpesaSenderService, mpesaPaymentRepository, mpesaProperties);
    }

    @Override
    protected APIResponse sendInboundPayment(Payment payment) {
        if (payment.getMetaData() != null && payment.getMetaData().containsKey(EntityMetaKey.Simulate)) {
            return simulateResponse((String) payment.getMetaData().get(EntityMetaKey.Simulate), "Inbound transaction");
        } else {
            return super.sendInboundPayment(payment);
        }
    }

    @Override
    protected APIResponse sendOutboundPayment(Payment payment) {
        if (payment.getMetaData() != null && payment.getMetaData().containsKey(EntityMetaKey.Simulate)) {
            return simulateResponse((String) payment.getMetaData().get(EntityMetaKey.Simulate), "Outbound transaction");
        } else {
            return super.sendOutboundPayment(payment);
        }
    }

    @Override
    protected APIResponse sendQueryStatus(Payment payment, String paymentId) {
        if (payment.getMetaData() != null && payment.getMetaData().containsKey(EntityMetaKey.Simulate)) {
            return simulateResponse((String) payment.getMetaData().get(EntityMetaKey.Simulate), "Query status");
        } else {
            return super.sendQueryStatus(payment, paymentId);
        }
    }

    @Override
    public void handleResponse(MpesaPayment mpesaPayment, Payment payment) {
        super.handleResponse(mpesaPayment, payment);
        if (mpesaPayment.isPaymentSuccessful()) {
            if (payment.getMetaData() != null && ("REFUND".equals(payment.getMetaData().get(EntityMetaKey.Simulate)))) {
                throw new MpesaException(mpesaPayment, "Simulated exception", ErrorCodes.EC9001);
            }
        }
    }

    @Override
    protected APIResponse sendRefund(MpesaPayment mpesaPayment, Map<String, Object> paymentMetaData) {
        if (paymentMetaData != null && paymentMetaData.containsKey(EntityMetaKey.Simulate)) {
            return simulateResponse((String) paymentMetaData.get(EntityMetaKey.Simulate), "Reversal transaction");
        } else {
            return super.sendRefund(mpesaPayment, paymentMetaData);
        }
    }

    private APIResponse simulateResponse(String simulateValue, String desc) {
        log.debug("  simulating {} response ({})", simulateValue, desc);
        APIResponse response = null;
        if ("UNANSWERED".equals(simulateValue) || "INBOUND_UNANSWERED".equals(simulateValue) && "Inbound transaction".equals(desc)) {
            Tool.sleep(5000);
        } else {
            response = new APIResponse();
            if ("SUCCESS".equals(simulateValue) || "REFUND".equals(simulateValue) || "REFUND_ERROR".equals(simulateValue) && !"Reversal transaction".equals(desc)) {
                response.setStatusLine("HTTP/1.1 201 Created");
                response.setStatusCode(201);
                response.setReason("Created (simulated)");
                response.setResult("{\"output_ResponseCode\":\"INS-0\",\"output_ResponseDesc\":\"Request processed successfully\",\"output_TransactionID\":\"87p1xjr88jft\",\"output_ConversationID\":\"f40929d63386410080ae5073859fd0ef\",\"output_ThirdPartyReference\":\"5XSXY2\"}");
                response.setParameters(Map.of(
                        "output_ResponseCode", "INS-0",
                        "output_ResponseDesc", "Request processed successfully (simulated)",
                        "output_TransactionID", "sim_" + Tool.generateCharacters(8).toLowerCase(),
                        "output_ConversationID", "f40929d63386410080ae5073859fd0ef",
                        "output_ResponseTransactionStatus", "Completed",
                        "output_ThirdPartyReference", "5XSXY2"
                ));
            } else {            // ERROR
                response.setStatusLine("HTTP/1.1 401 Unauthorized");
                response.setStatusCode(401);
                response.setReason("Unauthorized (simulated)");
                response.setResult("{\"output_ResponseCode\":\"INS-6\",\"output_ResponseDesc\":\"Transaction Failed\",\"output_TransactionID\":\"N/A\",\"output_ConversationID\":\"04a9f42f801d4000883eedf477b6398f\",\"output_ThirdPartyReference\":\"5XSXY2\"}");
                response.setParameters(Map.of(
                        "output_ResponseCode", "INS-6",
                        "output_ResponseDesc", "Transaction Failed (simulated)",
                        "output_TransactionID", "N/A",
                        "output_ConversationID", "04a9f42f801d4000883eedf477b6398f",
                        "output_ResponseTransactionStatus", "N/A",
                        "output_ThirdPartyReference", "5XSXY2"
                ));
            }
        }
        return response;
    }
}

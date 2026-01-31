package cash.ice.zim.api.service;

import cash.ice.common.constant.EntityMetaKey;
import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.zim.api.dto.CreateTransactionCardSpResult;
import cash.ice.zim.api.dto.LedgerSpResult;
import cash.ice.zim.api.dto.PaymentResponseZim;
import cash.ice.zim.api.error.SpRetryRequireException;
import cash.ice.zim.api.error.SpReturnedErrorException;
import cash.ice.zim.api.repository.LegacyPaymentDetailsRepository;
import cash.ice.zim.api.repository.LegacyPaymentsRepository;
import cash.ice.zim.api.repository.LegacyWalletsRepository;
import cash.ice.zim.api.util.LegacyStoredProcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import static cash.ice.common.dto.zim.PaymentRequestZim.CURRENCY_CODE;
import static cash.ice.common.dto.zim.PaymentRequestZim.PAYMENT_DESCRIPTION;

@Service
@Profile("!prod-k8s")
@Slf4j
public class ZimDataServiceUat extends ZimDataService {

    public ZimDataServiceUat(LegacyPaymentsRepository paymentsRepository, LegacyPaymentDetailsRepository paymentDetailsRepository, LegacyWalletsRepository walletsRepository, LegacyStoredProcService storedProcService) {
        super(paymentsRepository, paymentDetailsRepository, walletsRepository, storedProcService);
    }

    @Override
    public void obtainAndValidatePaymentData(PaymentRequestZim request) {
        if (request.getMetaData() != null && ("all".equals(request.getMetaData().get("simulateDb")) || "Obtain".equals(request.getMetaData().get("simulateDb")))) {
            log.info("  skipping data db check");
            request.addToMetaData(CURRENCY_CODE, request.getMetaData().getOrDefault("simulateCurrencyCode", "ZWL"));
            request.addToMetaData(PAYMENT_DESCRIPTION, request.getMetaData().getOrDefault("simulatePaymentDescription", "Simulated payment description"));
        } else {
            super.obtainAndValidatePaymentData(request);
        }
    }

    @Override
    public void approveLedgerPayment(PaymentRequestZim request, PaymentResponseZim response) {
        if (request.getMetaData().get(EntityMetaKey.SimulateErrorStep) == Integer.valueOf(41)) {
            response.setSpResult(new CreateTransactionCardSpResult().setSpName("p_Create_Transaction_Card").setResult(2));
            throw new SpReturnedErrorException("'p_Create_Transaction_Card' SP returned Result=2, Error: Simulated SP error", ErrorCodes.EC1110);
        } else if (request.getMetaData().get(EntityMetaKey.SimulateErrorStep) == Integer.valueOf(42) && response.getSpTries() == 1 ||
                request.getMetaData().get(EntityMetaKey.SimulateErrorStep) == Integer.valueOf(43)) {
            response.setSpResult(new CreateTransactionCardSpResult().setSpName("p_Create_Transaction_Card"));
            throw new SpRetryRequireException("'p_Create_Transaction_Card' SP returned Result=null, recheck is required", ErrorCodes.EC1105);
        }
        if (request.getMetaData() != null && ("all".equals(request.getMetaData().get("simulateDb")) || "Ledger".equals(request.getMetaData().get("simulateDb")))) {
            log.info("  skipping db ledger approve");
            if (isMpesaMtpRequest(request)) {
                response.setSpResult(new LedgerSpResult().setSpName("p_Create_Transaction_Card").setResult(1).setMessage("Funds successfully loaded. (simulated)").setTransactionId(99999));
            }
            if (request.isApprovePaymentFlat()) {
                response.setSpResult(new LedgerSpResult().setSpName("p_Payment_Approval").setResult(1).setMessage("Approved (simulated)").setTransactionId(99999));
            }
        } else {
            super.approveLedgerPayment(request, response);
            if (request.getMetaData().get(EntityMetaKey.SimulateErrorStep) == Integer.valueOf(13)) {
                if (response.getSpResult() instanceof CreateTransactionCardSpResult ctcResult) {
                    ctcResult.setResult(0);
                    ctcResult.setError("Simulated SP error response");
                    throw new ICEcashException(String.format("'%s' SP returned Result=%s, Error: %s", ctcResult.getSpName(), 0, "Simulated SP error response"), ErrorCodes.EC1105);
                } else if (response.getSpResult() instanceof LedgerSpResult lResult) {
                    lResult.setResult(0);
                    lResult.setError("Simulated SP error response");
                    throw new ICEcashException(String.format("'%s' SP returned Result=%s, Error: %s", lResult.getSpName(), 0, "Simulated SP error response"), ErrorCodes.EC1105);
                }
            }
        }
        if (request.getMetaData().get(EntityMetaKey.SimulateErrorStep) == Integer.valueOf(44)) {
            response.setSpResult(new CreateTransactionCardSpResult().setSpName("p_Create_Transaction_Card"));
            throw new SpRetryRequireException("'p_Create_Transaction_Card' SP returned Result=null, recheck is required", ErrorCodes.EC1105);
        }
    }

    @Override
    public void failLedgerPayment(PaymentRequestZim request, PaymentResponseZim response) {
        if (request.getMetaData() != null && ("all".equals(request.getMetaData().get("simulateDb")) || "Ledger".equals(request.getMetaData().get("simulateDb")))) {
            log.info("  skipping db ledger fail");
            if (!isMpesaMtpRequest(request) && request.isApprovePaymentFlat()) {
                response.setSpResult(new LedgerSpResult().setSpName("p_Payment_Approval").setResult(1).setError("Simulated transaction fail"));
            }
        } else {
            super.failLedgerPayment(request, response);
        }
    }

    @Override
    protected void cancelApprovedTransaction(PaymentResponseZim response, PaymentRequestZim request) {
        if (request.getMetaData() != null && ("all".equals(request.getMetaData().get("simulateDb")) || "Ledger".equals(request.getMetaData().get("simulateDb")))) {
            log.info("  skipping 'p_Payment_Reversal' SP call: paymentId={}, sessionId={}, createdById={}, channel=API", request.getPaymentId(), request.getSessionId(), request.getCreatedById());
            response.setSpResult(new LedgerSpResult().setSpName("p_Payment_Reversal").setResult(1).setMessage("Simulated transaction cancel"));
        } else {
            super.cancelApprovedTransaction(response, request);
        }
    }

    @Override
    protected void cancelTransactionCard(PaymentRequestZim request, Integer spTransactionId) {
        if (request.getMetaData() != null && ("all".equals(request.getMetaData().get("simulateDb")) || "Ledger".equals(request.getMetaData().get("simulateDb")))) {
            log.info("  skipping cancelling approved(SP) transaction");
        } else {
            super.cancelTransactionCard(request, spTransactionId);
        }
    }

    @Override
    public void milestoneCheck(PaymentRequestZim paymentRequest, Integer step) {
        if (paymentRequest.getMetaData().get(EntityMetaKey.SimulateErrorStep) == step) {
            throw new ICEcashException("Simulated error on step: " + step, ErrorCodes.EC1109);
        }
    }
}

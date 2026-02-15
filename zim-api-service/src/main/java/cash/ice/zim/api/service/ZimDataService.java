package cash.ice.zim.api.service;

import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.common.error.ErrorCodes;
import cash.ice.zim.api.dto.CreateTransactionCardSpResult;
import cash.ice.zim.api.dto.LedgerSpResult;
import cash.ice.zim.api.dto.PaymentResponseZim;
import cash.ice.zim.api.entity.LegacyPaymentDetails;
import cash.ice.zim.api.entity.LegacyPayments;
import cash.ice.zim.api.entity.LegacyWallets;
import cash.ice.zim.api.error.*;
import cash.ice.zim.api.repository.LegacyPaymentDetailsRepository;
import cash.ice.zim.api.repository.LegacyPaymentsRepository;
import cash.ice.zim.api.repository.LegacyWalletsRepository;
import cash.ice.zim.api.util.LegacyStoredProcService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static cash.ice.common.dto.zim.PaymentRequestZim.*;

@Service
@RequiredArgsConstructor
@Profile("prod-k8s")
@Slf4j
public class ZimDataService {
    private static final String TOLLS_PREFUND = "Tolls_PreFund";
    private static final String SYS = "SYS";
    private static final String API = "API";

    private final LegacyPaymentsRepository paymentsRepository;
    private final LegacyPaymentDetailsRepository paymentDetailsRepository;
    private final LegacyWalletsRepository walletsRepository;
    private final LegacyStoredProcService storedProcService;

    public void obtainAndValidatePaymentData(PaymentRequestZim request) {
        if (request.getPaymentId() != null) {           // 1st case
            LegacyPayments payment = paymentsRepository.findById(request.getPaymentId()).orElseThrow(() ->
                    new NotFoundException(String.format("DB Payment (id=%s) does not exist", request.getPaymentId()), ErrorCodes.EC1104));
            log.info("  Obtained {}", payment);
            LegacyPaymentDetails paymentDetails = paymentDetailsRepository.findByPaymentId(request.getPaymentId())
                    .orElseThrow(() -> new NotFoundException(String.format("DB Payment_Details (payment_id=%s) does not exist", request.getPaymentId()), ErrorCodes.EC1104));
            log.info("  Obtained {}", paymentDetails);
            int walletId = request.getWalletId() != null ? request.getWalletId() : 1;
            LegacyWallets wallet = walletsRepository.findById(walletId).orElseThrow(() ->
                    new NotFoundException(String.format("DB Wallet (id=%s) does not exist", walletId), ErrorCodes.EC1104));
            log.info("  Obtained {}", wallet);

            validateRequest(request, payment, paymentDetails);
            if (request.getAmount() == null) {
                request.setAmount(roundIfNeed(paymentDetails.getAmount()));
            }
            request.addToMetaData(CURRENCY_CODE, wallet.getIsoCurrencyCode());
            request.addToMetaData(PAYMENT_DESCRIPTION, payment.getDescription());
            request.addToMetaData(CREATED_BY_ID, payment.getCreatedById());

        } else if (request.getWalletId() != null && request.getTransactionCode() != null && request.getAmount() != null) {     // 3rd case
            LegacyWallets wallet = walletsRepository.findById(request.getWalletId()).orElseThrow(() ->
                    new NotFoundException(String.format("DB Wallet (id=%s) does not exist", request.getWalletId()), ErrorCodes.EC1104));
            log.info("  Obtained {}", wallet);
            request.addToMetaData(CURRENCY_CODE, wallet.getIsoCurrencyCode());

        } else {
            throw new ApiValidationException("Payment ID or (Wallet ID - TransactionCode - Amount) set is required");
        }
    }

    private void validateRequest(PaymentRequestZim request, LegacyPayments payment, LegacyPaymentDetails paymentDetails) {
        if (request.getAmount() != null && !Objects.equals(request.getAmount(), roundIfNeed(paymentDetails.getAmount()))) {
            throw new ApiValidationException("The 'amount' field and the DB 'Payment_Details.amount' do not match");
        } else if (payment.getStatus() != 0) {
            throw new ApiValidationException("Wrong payment status: " + payment.getStatus());
        }
    }

    private BigDecimal roundIfNeed(BigDecimal amount) {
        return amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : null;
    }

    public boolean isMpesaMtpRequest(PaymentRequestZim request) {
        return "mpesa".equals(request.getBankName()) &&
                Objects.equals(request.getWalletId(), 99) &&
                "MTP".equals(request.getTransactionCode());
    }

    public void approveLedgerPayment(PaymentRequestZim request, PaymentResponseZim response) throws SpExecutionException, SpRetryRequireException, SpReturnedErrorException {
        if (isMpesaMtpRequest(request)) {
            log.info("  Calling 'p_Create_Transaction_Card' SP ({})", request.getVendorRef());
            CreateTransactionCardSpResult spResult = storedProcService.createTransactionCard(
                    request.getSessionId(),
                    TOLLS_PREFUND,
                    request.getChannel(),
                    request.getAccountId(),
                    request.getPartnerId(),
                    request.getAccountFundId(),
                    request.getWalletId(),
                    request.getCardNumber(),
                    request.getPaymentDescription(),
                    request.getAmount(),
                    request.getOrganisation(),
                    request.getBankName(),
                    request.getAccountNumber(),
                    request.getVendorRef(),
                    response.getExternalTransactionId());
            log.info("  done SP ({}): {}", request.getVendorRef(), spResult);
            response.setSpResult(spResult);
            if (spResult.getResult() == null) {
                throw new SpRetryRequireException("'p_Create_Transaction_Card' SP returned Result=null, recheck is required", ErrorCodes.EC1105);
            } else if (spResult.getResult() != 1) {
                throw new SpReturnedErrorException(String.format("'p_Create_Transaction_Card' SP returned Result=%s, Error: %s", spResult.getResult(), spResult.getError()), ErrorCodes.EC1110);
            }

        } else if (request.isApprovePaymentFlat()) {
            log.info("  approving Payment in legacy Ledger ('p_Payment_Approval' SP) for {}", request.getVendorRef());
            LedgerSpResult spResult = invokeLedgerSp(request.getPaymentId(), request.getPartnerId(), request.getCreatedById(), 1);
            response.setSpResult(spResult);
            if (spResult.getResult() == null) {
                throw new SpRetryRequireException("'p_Payment_Approval' SP returned Result=null, recheck is required", ErrorCodes.EC1105);
            } else if (spResult.getResult() != 1) {
                throw new SpReturnedErrorException(String.format("'p_Payment_Approval' SP returned Result=%s, Error: %s", spResult.getResult(), spResult.getError()), ErrorCodes.EC1110);
            }
        }
    }

    public boolean checkIfAlreadyApproved(PaymentRequestZim request, PaymentResponseZim response) {
        if (isMpesaMtpRequest(request)) {
            log.info("  Checking TransactionCard exists for vendorRef: {}", request.getVendorRef());
            Integer transactionCardId = paymentsRepository.getTransactionCardIdBy(request.getVendorRef());
            if (transactionCardId != null) {
                CreateTransactionCardSpResult spResult = new CreateTransactionCardSpResult("p_Create_Transaction_Card", Map.of("Result", 1, "Transaction_ID", transactionCardId));
                log.info("  Already exists ({}): {}", request.getVendorRef(), spResult);
                response.setSpResult(spResult);
                return true;
            }
        } else if (request.isApprovePaymentFlat()) {
            log.info("  Checking is payment already approved for vendorRef: {}", request.getVendorRef());
            Integer paymentStatus = paymentsRepository.getPaymentStatus(request.getPaymentId());
            if (paymentStatus != null && paymentStatus == 1) {
                LedgerSpResult spResult = new LedgerSpResult("p_Payment_Approval", Map.of("Result", 1));
                log.info("  Already approved ({}): {}", request.getVendorRef(), spResult);
                response.setSpResult(spResult);
                return true;
            }
        }
        return false;
    }

    public void failLedgerPayment(PaymentRequestZim request, PaymentResponseZim response) {
        if (isMpesaMtpRequest(request)) {
            log.info("  no Ledger needed for mpesa MTP wallet=99 (for failed payment)");
        } else if (request.isApprovePaymentFlat()) {
            log.info("  Failing Payment in legacy Ledger ('p_Payment_Approval' SP)");
            LedgerSpResult spResult = invokeLedgerSp(request.getPaymentId(), request.getPartnerId(), request.getCreatedById(), 2);
            response.setSpResult(spResult);
        }
    }

    private LedgerSpResult invokeLedgerSp(Integer paymentId, Integer partnerId, Integer createdById, int status) {
        LedgerSpResult spResult = storedProcService.approvePayment(
                paymentId,
                0,
                SYS,
                status,
                partnerId,
                createdById);
        log.info("  Done  SP: " + spResult);
        return spResult;
    }

    @Transactional(timeout = 30)
    public void cancelPaymentApprovement(PaymentRequestZim request, PaymentResponseZim response, Object spResult) {
        if (spResult instanceof Map spResultMap) {
            if ("p_Create_Transaction_Card".equals(spResultMap.get("spName"))) {
                if (spResultMap.get("result") == Integer.valueOf(1)) {
                    Integer spTransactionId = (Integer) spResultMap.get("transactionId");
                    if (spTransactionId != null) {
                        cancelTransactionCard(request, spTransactionId);
                        spResultMap.put("error", "Transaction cancelled");
                        response.setSpResult(spResultMap);
                    } else {
                        log.error("  Error cancelling transaction card, transactionId is null, {}", spResultMap);
                    }
                } else {
                    log.error("  ignoring 'p_Create_Transaction_Card' cancelling since SP Result != 1: {}", spResultMap);
                }
            } else if ("p_Payment_Approval".equals(spResultMap.get("spName"))) {
                if (spResultMap.get("result") == Integer.valueOf(1)) {
                    cancelApprovedTransaction(response, request);
                } else {
                    log.error("  ignoring 'p_Payment_Approval' cancelling since SP Result != 1: {}", spResultMap);
                }
            } else {
                log.warn("  Cannot cancel transaction approvement, unknown type: {}, vendorRef: {}", spResult, request.getVendorRef());
            }
        } else {
            log.error("  'spResult' is not a Map: " + spResult);
        }
    }

    protected void cancelApprovedTransaction(PaymentResponseZim response, PaymentRequestZim request) {
        log.info("  Calling 'p_Payment_Reversal' SP");
        LedgerSpResult revSpResult = storedProcService.reversePayment(request.getPaymentId(), request.getSessionId(), API, request.getCreatedById());
        response.setSpResult(revSpResult);
    }

    protected void cancelTransactionCard(PaymentRequestZim request, Integer spTransactionId) {
        log.info("  Cancelling approved(SP) transaction");
        paymentsRepository.updateTransactionCard(spTransactionId, 3, request.getAccountId());
    }

    public void milestoneCheck(PaymentRequestZim paymentRequest, Integer step) {
    }

    public Map<String, String> getManualScripts(PaymentRequestZim request, PaymentResponseZim response) {
        Map<String, String> scripts = new LinkedHashMap<>();
        if (request != null) {
            if (isMpesaMtpRequest(request)) {
                String transactionId = "-";
                if (response != null && response.getSpResult() != null && response.getSpResult() instanceof CreateTransactionCardSpResult spResult) {
                    transactionId = String.valueOf(spResult.getTransactionId());
                }
                String externalTransactionId = response != null ? response.getExternalTransactionId() : null;
                scripts.put("approve", String.format("EXEC p_Create_Transaction_Card @Session_ID = %s, @Mode = 'Tolls_PreFund', @Channel = '%s', @Account_ID = %s, @Partner_ID = %s, @Account_Fund_ID = %s, @Wallet_ID = %s, @Amount = %s, @Card_Number = '%s', @Note = '%s', @Organisation = '%s', @MSISDN = '%s', @Vendor_Reference = '%s', @External_Transaction_Reference = '%s', @Payment_Method = '%s';",
                        request.getSessionId(), request.getChannel(), request.getAccountId(), request.getPartnerId(), request.getAccountFundId(), request.getWalletId(), request.getAmount(), request.getCardNumber(), request.getPaymentDescription(), request.getOrganisation(), request.getAccountNumber(), request.getVendorRef(), externalTransactionId, request.getBankName()));
                scripts.put("reversal", String.format("UPDATE Transactions_Card SET Status_ID = 3, Cancelled_Date = GETDATE(), Cancelled_By = %s WHERE Transactions_Card_ID = %s;",
                        request.getAccountId(), transactionId));
                scripts.put("fail", "-");
            } else {
                scripts.put("approve", String.format("EXEC p_Payment_Approval @Payment_ID = %s, @Session_ID = 0, @Channel = 'SYS', @Status = 1, @Partner_ID = %s, @Account_ID = %s;",
                        request.getPaymentId(), request.getPartnerId(), request.getCreatedById()));
                scripts.put("reversal", String.format("EXEC p_Payment_Reversal @Payment_ID = %s, @Session_ID = %s, @Channel = 'API', @Account_ID = %s;",
                        request.getPaymentId(), request.getSessionId(), request.getCreatedById()));
                scripts.put("fail", String.format("EXEC p_Payment_Approval @Payment_ID = %s, @Session_ID = 0, @Channel = 'SYS', @Status = 2, @Partner_ID = %s, @Account_ID = %s;",
                        request.getPaymentId(), request.getPartnerId(), request.getCreatedById()));
            }
        }
        return scripts;
    }
}

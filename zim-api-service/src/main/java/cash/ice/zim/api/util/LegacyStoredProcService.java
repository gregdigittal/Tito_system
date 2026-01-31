package cash.ice.zim.api.util;

import cash.ice.common.error.ErrorCodes;
import cash.ice.zim.api.dto.CreateTransactionCardSpResult;
import cash.ice.zim.api.dto.LedgerSpResult;
import cash.ice.zim.api.error.SpExecutionException;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LegacyStoredProcService {
    private final DataSource dataSource;

    public LedgerSpResult approvePayment(Integer paymentId, Integer sessionId, String channel, Integer status,
                                         @Nullable Integer partnerId, @Nullable Integer accountId) throws SpExecutionException {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(dataSource)
                .withProcedureName("p_Payment_Approval")
                .declareParameters(
                        new SqlParameter("Payment_ID", Types.INTEGER),
                        new SqlParameter("Session_ID", Types.INTEGER),
                        new SqlParameter("Channel", Types.VARCHAR),
                        new SqlParameter("Account_ID", Types.INTEGER),
                        new SqlParameter("Partner_ID", Types.INTEGER),
                        new SqlParameter("Organisation", Types.VARCHAR),
                        new SqlParameter("Payment_Method", Types.VARCHAR),
                        new SqlParameter("Payment_Method_ID", Types.VARCHAR),
                        new SqlParameter("Payment_Reference", Types.VARCHAR),
                        new SqlParameter("Status", Types.INTEGER),
                        new SqlParameter("Suppress_Output", Types.SMALLINT),
                        new SqlParameter("Notes", Types.VARCHAR),
                        new SqlOutParameter("Result", Types.INTEGER),
                        new SqlOutParameter("Message", Types.VARCHAR),
                        new SqlOutParameter("Error", Types.VARCHAR),
                        new SqlOutParameter("Transaction_ID", Types.INTEGER)
                );
        Map<String, Object> inParams = new HashMap<>();
        inParams.put("Payment_ID", paymentId);
        inParams.put("Session_ID", sessionId);
        inParams.put("Channel", channel);
        inParams.put("Account_ID", accountId);
        inParams.put("Partner_ID", partnerId);
        inParams.put("Organisation", "ICEcash");
        inParams.put("Payment_Method", null);
        inParams.put("Payment_Method_ID", null);
        inParams.put("Payment_Reference", null);
        inParams.put("Status", status);
        inParams.put("Suppress_Output", 0);
        inParams.put("Notes", null);
        Map<String, Object> spResult = executeStoredProc("p_Payment_Approval", simpleJdbcCall, inParams);
        return new LedgerSpResult("p_Payment_Approval", spResult);
    }

    public LedgerSpResult reversePayment(@Nullable Integer paymentId, Integer sessionId, String channel, @Nullable Integer accountId) throws SpExecutionException {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(dataSource)
                .withProcedureName("p_Payment_Reversal")
                .declareParameters(
                        new SqlParameter("Payment_ID", Types.INTEGER),
                        new SqlParameter("Journal_ID", Types.INTEGER),
                        new SqlParameter("Vendor_Ref", Types.VARCHAR),
                        new SqlParameter("PDS_Payment_Log_ID", Types.INTEGER),
                        new SqlParameter("Partner_Reference", Types.VARCHAR),
                        new SqlParameter("Receipt_ID", Types.INTEGER),
                        new SqlParameter("Border_COC_ID", Types.INTEGER),
                        new SqlParameter("Session_ID", Types.INTEGER),
                        new SqlParameter("Channel", Types.VARCHAR),
                        new SqlParameter("Account_ID", Types.INTEGER),
                        new SqlParameter("Partner_ID", Types.INTEGER),
                        new SqlParameter("Organisation", Types.VARCHAR),
                        new SqlParameter("Message_ID", Types.INTEGER),
                        new SqlParameter("Payment_Details", Types.VARCHAR),
                        new SqlParameter("Note", Types.VARCHAR),
                        new SqlParameter("Suppress_Output", Types.BIT),
                        new SqlOutParameter("Result", Types.INTEGER),
                        new SqlOutParameter("Message", Types.VARCHAR),
                        new SqlOutParameter("Error", Types.VARCHAR),
                        new SqlOutParameter("Transaction_ID", Types.INTEGER)
                );
        Map<String, Object> inParams = new HashMap<>();
        inParams.put("Payment_ID", paymentId);
        inParams.put("Journal_ID", null);
        inParams.put("Vendor_Ref", null);
        inParams.put("PDS_Payment_Log_ID", null);
        inParams.put("Partner_Reference", null);
        inParams.put("Receipt_ID", null);
        inParams.put("Border_COC_ID", null);
        inParams.put("Session_ID", sessionId);
        inParams.put("Channel", channel);
        inParams.put("Account_ID", accountId);
        inParams.put("Partner_ID", null);
        inParams.put("Organisation", "ICEcash");
        inParams.put("Message_ID", null);
        inParams.put("Payment_Details", null);
        inParams.put("Note", null);
        inParams.put("Suppress_Output", null);
        Map<String, Object> spResult = executeStoredProc("p_Payment_Reversal", simpleJdbcCall, inParams);
        return new LedgerSpResult("p_Payment_Reversal", spResult);
    }

    public CreateTransactionCardSpResult createTransactionCard(@Nullable Integer sessionId, String mode, String channel, Integer accountId,
                                                               Integer partnerId, @Nullable Integer accountFundId, Integer walletId, @Nullable String cardNumber,
                                                               @Nullable String note, BigDecimal amount, @Nullable String organisation, @Nullable String paymentMethod,
                                                               @Nullable String msisdn, @Nullable String vendorRef, @Nullable String externalTransactionId) throws SpExecutionException {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(dataSource)
                .withProcedureName("p_Create_Transaction_Card")
                .declareParameters(
                        new SqlParameter("Session_ID", Types.INTEGER),
                        new SqlParameter("Mode", Types.VARCHAR),
                        new SqlParameter("Channel", Types.VARCHAR),
                        new SqlParameter("Account_ID", Types.INTEGER),
                        new SqlParameter("Partner_ID", Types.INTEGER),
                        new SqlParameter("Account_Fund_ID", Types.INTEGER),
                        new SqlParameter("Account_Sender_ID", Types.INTEGER),
                        new SqlParameter("Wallet_ID", Types.INTEGER),
                        new SqlParameter("Amount", Types.DECIMAL),
                        new SqlParameter("Card_Number", Types.VARCHAR),
                        new SqlParameter("VRN", Types.VARCHAR),
                        new SqlParameter("Note", Types.VARCHAR),
                        new SqlParameter("Location_ID", Types.INTEGER),
                        new SqlParameter("Organisation", Types.VARCHAR),
                        new SqlParameter("PVV", Types.VARCHAR),
                        new SqlParameter("Reference_new", Types.VARCHAR),
                        new SqlParameter("MSISDN", Types.VARCHAR),
                        new SqlParameter("Vendor_Reference", Types.VARCHAR),
                        new SqlParameter("External_Transaction_Reference", Types.VARCHAR),
                        new SqlParameter("Reference", Types.VARCHAR),
                        new SqlParameter("Transaction_Reference", Types.INTEGER),
                        new SqlParameter("Reference_Type", Types.VARCHAR),
                        new SqlParameter("PartnerReference", Types.VARCHAR),
                        new SqlParameter("Payment_Method", Types.VARCHAR),
                        new SqlParameter("Override_Overdraft_Check", Types.BIT),
                        new SqlParameter("Suppress_Output", Types.BIT),
                        new SqlOutParameter("Transaction_ID", Types.INTEGER),
                        new SqlOutParameter("DR_Account_ID", Types.INTEGER),
                        new SqlOutParameter("CR_Account_ID", Types.INTEGER),
                        new SqlOutParameter("Balance", Types.DECIMAL),
                        new SqlOutParameter("DR_Fees", Types.DECIMAL),
                        new SqlOutParameter("Result", Types.INTEGER),
                        new SqlOutParameter("Message", Types.VARCHAR),
                        new SqlOutParameter("Error", Types.VARCHAR)
                );
        Map<String, Object> inParams = new HashMap<>();
        inParams.put("Session_ID", sessionId);
        inParams.put("Mode", mode);
        inParams.put("Channel", channel);
        inParams.put("Account_ID", accountId);
        inParams.put("Partner_ID", partnerId);
        inParams.put("Account_Fund_ID", accountFundId);
        inParams.put("Account_Sender_ID", null);
        inParams.put("Wallet_ID", walletId);
        inParams.put("Amount", amount);
        inParams.put("Card_Number", cardNumber);
        inParams.put("VRN", null);
        inParams.put("Note", note);
        inParams.put("Location_ID", null);
        inParams.put("Organisation", organisation);
        inParams.put("PVV", null);
        inParams.put("Reference_new", null);
        inParams.put("MSISDN", msisdn);
        inParams.put("Vendor_Reference", vendorRef);
        inParams.put("External_Transaction_Reference", externalTransactionId);
        inParams.put("Reference", null);
        inParams.put("Transaction_Reference", null);
        inParams.put("Reference_Type", null);
        inParams.put("PartnerReference", null);
        inParams.put("Payment_Method", paymentMethod);
        inParams.put("Override_Overdraft_Check", 1);
        inParams.put("Suppress_Output", null);
        Map<String, Object> spResult = executeStoredProc("p_Create_Transaction_Card", simpleJdbcCall, inParams);
        return new CreateTransactionCardSpResult("p_Create_Transaction_Card", spResult);
    }

    private Map<String, Object> executeStoredProc(String spName, SimpleJdbcCall simpleJdbcCall, Map<String, Object> inParams) throws SpExecutionException {
        try {
            return simpleJdbcCall.execute(inParams);
        } catch (Exception e) {
            throw new SpExecutionException(String.format("'%s' SP execution error: %s: %s", spName, e.getClass().getCanonicalName(), e.getMessage()), ErrorCodes.EC1105);
        }
    }
}

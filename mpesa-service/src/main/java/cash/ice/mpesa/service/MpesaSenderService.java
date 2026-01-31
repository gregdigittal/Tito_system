package cash.ice.mpesa.service;

import com.fc.sdk.APIResponse;

import java.math.BigDecimal;

public interface MpesaSenderService {

    APIResponse sendInboundRequest(String transactionReference, String msisdn, BigDecimal amount);

    APIResponse sendOutboundRequest(String transactionReference, String msisdn, BigDecimal amount);

    APIResponse sendB2bRequest(String transactionReference, String primaryBusinessCode, String receiverBusinessCode, BigDecimal amount);

    APIResponse sendReversalRequest(String transactionId, BigDecimal optionalAmount);

    APIResponse sendQueryTransactionStatusRequest(String transactionOrConversationOrVendorId);

    APIResponse sendQueryNameRequest(String msisdn);
}

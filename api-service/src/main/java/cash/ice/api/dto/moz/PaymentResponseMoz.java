package cash.ice.api.dto.moz;

import cash.ice.common.dto.PaymentResponse;
import cash.ice.common.dto.ResponseStatus;
import cash.ice.common.utils.Tool;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class PaymentResponseMoz extends PaymentResponse {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, BalanceResponse> accountBalances;           // Map<accountNumber, BalanceResponse>
    private BigDecimal subsidyBalance;
    @JsonIgnore
    private Integer initiatorEntityId;

    public static PaymentResponseMoz success(String vendorRef, String transactionId, Map<String, BalanceResponse> accountBalances, BigDecimal balance, BigDecimal subsidyBalance, String primaryMsisdn, String locale) {
        PaymentResponseMoz response = new PaymentResponseMoz();
        response.setVendorRef(vendorRef);
        response.setDate(Tool.currentDateTime());
        response.setStatus(ResponseStatus.SUCCESS);
        response.setMessage("Transaction processed successfully");
        response.setTransactionId(transactionId);
        response.setAccountBalances(accountBalances);
        response.setBalance(balance);
        response.setSubsidyBalance(subsidyBalance);
        response.setPrimaryMsisdn(primaryMsisdn);
        response.setLocale(locale);
        return response;
    }

    public static PaymentResponseMoz success(PaymentResponse paymentResponse) {
        PaymentResponseMoz response = new PaymentResponseMoz();
        response.setDate(Tool.currentDateTime());
        response.setStatus(ResponseStatus.SUCCESS);
        response.setMessage("Transaction offload completed");
        response.setBalance(paymentResponse.getBalance());
        if (paymentResponse instanceof PaymentResponseMoz paymentResponseMoz) {
            response.setSubsidyBalance(paymentResponseMoz.subsidyBalance);
            response.setPrimaryMsisdn(paymentResponseMoz.getPrimaryMsisdn());
            response.setLocale(paymentResponseMoz.getLocale());
        }
        return response;
    }

    public static PaymentResponse error(String vendorRef, String errorCode, String errorMessage, Integer initiatorEntityId) {
        PaymentResponseMoz response = new PaymentResponseMoz();
        response.setVendorRef(vendorRef);
        response.setDate(Tool.currentDateTime());
        response.setStatus(ResponseStatus.ERROR);
        response.setErrorCode(errorCode);
        response.setMessage(errorMessage);
        response.setInitiatorEntityId(initiatorEntityId);
        return response;
    }

    @Data
    @Accessors(chain = true)
    public static class BalanceResponse {
        private int accountTypeId;
        private String accountType;
        private String currencyCode;
        private BigDecimal balance;
    }
}

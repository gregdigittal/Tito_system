package cash.ice.common.dto.zim;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class PaymentRequestZim {
    public static final String PAYMENT_ID = "paymentId";
    public static final String PAYMENT_COLLECTION_ID = "paymentCollectionId";
    public static final String APPROVE_PAYMENT_FLAT = "approvePaymentFlat";
    public static final String TRANSACTION_CODE = "transactionCode";
    public static final String WALLET_ID = "walletId";
    public static final String SESSION_ID = "sessionId";
    public static final String CHANNEL = "channel";
    public static final String ACCOUNT_ID = "accountId";
    public static final String PARTNER_ID = "partnerId";
    public static final String ACCOUNT_FUND_ID = "accountFundId";
    public static final String CARD_NUMBER = "cardNumber";
    public static final String ORGANISATION = "organisation";

    public static final String CURRENCY_CODE = "currencyCode";
    public static final String PAYMENT_DESCRIPTION = "paymentDescription";
    public static final String CREATED_BY_ID = "createdById";
    public static final String EXPIRATION_TIME = "expirationTime";

    @Id
    @NotBlank(message = "'vendorRef' field is required")
    private String vendorRef;
    private Integer partnerId;
    @NotBlank(message = "'bankName' field is required")
    private String bankName;
    @NotBlank(message = "'accountNumber' field is required")
    private String accountNumber;
    private BigDecimal amount;
    private Map<String, Object> metaData;

    public void addToMetaData(String key, Object value) {
        if (metaData == null) {
            metaData = new HashMap<>();
        }
        metaData.put(key, value);
    }

    @JsonIgnore
    public Integer getPaymentId() {
        return metaData != null ? (Integer) metaData.get(PAYMENT_ID) : null;
    }

    @JsonIgnore
    public Integer getPaymentCollectionId() {
        return metaData != null ? (Integer) metaData.get(PAYMENT_COLLECTION_ID) : null;
    }

    @JsonIgnore
    public boolean isApprovePaymentFlat() {
        return metaData != null && metaData.get(APPROVE_PAYMENT_FLAT) == Boolean.TRUE;
    }

    @JsonIgnore
    public String getTransactionCode() {
        return metaData != null ? (String) metaData.get(TRANSACTION_CODE) : null;
    }

    @JsonIgnore
    public String getCurrencyCode() {
        return metaData != null ? (String) metaData.get(CURRENCY_CODE) : null;
    }

    @JsonIgnore
    public String getPaymentDescription() {
        return metaData != null ? (String) metaData.get(PAYMENT_DESCRIPTION) : null;
    }

    @JsonIgnore
    public Integer getCreatedById() {
        return metaData != null ? (Integer) metaData.get(CREATED_BY_ID) : null;
    }

    @JsonIgnore
    public Long getExpirationTime() {
        return metaData != null ? (Long) metaData.get(EXPIRATION_TIME) : null;
    }

    @JsonIgnore
    public Integer getWalletId() {
        return metaData != null ? (Integer) metaData.get(WALLET_ID) : null;
    }

    @JsonIgnore
    public Integer getSessionId() {
        return metaData != null ? (Integer) metaData.get(SESSION_ID) : null;
    }

    @JsonIgnore
    public String getChannel() {
        return metaData != null ? (String) metaData.get(CHANNEL) : null;
    }

    @JsonIgnore
    public Integer getAccountId() {
        return metaData != null ? (Integer) metaData.get(ACCOUNT_ID) : null;
    }

    @JsonIgnore
    public Integer getPartnerId() {
        return metaData != null ? (Integer) metaData.get(PARTNER_ID) : null;
    }

    @JsonIgnore
    public Integer getAccountFundId() {
        return metaData != null ? (Integer) metaData.get(ACCOUNT_FUND_ID) : null;
    }

    @JsonIgnore
    public String getCardNumber() {
        return metaData != null ? (String) metaData.get(CARD_NUMBER) : null;
    }

    @JsonIgnore
    public String getOrganisation() {
        return metaData != null ? (String) metaData.get(ORGANISATION) : null;
    }
}

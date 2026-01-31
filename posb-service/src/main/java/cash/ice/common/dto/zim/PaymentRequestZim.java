package cash.ice.common.dto.zim;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@Data
@Accessors(chain = true)
@Serdeable.Serializable
@Serdeable.Deserializable
public class PaymentRequestZim {
    public static final String PAYMENT_ID = "paymentId";
    public static final String PAYMENT_COLLECTION_ID = "paymentCollectionId";
    public static final String APPROVE_PAYMENT_FLAT = "approvePaymentFlat";
    public static final String TRANSACTION_CODE = "transactionCode";
    public static final String WALLET_ID = "walletId";

    public static final String CURRENCY_CODE = "currencyCode";
    public static final String PAYMENT_DESCRIPTION = "paymentDescription";

    private String vendorRef;
    private Integer partnerId;
    private String bankName;
    private String accountNumber;
    private BigDecimal amount;
    private Map<String, Object> metaData;

    public void addToMetaData(String key, Object value) {
        if (metaData == null) {
            metaData = new HashMap<>();
        }
        metaData.put(key, value);
    }

    public Integer getPaymentId() {
        return metaData != null ? (Integer) metaData.get(PAYMENT_ID) : null;
    }

    public Integer getPaymentCollectionId() {
        return metaData != null ? (Integer) metaData.get(PAYMENT_COLLECTION_ID) : null;
    }

    public boolean isApprovePaymentFlat() {
        return metaData != null && metaData.get(APPROVE_PAYMENT_FLAT) == Boolean.TRUE;
    }

    public String getTransactionCode() {
        return metaData != null ? (String) metaData.get(TRANSACTION_CODE) : null;
    }

    public String getCurrencyCode() {
        return metaData != null ? (String) metaData.get(CURRENCY_CODE) : null;
    }

    public String getPaymentDescription() {
        return metaData != null ? (String) metaData.get(PAYMENT_DESCRIPTION) : null;
    }

    public Integer getWalletId() {
        return metaData != null ? (Integer) metaData.get(WALLET_ID) : null;
    }

    public String getSimulate() {
        return metaData != null ? (String) metaData.get("simulate") : null;            // todo
    }
}
package cash.ice.api.dto;

import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.PaymentLine;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class PaymentLineView {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer id;
    private Integer accountId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String details;
    @NotNull
    private BigDecimal amount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String currency;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String transactionCode;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer transactionId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> meta;

    public Map<String, Object> getMetaData() {
        return meta;
    }

    public void setMetaData(String jsonString) {
        this.meta = Tool.jsonStringToMap(jsonString);
    }

    public PaymentLine toPaymentLine() {
        return new PaymentLine()
                .setId(id)
                .setAccountId(accountId)
                .setDetails(details)
                .setAmount(amount)
                .setCurrency(currency)
                .setTransactionCode(transactionCode)
                .setTransactionId(transactionId)
                .setMeta(meta != null ? new HashMap<>(meta) : null);
    }

    public static PaymentLineView create(PaymentLine paymentLine) {
        return new PaymentLineView()
                .setId(paymentLine.getId())
                .setAccountId(paymentLine.getAccountId())
                .setDetails(paymentLine.getDetails())
                .setAmount(paymentLine.getAmount())
                .setCurrency(paymentLine.getCurrency())
                .setTransactionCode(paymentLine.getTransactionCode())
                .setTransactionId(paymentLine.getTransactionId())
                .setMeta(paymentLine.getMeta() != null ? new HashMap<>(paymentLine.getMeta()) : null);
    }
}

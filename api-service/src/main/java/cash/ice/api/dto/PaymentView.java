package cash.ice.api.dto;

import cash.ice.api.entity.zim.Payment;
import cash.ice.api.entity.zim.PaymentStatus;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.AccountSide;
import cash.ice.sqldb.entity.PaymentLine;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class PaymentView {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer id;
    @NotNull
    private Integer accountId;
    @NotNull
    private AccountSide accountSide;
    private PaymentStatus status;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String description;
    private Integer count;
    private BigDecimal total;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer taxDeclarationId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer taxReasonId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer paymentCollectionId;
    private LocalDateTime createdDate;
    private List<PaymentLineView> paymentLines = new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<PaymentDocument> documents;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> meta;

    public Payment toPayment() {
        return new Payment()
                .setId(id)
                .setAccountId(accountId)
                .setAccountSide(accountSide)
                .setStatus(status)
                .setDescription(description)
                .setCount(count)
                .setTotal(total)
                .setTaxDeclarationId(taxDeclarationId)
                .setTaxReasonId(taxReasonId)
                .setPaymentCollectionId(paymentCollectionId)
                .setCreatedDate(createdDate)
                .setDocuments(documents != null ? new ArrayList<>(documents) : null)
                .setMeta(meta != null ? new HashMap<>(meta) : null);
    }

    public static PaymentView create(Payment payment, List<PaymentLine> paymentLines) {
        return new PaymentView()
                .setId(payment.getId())
                .setAccountId(payment.getAccountId())
                .setAccountSide(payment.getAccountSide())
                .setStatus(payment.getStatus())
                .setDescription(payment.getDescription())
                .setCount(payment.getCount())
                .setTotal(payment.getTotal())
                .setTaxDeclarationId(payment.getTaxDeclarationId())
                .setTaxReasonId(payment.getTaxReasonId())
                .setPaymentCollectionId(payment.getPaymentCollectionId())
                .setCreatedDate(payment.getCreatedDate())
                .setPaymentLines(paymentLines != null ? paymentLines.stream().map(PaymentLineView::create).toList() : null)
                .setDocuments(payment.getDocuments() != null ? new ArrayList<>(payment.getDocuments()) : null)
                .setMeta(payment.getMeta() != null ? new HashMap<>(payment.getMeta()) : null);
    }

    public Map<String, Object> getMetaData() {
        return meta;
    }

    public void setMetaData(String jsonString) {
        this.meta = Tool.jsonStringToMap(jsonString);
    }
}

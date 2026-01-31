package cash.ice.api.dto;

import cash.ice.sqldb.entity.AuthorisationType;
import cash.ice.sqldb.entity.LimitTier;
import cash.ice.sqldb.entity.PaymentDirection;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class TransactionLimitView {
    @NotNull
    private Integer id;

    private String currency;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String transactionCode;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer kycStatusId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String entityType;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String accountType;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String initiatorType;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LimitTier tier;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private AuthorisationType authorisationType;
    private PaymentDirection direction;

    private boolean active;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal transactionMinLimit;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal transactionMaxLimit;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal dailyLimit;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal weeklyLimit;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal monthlyLimit;
    private LocalDateTime createdDate;

    public String criteriaToString() {
        return "TransactionLimitView{" +
                "currency='" + currency + '\'' +
                (transactionCode != null ? ", transactionCode='" + transactionCode + '\'' : "") +
                (kycStatusId != null ? ", kycStatusId=" + kycStatusId : "") +
                (entityType != null ? ", entityType='" + entityType + '\'' : "") +
                (accountType != null ? ", accountType='" + accountType + '\'' : "") +
                (initiatorType != null ? ", initiatorType='" + initiatorType + '\'' : "") +
                (tier != null ? ", tier=" + tier : "") +
                (authorisationType != null ? ", authorisationType=" + authorisationType : "") +
                (direction != null ? ", direction=" + direction : "") +
                '}';
    }
}

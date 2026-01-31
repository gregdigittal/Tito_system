package cash.ice.common.dto.fee;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class FeeEntry {
    private Integer feeId;
    private Integer transactionCodeId;
    private String transactionCodeDescription;
    private boolean affordabilityCheck;
    private BigDecimal drAccountBalanceMinimum;
    private BigDecimal drAccountOverdraftLimit;
    private Integer drAccountId;
    private Integer drAccountTypeId;
    private Integer drEntityId;
    private String drAuthorisationTypeString;
    private Integer crAccountId;
    private Integer crAccountTypeId;
    private Integer crEntityId;
    private String crAuthorisationTypeString;

    private String drEntityFirstName;
    private String drEntityLastName;
    private String crEntityFirstName;
    private String crEntityLastName;
    private BigDecimal sourceAmount;
    private BigDecimal amount;
}

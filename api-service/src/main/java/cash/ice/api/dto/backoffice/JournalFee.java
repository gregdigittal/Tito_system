package cash.ice.api.dto.backoffice;

import cash.ice.sqldb.entity.ChargeType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class JournalFee {
    private Integer feeId;
    private BigDecimal amount;
    @JsonIgnore
    private ChargeType chargeType;
    @JsonIgnore
    private Integer processOrder;
    @JsonIgnore
    private Integer drAccountId;
    @JsonIgnore
    private Integer crAccountId;
}

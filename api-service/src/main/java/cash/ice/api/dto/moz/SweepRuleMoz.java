package cash.ice.api.dto.moz;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Phase 8-6: Sweep rule view for GraphQL. */
@Data
@Accessors(chain = true)
public class SweepRuleMoz {
    private Integer id;
    private Integer accountId;
    private String destinationType;
    private String destinationRef;
    private String triggerType;
    private String scheduleExpression;
    private BigDecimal thresholdAmount;
    private Boolean active;
    private LocalDateTime createdAt;
}

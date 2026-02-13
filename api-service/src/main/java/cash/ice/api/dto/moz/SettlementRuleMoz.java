package cash.ice.api.dto.moz;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** Phase 8-5: Settlement rule view for GraphQL. */
@Data
@Accessors(chain = true)
public class SettlementRuleMoz {
    private Integer id;
    private Integer entityId;
    private String ruleName;
    private String shareJson;
    private Boolean active;
    private LocalDateTime createdAt;
}

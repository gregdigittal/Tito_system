package cash.ice.api.dto.moz;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Phase 8-4: Trip view for GraphQL type TripMoz. */
@Data
@Accessors(chain = true)
public class TripMoz {
    private Integer id;
    private LocalDateTime tapInAt;
    private LocalDateTime tapOutAt;
    private Integer routeId;
    private String routeName;
    private BigDecimal fare;
    private String currency;
    private String status;
    private Integer deviceId;
}

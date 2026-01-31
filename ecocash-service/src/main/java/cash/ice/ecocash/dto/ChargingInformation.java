package cash.ice.ecocash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class ChargingInformation {
    private BigDecimal amount;
    private String currency;
    private String description;
}

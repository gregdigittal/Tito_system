package cash.ice.fbc.dto.flexcube;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class FlexcubeBalanceResponse implements Serializable {
    private String resultCode;
    private String resultDescription;
    private String account;
    private String branch;
    private String currency;
    private BigDecimal availableBalance;
    private BigDecimal ledgerBalance;
    private Instant createdTime;
}

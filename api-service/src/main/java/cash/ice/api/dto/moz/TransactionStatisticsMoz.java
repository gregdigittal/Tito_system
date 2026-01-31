package cash.ice.api.dto.moz;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Accessors(chain = true)
public class TransactionStatisticsMoz {
    private LocalDate day;
    private int trips;
    private BigDecimal income = BigDecimal.ZERO;
    private BigDecimal prepaidExpenses = BigDecimal.ZERO;
    private BigDecimal subsidyExpenses = BigDecimal.ZERO;
}

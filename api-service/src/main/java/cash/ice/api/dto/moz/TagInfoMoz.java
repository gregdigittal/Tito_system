package cash.ice.api.dto.moz;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class TagInfoMoz {
    private BigDecimal prepaidBalance;
    private BigDecimal subsidyBalance;
    private String firstName;
    private String lastName;
    private String accountNumber;
    private String status;
}

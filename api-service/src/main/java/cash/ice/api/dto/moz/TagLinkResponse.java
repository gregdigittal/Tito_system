package cash.ice.api.dto.moz;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class TagLinkResponse {
    private BigDecimal prepaidBalance;
    private BigDecimal subsidyBalance;
    private String firstName;
    private String lastName;
}

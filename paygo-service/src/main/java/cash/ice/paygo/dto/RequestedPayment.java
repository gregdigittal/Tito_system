package cash.ice.paygo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class RequestedPayment {
    private BigDecimal amount;
    private String currencyCode;
    private String narration;
    private int expirySeconds;
    private String initiator;
}

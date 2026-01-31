package cash.ice.ecocash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class Payment {
    private String vendorRef;
    private String tx;
    private String currencyCode;
    private String initiator;
    private String partnerId;
    private BigDecimal amount;
    private Object pendingRequest;
    private Map<String, Object> metaData = new HashMap<>();
}

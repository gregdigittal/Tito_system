package cash.ice.mpesa.dto;

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
    private Type paymentType;
    private String msisdn;
    private BigDecimal amount;
    private Map<String, Object> metaData = new HashMap<>();

    public enum Type {
        Inbound, Outbound, B2B;
    }
}

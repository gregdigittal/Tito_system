package cash.ice.ecocash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class PaymentAmount {
    private ChargingInformation charginginformation;
    private ChargeMetaData chargeMetaData;
    private BigDecimal totalAmountCharged;
}

package cash.ice.posb.dto;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@MappedEntity("PosbPaymentRequest")
@Data
@Accessors(chain = true)
public class PaymentRequest {
    @Id
    @GeneratedValue
    private String id;
    private String vendorRef;
    private String currencyCode;
    private BigDecimal amount;
}

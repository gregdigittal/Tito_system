package cash.ice.posb.dto.posb;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@NoArgsConstructor
@Data
@Accessors(chain = true)
@Serdeable.Serializable
@Serdeable.Deserializable
public class PosbInstructionRequest {
    private String paymentReference;
    private String customerAccountNumber;
    private BigDecimal amount;
    private String description;
    private String currency;
}

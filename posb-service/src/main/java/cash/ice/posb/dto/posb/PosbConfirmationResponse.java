package cash.ice.posb.dto.posb;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
@Serdeable.Serializable
@Serdeable.Deserializable
public class PosbConfirmationResponse {
    private String paymentReference;
    private String status;
    private Integer responseCode;
    private String narrative;
    private Integer code;
    private String message;
}

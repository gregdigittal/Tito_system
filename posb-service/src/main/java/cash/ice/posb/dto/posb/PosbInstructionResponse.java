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
public class PosbInstructionResponse {
    private String paymentReference;
    private String icecashPoolAccountNumber;
    private String currency;
    private String customerName;
    private String customerMobileNumber;
    private String status;
    private String narrative;
    private String otpExpiringTime;
    private Integer code;
    private String message;
}

package cash.ice.common.dto.zim;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class PaymentOtpRequestZim {
    @NotBlank(message = "'vendorRef' field is required")
    private String vendorRef;
    @NotBlank(message = "'otp' field is required")
    private String otp;
}

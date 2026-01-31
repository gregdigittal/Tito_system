package cash.ice.common.dto.zim;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class PaymentOtpRequestZim {
    private String vendorRef;
    private String otp;
}

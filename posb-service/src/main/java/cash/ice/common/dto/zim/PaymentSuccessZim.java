package cash.ice.common.dto.zim;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Accessors(chain = true)
public class PaymentSuccessZim {
    private String vendorRef;
    private String bankName;
    private String transactionId;
}

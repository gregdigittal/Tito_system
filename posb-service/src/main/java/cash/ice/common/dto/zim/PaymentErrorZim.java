package cash.ice.common.dto.zim;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class PaymentErrorZim {
    private String vendorRef;
    private String message;
    private String errorCode;
    private LocalDateTime date;
}

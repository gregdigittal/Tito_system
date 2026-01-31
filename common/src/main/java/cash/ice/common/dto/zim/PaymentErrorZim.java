package cash.ice.common.dto.zim;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Accessors(chain = true)
public class PaymentErrorZim {
    private String vendorRef;
    private String message;
    private String errorCode;
    private LocalDateTime date;
    private Object spResult;

    public PaymentErrorZim(String vendorRef, String message, String errorCode, LocalDateTime date) {
        this.vendorRef = vendorRef;
        this.message = message;
        this.errorCode = errorCode;
        this.date = date;
    }
}

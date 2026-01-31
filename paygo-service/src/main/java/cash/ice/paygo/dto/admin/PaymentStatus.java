package cash.ice.paygo.dto.admin;

import cash.ice.paygo.dto.PaygoCallbackResponse;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class PaymentStatus {
    private String merchantPaymentId;
    private BigDecimal amount;
    private boolean complete;
    private boolean success;
    private String created;
    private String updated;
    private String currencyCode;
    private String expiresAt;
    private String initiator;
    private String narration;
    private String reference;
    private String token;
    private PaygoCallbackResponse[] advices;
}

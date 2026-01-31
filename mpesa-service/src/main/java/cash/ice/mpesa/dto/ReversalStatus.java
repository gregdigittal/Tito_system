package cash.ice.mpesa.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class ReversalStatus {
    private Boolean refunded;
    private String originalTransactionId;
    private String responseStatus;
    private String response;
    private String transactionStatus;
    private String transactionStatusResponse;
    private String reversalTransactionId;
    private String reversalConversationId;
}

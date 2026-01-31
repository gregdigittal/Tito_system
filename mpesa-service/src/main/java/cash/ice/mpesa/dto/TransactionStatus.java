package cash.ice.mpesa.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class TransactionStatus {
    private String queryReference;
    private int statusCode;
    private String statusReason;
    private String responseCode;
    private String responseDesc;
    private String responseTransactionStatus;
}

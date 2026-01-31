package cash.ice.fbc.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class FbcTransferSubmissionRequest {
    private BigDecimal amount;
    private String currency;
    private String externalReference;
    private String initiatorID;
    private String paymentDetails;
    private String sourceBranchCode;
    private String sourceAccountNumber;
}

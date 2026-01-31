package cash.ice.ecocash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class ReversalStatus {
    private Boolean refunded;
    private String originalEcocashReference;
    private String transactionOperationStatus;
    private String clientCorrelator;
    private Instant time;
}

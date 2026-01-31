package cash.ice.ledger.performance;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class TestPerformanceRequest {
    @NotEmpty
    private String logic;
    @NotNull
    private Integer recordsPerAccount;
    private Integer transactionLines;
    private Integer threads;
    @NotNull
    private Integer totalInvokes;
    private Boolean saveResults;
}

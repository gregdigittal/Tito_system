package cash.ice.ledger.performance;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotNull;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class TestPerformanceInitRequest {
    @NotNull
    private Integer totalRecords;
    private Integer tasksQueueCapacity;
    private Integer threads;
}

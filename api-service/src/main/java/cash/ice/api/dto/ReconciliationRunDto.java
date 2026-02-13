package cash.ice.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Phase 8-13: Reconciliation run summary for Admin read API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReconciliationRunDto {

    private Integer id;
    private LocalDate businessDate;
    private String deviceId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String status;
    private Integer serverCount;
    private Integer deviceCount;
    private Integer matchedCount;
    private Integer mismatchCount;
    private String detailJson;
}

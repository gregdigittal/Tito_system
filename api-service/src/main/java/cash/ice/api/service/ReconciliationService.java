package cash.ice.api.service;

import cash.ice.sqldb.entity.ReconciliationRun;
import cash.ice.sqldb.repository.ReconciliationRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Phase 8-13: Daily reconciliation — server vs device. Stub: persists a run with 0 counts.
 * TODO: Load server transactions for date; load device data (8-14); match; set counts and detail_json.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReconciliationService {

    private final ReconciliationRunRepository reconciliationRunRepository;

    @Transactional(timeout = 30)
    public void runForDate(LocalDate businessDate) {
        LocalDateTime started = LocalDateTime.now();
        ReconciliationRun run = new ReconciliationRun()
                .setBusinessDate(businessDate)
                .setStartedAt(started)
                .setStatus("RUNNING");
        run = reconciliationRunRepository.save(run);
        try {
            // TODO: load server tx, device data, compare, compute counts
            run.setServerCount(0);
            run.setDeviceCount(0);
            run.setMatchedCount(0);
            run.setMismatchCount(0);
            run.setFinishedAt(LocalDateTime.now());
            run.setStatus("COMPLETED");
        } catch (Exception e) {
            log.warn("Reconciliation failed for {}: {}", businessDate, e.getMessage());
            run.setStatus("FAILED");
            run.setFinishedAt(LocalDateTime.now());
        }
        reconciliationRunRepository.save(run);
    }
}

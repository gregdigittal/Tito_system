package cash.ice.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Phase 8-11: EOD settlement orchestration — settlement (8-5) → TiTo fee (8-10) → sweep (8-6).
 * Order ensures correct balances before fee deduction and before sweep payouts.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EodSettlementService {

    private final SettlementService settlementService;
    private final TitoRevenueFeeService titoRevenueFeeService;
    private final SweepExecutionService sweepExecutionService;

    /**
     * Run full EOD for the given business date. Idempotency should be enforced per step (e.g. settlement run stored per date).
     */
    public void runForDate(LocalDate businessDate) {
        log.info("EOD settlement started for business date {}", businessDate);
        // Step 1: Settlement (8-5)
        settlementService.runForDate(businessDate);
        // Step 2: TiTo fee (8-10)
        titoRevenueFeeService.runForDate(businessDate);
        // Step 3: Sweep (8-6)
        sweepExecutionService.runScheduledForDate(businessDate);
        log.info("EOD settlement finished for business date {}", businessDate);
    }
}

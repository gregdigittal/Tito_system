package cash.ice.api.service;

import cash.ice.sqldb.repository.SettlementRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Phase 8-5: Settlement — split revenue per Sacco/entity according to settlement_rule (driver %, conductor %, etc.).
 * EOD step 1. Stub: logs rule count; actual split posting not implemented.
 *
 * @deprecated Incomplete implementation. Use only for EOD orchestration; actual posting is not performed.
 */
@Deprecated(since = "0.1.1", forRemoval = false)
@Service
@Slf4j
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRuleRepository settlementRuleRepository;

    public void runForDate(LocalDate businessDate) {
        long ruleCount = settlementRuleRepository.findAll().stream().filter(r -> Boolean.TRUE.equals(r.getActive())).count();
        log.info("EOD settlement (8-5) started for business date {} ({} active rule(s); stub — split posting not yet implemented)", businessDate, ruleCount);
        // TODO(backlog): Implement actual settlement posting logic
        //   - Load trips/turnover for date
        //   - Apply share_json splits
        //   - Post to respective accounts
        //   - Record transactions
    }
}

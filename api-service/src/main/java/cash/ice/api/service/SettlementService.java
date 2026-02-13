package cash.ice.api.service;

import cash.ice.sqldb.repository.SettlementRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Phase 8-5: Settlement — split revenue per Sacco/entity according to settlement_rule (driver %, conductor %, etc.).
 * EOD step 1. Stub: logs rule count; actual split posting TODO.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRuleRepository settlementRuleRepository;

    public void runForDate(LocalDate businessDate) {
        long ruleCount = settlementRuleRepository.findAll().stream().filter(r -> Boolean.TRUE.equals(r.getActive())).count();
        log.info("EOD settlement (8-5) started for business date {} ({} active rule(s); stub — split posting not yet implemented)", businessDate, ruleCount);
        // TODO: For each entity with settlement rule, load trips/turnover for date; apply share_json; post to respective accounts
    }
}

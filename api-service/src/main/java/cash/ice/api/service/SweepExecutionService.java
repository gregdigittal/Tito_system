package cash.ice.api.service;

import cash.ice.api.config.property.DeploymentConfigProperties;
import cash.ice.api.dto.moz.MoneyProviderMoz;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.AccountBalance;
import cash.ice.sqldb.entity.SweepRule;
import cash.ice.sqldb.repository.AccountBalanceRepository;
import cash.ice.sqldb.repository.AccountRepository;
import cash.ice.sqldb.repository.SweepRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Phase 8-6: Sweep execution — applies active sweep rules (wallet → mobile money or bank).
 * Triggered by EOD job (8-11) or scheduler. Cash-out integrated via EntityMozService.cashOutToMobileMoneyByAccountId;
 * bank transfer TODO when provider available.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SweepExecutionService {

    private final SweepRuleRepository sweepRuleRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final EntityMozService entityMozService;
    private final DeploymentConfigProperties deploymentConfig;
    private final TopUpServiceSelector topUpServiceSelector;

    /**
     * Run sweep for rules that are due for the given business date (e.g. SCHEDULE with cron match, or THRESHOLD).
     */
    @Transactional(timeout = 30)
    public void runScheduledForDate(LocalDate businessDate) {
        List<SweepRule> allActive = sweepRuleRepository.findAll().stream()
                .filter(SweepRule::getActive)
                .toList();
        if (allActive.isEmpty()) {
            log.debug("EOD sweep: no active sweep rules for date {}", businessDate);
            return;
        }
        for (SweepRule rule : allActive) {
            if ("SCHEDULE".equals(rule.getTriggerType()) || "THRESHOLD".equals(rule.getTriggerType())) {
                try {
                    executeRule(rule, businessDate);
                } catch (Exception e) {
                    log.warn("Sweep rule id={} failed for date {}: {}", rule.getId(), businessDate, e.getMessage());
                }
            }
        }
    }

    private void executeRule(SweepRule rule, LocalDate businessDate) {
        Optional<Account> accountOpt = accountRepository.findById(rule.getAccountId());
        if (accountOpt.isEmpty()) {
            log.warn("Sweep rule id={}: account id={} not found", rule.getId(), rule.getAccountId());
            return;
        }
        BigDecimal balance = accountBalanceRepository.findByAccountId(rule.getAccountId())
                .map(AccountBalance::getBalance)
                .orElse(BigDecimal.ZERO);
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Sweep rule id={}: zero balance, skip", rule.getId());
            return;
        }
        if ("MOBILE_MONEY".equalsIgnoreCase(rule.getDestinationType())) {
            String mobile = StringUtils.hasText(rule.getDestinationRef()) ? rule.getDestinationRef().trim() : null;
            if (mobile == null) {
                log.warn("Sweep rule id={}: MOBILE_MONEY requires destination_ref (mobile number)", rule.getId());
                return;
            }
            BigDecimal amount = rule.getThresholdAmount() != null && rule.getThresholdAmount().compareTo(BigDecimal.ZERO) > 0
                    ? balance.min(rule.getThresholdAmount())
                    : balance;
            MoneyProviderMoz provider = resolveProvider();
            try {
                entityMozService.cashOutToMobileMoneyByAccountId(rule.getAccountId(), provider, mobile, amount);
                // TODO: on success debit account and record transaction
            } catch (Exception e) {
                log.warn("Sweep rule id={} cash-out failed: {}", rule.getId(), e.getMessage());
            }
        } else if ("BANK".equalsIgnoreCase(rule.getDestinationType())) {
            log.info("Sweep rule id={} BANK destination (stub — bank transfer not yet implemented)", rule.getId());
        } else {
            log.debug("Sweep rule id={} destinationType={} unsupported", rule.getId(), rule.getDestinationType());
        }
    }

    private MoneyProviderMoz resolveProvider() {
        String countryCode = StringUtils.hasText(deploymentConfig.getCountryCode()) ? deploymentConfig.getCountryCode().trim() : "KE";
        List<String> allowed = topUpServiceSelector.getAllowedProviderIds(countryCode);
        if (!allowed.isEmpty() && allowed.contains(MoneyProviderMoz.MPESA.name())) {
            return MoneyProviderMoz.MPESA;
        }
        if (!allowed.isEmpty() && allowed.contains(MoneyProviderMoz.EMOLA.name())) {
            return MoneyProviderMoz.EMOLA;
        }
        return MoneyProviderMoz.MPESA;
    }
}

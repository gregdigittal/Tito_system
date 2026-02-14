package cash.ice.api.service;

import cash.ice.api.config.property.DeploymentConfigProperties;
import cash.ice.sqldb.entity.AccountType;
import cash.ice.sqldb.entity.Currency;
import cash.ice.sqldb.entity.TitoFeeRule;
import cash.ice.sqldb.repository.CurrencyRepository;
import cash.ice.sqldb.repository.TitoFeeRuleRepository;
import cash.ice.sqldb.repository.AccountTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

import static cash.ice.sqldb.entity.AccountType.TITO_REVENUE;

/**
 * Phase 8-10: TiTo revenue account; EOD deduction of fees and device rental.
 * Fee rules and TiTo revenue account type wired; actual debit/credit posting not implemented.
 *
 * @deprecated Incomplete implementation. Fee rules are loaded but no ledger posting is performed.
 */
@Deprecated(since = "0.1.1", forRemoval = false)
@Service
@Slf4j
@RequiredArgsConstructor
public class TitoRevenueFeeService {

    private final DeploymentConfigProperties deploymentConfig;
    private final TitoFeeRuleRepository titoFeeRuleRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final CurrencyRepository currencyRepository;

    public void runForDate(LocalDate businessDate) {
        String countryCode = StringUtils.hasText(deploymentConfig.getCountryCode())
                ? deploymentConfig.getCountryCode().trim() : "KE";
        List<TitoFeeRule> rules = titoFeeRuleRepository.findByCountryCodeAndActiveTrue(countryCode);
        if (rules.isEmpty()) {
            log.debug("EOD fee run for {}: no active fee rules for country {}", businessDate, countryCode);
            return;
        }
        Currency defaultCurrency = currencyRepository.findByIsoCode(deploymentConfig.getDefaultCurrencyCode()).orElse(null);
        AccountType titoRevenueType = defaultCurrency != null
                ? accountTypeRepository.findByNameAndCurrencyId(TITO_REVENUE, defaultCurrency.getId()).orElse(null)
                : null;
        if (titoRevenueType == null) {
            log.warn("EOD fee run for {}: TiTo Revenue account type not found for currency {}", businessDate, deploymentConfig.getDefaultCurrencyCode());
            return;
        }
        log.info("EOD fee run for business date {}: {} rule(s), TiTo revenue account type id={} (stub — posting not yet implemented)", businessDate, rules.size(), titoRevenueType.getId());
        // TODO(backlog): Implement actual fee posting logic
        //   - For each rule, resolve source account type
        //   - Iterate source accounts with balance; compute fee (percent or fixed)
        //   - Debit source, credit TiTo revenue account
        //   - Record transactions
    }
}

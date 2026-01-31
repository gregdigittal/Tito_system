package cash.ice.sync.service;

import cash.ice.sqldb.entity.AccountType;
import cash.ice.sqldb.entity.Currency;
import cash.ice.sqldb.repository.AccountTypeRepository;
import cash.ice.sqldb.repository.CurrencyRepository;
import cash.ice.sync.dto.ChangeAction;
import cash.ice.sync.dto.DataChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cash.ice.sync.task.Utils.getVal;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountTypesSyncService implements DataMigrator {
    @SuppressWarnings("SqlResolve")
    private static final String MIGRATE_SQL = "select * from dbo.Wallets";

    private final JdbcTemplate jdbcTemplate;
    private final CurrencyRepository currencyRepository;
    private final AccountTypeRepository accountTypeRepository;

    @Transactional
    @Override
    public void migrateData() {
        log.debug("Start migrating Account Types");
        Map<String, Currency> currencies = currencyRepository.findAll().stream().collect(Collectors.toMap(Currency::getIsoCode, currency -> currency));
        List<AccountType> accountTypes = jdbcTemplate.query(MIGRATE_SQL, (rs, rowNum) -> {
            Currency currency = getVal(currencies, rs.getString("ISO_Currency_Code"));
            return new AccountType()
                    .setCurrencyId(currency == null ? null : currency.getId())
                    .setName(rs.getString("Wallet_Name"))
                    .setDescription(rs.getString("Description"))
                    .setLegacyWalletId(String.valueOf(rs.getInt("Wallet_ID")))
                    .setActive(true);
        });
        accountTypeRepository.saveAll(accountTypes);
        log.info("Finished migrating Account Types: {} processed, {} total", accountTypes.size(), accountTypeRepository.count());
    }

    public void update(DataChange dataChange) {
        AccountType accountType = accountTypeRepository.findByLegacyWalletId(dataChange.getIdentifier()).orElse(null);
        if (dataChange.getAction() == ChangeAction.DELETE) {
            if (accountType != null) {
                accountTypeRepository.delete(accountType);
            } else {
                log.warn("Cannot delete AccountType with legacyWalletId: {}, it is absent", dataChange.getIdentifier());
            }
        } else {                // update
            if (accountType == null) {
                accountType = new AccountType().setLegacyWalletId(dataChange.getIdentifier());
            }
            fillAccountTypeFields(accountType, dataChange.getData());
            accountTypeRepository.save(accountType);
        }
    }

    private void fillAccountTypeFields(AccountType accountType, Map<String, Object> data) {
        data.forEach((column, value) -> {
            switch (column) {
                case "Active" -> accountType.setActive((Boolean) value);
                case "Name" -> accountType.setName((String) value);
                case "Description" -> accountType.setDescription((String) value);
                case "Currency" -> accountType.setCurrencyId(currencyRepository.findByIsoCode((String) value).orElseThrow().getId());
                case "AuditTransactionInterval" -> accountType.setAuditTransactionInterval((Integer) value);
                case "AuditTransactionValue" -> accountType.setAuditTransactionValue(new BigDecimal(String.valueOf(value)));
                default -> log.warn("Unknown accountType field: '{}' has value: '{}'", column, value);
            }
        });
    }
}

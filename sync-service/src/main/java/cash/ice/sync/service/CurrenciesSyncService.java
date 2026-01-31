package cash.ice.sync.service;

import cash.ice.sqldb.entity.Currency;
import cash.ice.sqldb.repository.CurrencyRepository;
import cash.ice.sync.dto.ChangeAction;
import cash.ice.sync.dto.DataChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrenciesSyncService implements DataMigrator {
    @SuppressWarnings("SqlResolve")
    private static final String MIGRATE_SQL = "select * from dbo.Currency";
    private static final String ACTIVE = "Active";

    private final JdbcTemplate jdbcTemplate;
    private final CurrencyRepository currencyRepository;

    @Transactional
    @Override
    public void migrateData() {
        log.debug("Start migrating Currencies");
        List<Currency> currencies = jdbcTemplate.query(MIGRATE_SQL, (rs, rowNum) ->
                new Currency()
                        .setIsoCode(rs.getString("Currency"))
                        .setActive(rs.getBoolean(ACTIVE))
                        .setPostilionCode(rs.getInt("Postilion_Code")));
        currencyRepository.saveAll(currencies);
        log.info("Finished migrating Currencies: {} processed, {} total", currencies.size(), currencyRepository.count());
    }

    public void update(DataChange dataChange) {
        Currency currency = currencyRepository.findByIsoCode(dataChange.getIdentifier()).orElse(null);
        if (dataChange.getAction() == ChangeAction.DELETE) {
            if (currency != null) {
                currencyRepository.delete(currency);
            } else {
                log.warn("Cannot delete Currency with isoCode: {}, it is absent", dataChange.getIdentifier());
            }
        } else {                // update
            if (currency == null) {
                currency = new Currency().setIsoCode(dataChange.getIdentifier());
            }
            if (dataChange.getData().containsKey(ACTIVE)) {
                currency.setActive((Boolean) dataChange.getData().get(ACTIVE));
            }
            if (dataChange.getData().containsKey("PostilionCode")) {
                currency.setPostilionCode((Integer) dataChange.getData().get("PostilionCode"));
            }
            currencyRepository.save(currency);
        }
    }
}

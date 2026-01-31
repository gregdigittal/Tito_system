package cash.ice.sync.service;

import cash.ice.sqldb.entity.Country;
import cash.ice.sqldb.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CountriesSyncService implements DataMigrator {
    @SuppressWarnings("SqlResolve")
    private static final String MIGRATE_SQL = "select * from dbo.Countries";

    private final JdbcTemplate configJdbcTemplate;
    private final CountryRepository countryRepository;

    @Transactional
    @Override
    public void migrateData() {
        log.debug("Start migrating Countries");
        List<Country> countries = configJdbcTemplate.query(MIGRATE_SQL, (rs, rowNum) ->
                new Country()
                        .setIsoCode(rs.getString("Code"))
                        .setName(rs.getString("Name")));
        countryRepository.saveAll(countries);
        log.info("Finished migrating Countries: {} processed, {} total", countries.size(), countryRepository.count());
    }
}

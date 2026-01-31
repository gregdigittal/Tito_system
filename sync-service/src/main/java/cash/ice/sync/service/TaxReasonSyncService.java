package cash.ice.sync.service;

import cash.ice.sqldb.entity.zim.TaxReason;
import cash.ice.sqldb.repository.zim.TaxReasonRepository;
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
public class TaxReasonSyncService implements DataMigrator {
    @SuppressWarnings("SqlResolve")
    private static final String MIGRATE_SQL = "select * from dbo.TAX_Reason";

    private final JdbcTemplate jdbcTemplate;
    private final TaxReasonRepository taxReasonRepository;

    @Transactional
    @Override
    public void migrateData() {
        log.debug("Start migrating TaxReasons");
        List<TaxReason> taxReasons = jdbcTemplate.query(MIGRATE_SQL, (rs, rowNum) ->
                new TaxReason()
                        .setDescription(rs.getString("Description"))
                        .setDisplay(rs.getBoolean("Display")));
        taxReasonRepository.saveAll(taxReasons);
        log.info("Finished migrating TaxReasons: {} processed, {} total", taxReasons.size(), taxReasonRepository.count());
    }

    public void update(DataChange dataChange) {
        TaxReason taxReason = taxReasonRepository.findByDescription(dataChange.getIdentifier()).orElse(null);
        if (dataChange.getAction() == ChangeAction.DELETE) {
            if (taxReason != null) {
                taxReasonRepository.delete(taxReason);
            } else {
                log.warn("Cannot delete TaxReason with description: {}, it is absent", dataChange.getIdentifier());
            }
        } else {                // update
            if (taxReason == null) {
                taxReason = new TaxReason().setDescription(dataChange.getIdentifier());
                if (dataChange.getData().containsKey("Display")) {
                    taxReason.setDisplay((Boolean) dataChange.getData().get("Display"));
                }
                taxReasonRepository.save(taxReason);
            }
        }
    }
}

package cash.ice.sync.service;

import cash.ice.sqldb.entity.zim.TaxDeclaration;
import cash.ice.sqldb.repository.zim.TaxDeclarationRepository;
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
public class TaxDeclarationSyncService implements DataMigrator {
    @SuppressWarnings("SqlResolve")
    private static final String MIGRATE_SQL = "select * from dbo.TAX_Declaration";

    private final JdbcTemplate jdbcTemplate;
    private final TaxDeclarationRepository taxDeclarationRepository;

    @Transactional
    @Override
    public void migrateData() {
        log.debug("Start migrating TaxDeclarations");
        List<TaxDeclaration> taxDeclarations = jdbcTemplate.query(MIGRATE_SQL, (rs, rowNum) ->
                new TaxDeclaration().setDescription(rs.getString("Description")));
        taxDeclarationRepository.saveAll(taxDeclarations);
        log.info("Finished migrating TaxDeclarations: {} processed, {} total", taxDeclarations.size(), taxDeclarationRepository.count());
    }

    public void update(DataChange dataChange) {
        TaxDeclaration taxDeclaration = taxDeclarationRepository.findByDescription(dataChange.getIdentifier()).orElse(null);
        if (dataChange.getAction() == ChangeAction.DELETE) {
            if (taxDeclaration != null) {
                taxDeclarationRepository.delete(taxDeclaration);
            } else {
                log.warn("Cannot delete TaxDeclaration with description: {}, it is absent", dataChange.getIdentifier());
            }
        } else {                // update
            if (taxDeclaration == null) {
                taxDeclaration = new TaxDeclaration().setDescription(dataChange.getIdentifier());
                taxDeclarationRepository.save(taxDeclaration);
            }
        }
    }
}

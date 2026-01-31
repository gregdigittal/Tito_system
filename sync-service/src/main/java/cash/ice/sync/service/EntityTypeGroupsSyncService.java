package cash.ice.sync.service;

import cash.ice.sqldb.entity.EntityTypeGroup;
import cash.ice.sqldb.repository.EntityTypeGroupRepository;
import cash.ice.sync.dto.ChangeAction;
import cash.ice.sync.dto.DataChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EntityTypeGroupsSyncService {
    @SuppressWarnings("SqlResolve")
    private static final String ACCOUNTS_TYPES_GROUPS_SQL = "select * from dbo.Accounts_Types_Groups";

    private final EntityTypeGroupRepository entityTypeGroupRepository;
    private final JdbcTemplate jdbcTemplate;

    public Map<Integer, EntityTypeGroup> migrateEntityTypeGroups() {
        log.debug("Start migrating Entity Type Groups");
        Map<Integer, EntityTypeGroup> map = new HashMap<>();
        List<EntityTypeGroup> entityTypeGroups = jdbcTemplate.query(ACCOUNTS_TYPES_GROUPS_SQL, (rs, rowNum) -> {
            EntityTypeGroup accountTypeGroup = new EntityTypeGroup().setDescription(rs.getString("Account_Type_Group"));
            map.put(rs.getInt("ID"), accountTypeGroup);
            return accountTypeGroup;
        });
        entityTypeGroupRepository.saveAll(entityTypeGroups);
        log.info("Finished migrating Entity Type Groups: {} processed, {} total", entityTypeGroups.size(), entityTypeGroupRepository.count());
        return map;
    }

    public void update(DataChange dataChange) {
        EntityTypeGroup entityTypeGroup = entityTypeGroupRepository.findByDescription(dataChange.getIdentifier()).orElse(null);
        if (dataChange.getAction() == ChangeAction.DELETE) {
            if (entityTypeGroup != null) {
                entityTypeGroupRepository.delete(entityTypeGroup);
            } else {
                log.warn("Cannot delete Entity Type Group with description: {}, it is absent", dataChange.getIdentifier());
            }
        } else {                // update
            if (entityTypeGroup == null) {
                entityTypeGroup = new EntityTypeGroup().setDescription(dataChange.getIdentifier());
                entityTypeGroupRepository.save(entityTypeGroup);
            }
        }
    }
}

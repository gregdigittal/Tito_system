package cash.ice.sync.service;

import cash.ice.sqldb.entity.EntityType;
import cash.ice.sqldb.entity.EntityTypeGroup;
import cash.ice.sqldb.repository.EntityTypeGroupRepository;
import cash.ice.sqldb.repository.EntityTypeRepository;
import cash.ice.sync.dto.ChangeAction;
import cash.ice.sync.dto.DataChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cash.ice.sync.task.Utils.getVal;

@Service
@Slf4j
@RequiredArgsConstructor
public class EntityTypesSyncService {
    @SuppressWarnings("SqlResolve")
    private static final String ACCOUNTS_TYPES_SQL = "select * from dbo.Accounts_Types";

    private final EntityTypeRepository entityTypeRepository;
    private final EntityTypeGroupRepository entityTypeGroupRepository;
    private final JdbcTemplate jdbcTemplate;

    public Map<String, EntityType> migrateEntityTypes(Map<Integer, EntityTypeGroup> entityTypeGroups) {
        log.debug("Start migrating Entity Types");
        Map<String, EntityType> map = new HashMap<>();
        List<EntityType> entityTypes = jdbcTemplate.query(ACCOUNTS_TYPES_SQL, (rs, rowNum) -> {
            EntityTypeGroup typeGroup = getVal(entityTypeGroups, rs.getInt("Account_Type_Group_ID"));
            EntityType entityType = new EntityType()
                    .setDescription(rs.getString("Account_Type"))
                    .setEntityTypeGroupId(typeGroup == null ? null : typeGroup.getId());
            map.put(rs.getString("Account_Type"), entityType);
            return entityType;
        });
        entityTypeRepository.saveAll(entityTypes);
        log.info("Finished migrating Entity Types: {} processed, {} total", entityTypes.size(), entityTypeRepository.count());
        return map;
    }

    public void update(DataChange dataChange) {
        EntityType entityType = entityTypeRepository.findByDescription(dataChange.getIdentifier()).orElse(null);
        if (dataChange.getAction() == ChangeAction.DELETE) {
            if (entityType != null) {
                entityTypeRepository.delete(entityType);
            } else {
                log.warn("Cannot delete Entity Type with description: {}, it is absent", dataChange.getIdentifier());
            }
        } else {                // update
            if (entityType == null) {
                EntityTypeGroup entityTypeGroup = entityTypeGroupRepository.findByDescription((String) dataChange.getData().get("Group")).orElseThrow();
                entityType = new EntityType()
                        .setDescription(dataChange.getIdentifier())
                        .setEntityTypeGroupId(entityTypeGroup.getId());
                entityTypeRepository.save(entityType);
            }
        }
    }
}

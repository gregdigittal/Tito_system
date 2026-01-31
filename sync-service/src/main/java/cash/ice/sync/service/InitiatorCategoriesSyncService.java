package cash.ice.sync.service;

import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.InitiatorCategory;
import cash.ice.sqldb.repository.EntityRepository;
import cash.ice.sqldb.repository.InitiatorCategoryRepository;
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
public class InitiatorCategoriesSyncService {
    @SuppressWarnings("SqlResolve")
    private static final String CARD_CATEGORY_SQL = "select c.Card_Category_ID, c.Category, o.Account_ID from dbo.Card_Category c " +
            "left join dbo.Organisations o on c.Organisation = o.Organisation_Name";
    private static final String ACCOUNT_ID = "Account_ID";

    private final EntityRepository entityRepository;
    private final InitiatorCategoryRepository initiatorCategoryRepository;
    private final JdbcTemplate jdbcTemplate;

    public Map<Integer, InitiatorCategory> migrateInitiatorCategories() {
        log.debug("Start migrating Initiator Categories");
        Map<Integer, InitiatorCategory> map = new HashMap<>();
        List<InitiatorCategory> initiatorCategories = jdbcTemplate.query(CARD_CATEGORY_SQL, (rs, rowNum) -> {
            EntityClass entity = entityRepository.findByLegacyAccountId(rs.getInt(ACCOUNT_ID)).orElse(null);
            InitiatorCategory category = new InitiatorCategory()
                    .setCategory(rs.getString("Category"))
                    .setEntityId(entity == null ? null : entity.getId());
            map.put(rs.getInt("Card_Category_ID"), category);
            return category;
        });
        initiatorCategoryRepository.saveAll(initiatorCategories);
        log.info("Finished migrating Initiator Categories: {} processed, {} total", map.size(), initiatorCategoryRepository.count());
        return map;
    }

    public void update(DataChange dataChange) {
        InitiatorCategory initiatorCategory = initiatorCategoryRepository.findByCategory(dataChange.getIdentifier()).orElse(null);
        if (dataChange.getAction() == ChangeAction.DELETE) {
            if (initiatorCategory != null) {
                initiatorCategoryRepository.delete(initiatorCategory);
            } else {
                log.warn("Cannot delete Initiator Category with description: {}, it is absent", dataChange.getIdentifier());
            }
        } else {                // update
            if (initiatorCategory == null) {
                initiatorCategory = new InitiatorCategory().setCategory(dataChange.getIdentifier());
            }
            if (dataChange.getData().containsKey("LegacyAccountID")) {
                EntityClass entity = entityRepository.findByLegacyAccountId((Integer) dataChange.getData().get("LegacyAccountID")).orElseThrow();
                initiatorCategory.setEntityId(entity.getId());
            }
            initiatorCategoryRepository.save(initiatorCategory);
        }
    }
}

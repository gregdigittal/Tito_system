package cash.ice.sync.service;

import cash.ice.sqldb.entity.InitiatorStatus;
import cash.ice.sqldb.repository.InitiatorStatusRepository;
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
public class InitiatorStatusesSyncService {
    @SuppressWarnings("SqlResolve")
    private static final String CARDS_STATUS_SQL = "select * from dbo.Cards_Status";

    private final InitiatorStatusRepository initiatorStatusRepository;
    private final JdbcTemplate jdbcTemplate;

    public Map<Integer, InitiatorStatus> migrateInitiatorStatuses() {
        log.debug("Start migrating Initiator Statuses");
        Map<Integer, InitiatorStatus> map = new HashMap<>();
        List<InitiatorStatus> initiatorStatuses = jdbcTemplate.query(CARDS_STATUS_SQL, (rs, rowNum) -> {
            InitiatorStatus initiatorStatus = new InitiatorStatus()
                    .setName(rs.getString("Status_Name"))
                    .setPermitTransaction(false)
                    .setActive(true);
            map.put(rs.getInt("Status_ID"), initiatorStatus);
            return initiatorStatus;
        });
        initiatorStatusRepository.saveAll(initiatorStatuses);
        log.info("Finished migrating Initiator Statuses: {} processed, {} total", map.size(), initiatorStatusRepository.count());
        return map;
    }

    public void update(DataChange dataChange) {
        InitiatorStatus initiatorStatus = initiatorStatusRepository.findByName(dataChange.getIdentifier()).orElse(null);
        if (dataChange.getAction() == ChangeAction.DELETE) {
            if (initiatorStatus != null) {
                initiatorStatusRepository.delete(initiatorStatus);
            } else {
                log.warn("Cannot delete Initiator Category with description: {}, it is absent", dataChange.getIdentifier());
            }
        } else {                // update
            if (initiatorStatus == null) {
                initiatorStatus = new InitiatorStatus().setName(dataChange.getIdentifier());
            }
            if (dataChange.getData().containsKey("Active")) {
                initiatorStatus.setActive((Boolean) dataChange.getData().get("Active"));
            }
            if (dataChange.getData().containsKey("PermitTransaction")) {
                initiatorStatus.setPermitTransaction((Boolean) dataChange.getData().get("PermitTransaction"));
            }
            initiatorStatusRepository.save(initiatorStatus);
        }
    }
}

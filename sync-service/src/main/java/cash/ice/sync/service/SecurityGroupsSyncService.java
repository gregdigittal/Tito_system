package cash.ice.sync.service;

import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.SecurityGroup;
import cash.ice.sqldb.entity.SecurityRight;
import cash.ice.sqldb.repository.SecurityGroupRepository;
import cash.ice.sqldb.repository.SecurityRightRepository;
import cash.ice.sync.dto.ChangeAction;
import cash.ice.sync.dto.SecurityGroupChange;
import cash.ice.sync.dto.SecurityRightChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityGroupsSyncService implements DataMigrator {
    @SuppressWarnings("SqlResolve")
    private static final String ONLINE_RIGHTS_SQL = "select * from dbo.Online_Rights";
    @SuppressWarnings("SqlResolve")
    private static final String ONLINE_GROUPS_SQL = "select * from dbo.Online_Groups";
    @SuppressWarnings("SqlResolve")
    private static final String ONLINE_GROUP_RIGHTS_SQL = "select * from dbo.Online_Group_Rights";
    @SuppressWarnings("SqlResolve")
    private static final String MOBI_RIGHTS_SQL = "select * from dbo.Mobi_Security_Rights";
    @SuppressWarnings("SqlResolve")
    private static final String MOBI_GROUPS_SQL = "select * from dbo.Mobi_Security_Groups";
    @SuppressWarnings("SqlResolve")
    private static final String MOBI_GROUP_RIGHTS_SQL = "select * from dbo.Mobi_Security_Group_Rights";

    private final JdbcTemplate jdbcTemplate;
    private final SecurityRightRepository securityRightRepository;
    private final SecurityGroupRepository securityGroupRepository;

    @Transactional
    @Override
    public void migrateData() {
        migrateOnlineGroups();
        migrateMobiGroups();
    }

    private void migrateOnlineGroups() {
        log.debug("Start migrating Online Rights");
        Map<Integer, SecurityRight> onlineRights = migrateRights(ONLINE_RIGHTS_SQL, "Online_Right_ID", false);
        Map<Integer, SecurityGroup> onlineGroupMap = new HashMap<>();
        jdbcTemplate.query(ONLINE_GROUPS_SQL, rs -> {
            onlineGroupMap.put(rs.getInt("Online_Group_ID"), new SecurityGroup()
                    .setLegacyId("online:" + rs.getInt("Online_Group_ID"))
                    .setName(rs.getString("Group_Name"))
                    .setDescription(rs.getString("Group_Description"))
                    .setActive(rs.getBoolean("Active"))
                    .setMeta(new Tool.MapBuilder<String, Object>(new HashMap<>())
                            .put("OnlineProfileAccess", rs.getBoolean("Online_Profile_Access"))
                            .putIfNonNull("OrderBy", rs.getString("Orderby")).build()));
        });
        jdbcTemplate.query(ONLINE_GROUP_RIGHTS_SQL, rs -> {
            SecurityGroup securityGroup = onlineGroupMap.get(rs.getInt("Online_Group_ID"));
            SecurityRight securityRight = onlineRights.get(rs.getInt("Online_Right_ID"));
            if (securityGroup == null) {
                log.warn("  Security online group %s is absent".formatted(rs.getInt("Online_Group_ID")));
            } else if (securityRight == null) {
                log.warn("  Security right %s is absent".formatted(rs.getInt("Online_Right_ID")));
            } else {
                securityGroup.getRights().add(securityRight);
            }
        });
        securityGroupRepository.saveAll(onlineGroupMap.values());
        log.info("Finished migrating Online Rights");
    }

    private void migrateMobiGroups() {
        log.debug("Start migrating Mobi Rights");
        Map<Integer, SecurityRight> mobiRights = migrateRights(MOBI_RIGHTS_SQL, "Mobi_Right_ID", true);
        Map<Integer, SecurityGroup> mobiGroupMap = new HashMap<>();
        jdbcTemplate.query(MOBI_GROUPS_SQL, rs -> {
            mobiGroupMap.put(rs.getInt("Security_Group_ID"), new SecurityGroup()
                    .setLegacyId("mobi:" + rs.getInt("Security_Group_ID"))
                    .setName(rs.getString("Group_Name"))
                    .setDescription(rs.getString("Group_Description"))
                    .setActive(rs.getBoolean("Active"))
                    .setMeta(new Tool.MapBuilder<String, Object>(new HashMap<>())
                            .putIfNonNull("OverdraftLimit", rs.getBigDecimal("Overdraft_Limit"))
                            .putIfNonNull("OrganisationId", rs.getObject("Organisation_ID", Integer.class))
                            .putIfNonNull("GroupLevel", rs.getObject("Group_Level", Integer.class))
                            .putIfNonNull("TransactionCodeZesa", rs.getString("Transaction_Code_ZESA"))
                            .putIfNonNull("TransactionCodeAir", rs.getString("Transaction_Code_AIR"))
                            .putIfNonNull("TransactionCodeInsurance", rs.getString("Transaction_Code_Insurance"))
                            .putIfNonNull("ParentAccountId", rs.getObject("Parent_Account_ID", Integer.class)).build()));
        });
        jdbcTemplate.query(MOBI_GROUP_RIGHTS_SQL, rs -> {
            SecurityGroup securityGroup = mobiGroupMap.get(rs.getInt("Security_Group_ID"));
            SecurityRight securityRight = mobiRights.get(rs.getInt("Security_Right_ID"));
            if (securityGroup == null) {
                log.warn("  Security mobi group %s is absent".formatted(rs.getInt("Security_Group_ID")));
            } else if (securityRight == null) {
                log.warn("  Security right %s is absent".formatted(rs.getInt("Security_Right_ID")));
            } else {
                securityGroup.getRights().add(securityRight);
            }
        });
        securityGroupRepository.saveAll(mobiGroupMap.values());
        log.info("Finished migrating Mobi Rights");
    }

    private Map<Integer, SecurityRight> migrateRights(String sql, String idColumn, boolean queryRightType) {
        Map<Integer, SecurityRight> securityRightMap = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            SecurityRight securityRight = securityRightRepository.findByName(rs.getString("Right_Name")).orElse(null);
            if (securityRight == null) {
                securityRight = securityRightRepository.save(new SecurityRight()
                        .setName(rs.getString("Right_Name"))
                        .setDescription(rs.getString("Right_Description"))
                        .setRightType(queryRightType ? rs.getString("Right_Type") : null));
            }
            securityRightMap.put(rs.getInt(idColumn), securityRight);
        });
        return securityRightMap;
    }

    private void removeSecurityRight(SecurityGroup securityGroup, String right) {
        securityGroup.getRights().removeIf(securityRight -> securityRight.getName().equals(right));
    }

    @Transactional
    public void updateRight(SecurityRightChange dataChange) {
        SecurityRight securityRight = securityRightRepository.findByName(dataChange.getRight()).orElse(null);
        if (dataChange.getAction() == ChangeAction.DELETE) {
            if (securityRight != null) {
                List<SecurityGroup> groups = securityGroupRepository.findByRightsName(dataChange.getRight());
                groups.forEach(securityGroup -> removeSecurityRight(securityGroup, dataChange.getRight()));
                securityGroupRepository.saveAll(groups);
                securityRightRepository.delete(securityRight);
            } else {
                log.warn("Cannot delete SecurityRight: {}, it is absent", dataChange.getRight());
            }
        } else {                // update
            if (securityRight == null) {
                securityRight = new SecurityRight().setName(dataChange.getRight());
            }
            if (dataChange.getData().containsKey("Description")) {
                securityRight.setDescription((String) dataChange.getData().get("Description"));
            }
            if (dataChange.getData().containsKey("RightType")) {
                securityRight.setRightType((String) dataChange.getData().get("RightType"));
            }
            securityRightRepository.save(securityRight);
        }
    }

    @Transactional
    public void updateGroup(SecurityGroupChange dataChange) {
        String legacyId = "%s:%s".formatted(dataChange.getType(), dataChange.getIdentifier());
        SecurityGroup securityGroup = securityGroupRepository.findByLegacyId(legacyId).orElse(null);
        if (dataChange.getAction() == ChangeAction.DELETE) {
            if (securityGroup != null) {
                securityGroupRepository.delete(securityGroup);
            } else {
                log.warn("Cannot delete SecurityGroup with legacyId: {}, it is absent", legacyId);
            }
        } else {                // update
            if (securityGroup == null) {
                securityGroup = new SecurityGroup().setLegacyId(legacyId);
            }
            fillSecurityGroupFields(securityGroup, dataChange.getData());
            securityGroupRepository.save(securityGroup);
        }
    }

    @SuppressWarnings("unchecked")
    private void fillSecurityGroupFields(SecurityGroup securityGroup, Map<String, Object> data) {
        data.forEach((column, value) -> {
            switch (column) {
                case "Name" -> securityGroup.setName((String) value);
                case "Description" -> securityGroup.setDescription((String) value);
                case "Active" -> securityGroup.setActive((Boolean) value);
                case "Rights" -> ((Map<String, String>) value).forEach((right, action) -> {
                    switch (action) {
                        case "ADD" -> {
                            SecurityRight securityRight = securityRightRepository.findByName(right).orElse(null);
                            if (securityRight == null) {
                                securityRight = securityRightRepository.save(new SecurityRight().setName(right));
                            }
                            securityGroup.getRights().add(securityRight);
                        }
                        case "DELETE" -> removeSecurityRight(securityGroup, right);
                        default -> log.warn("Unknown action: {} for right: {}", action, right);
                    }
                });
                default -> securityGroup.updateMetaDataValue(column, value);
            }
        });
    }
}

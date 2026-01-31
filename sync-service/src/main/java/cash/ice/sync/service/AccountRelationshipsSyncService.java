package cash.ice.sync.service;

import cash.ice.common.performance.PerfStopwatch;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.converter.JsonToMapConverter;
import cash.ice.sqldb.entity.Currency;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import cash.ice.sync.dto.ChangeAction;
import cash.ice.sync.dto.RelationshipChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static cash.ice.sqldb.entity.AccountType.PRIMARY_ACCOUNT;
import static java.util.stream.Collectors.toMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountRelationshipsSyncService implements DataMigrator {
    private static final String OTHER_INFO = "Other_Info";
    private static final String CREATED_DATE = "Created_Date";
    @SuppressWarnings("SqlResolve")
    private static final String ACCOUNTS_ORGANISATION_SQL = """
            Select  Account_ID user_account_id
                 ,Partner_ID partner_account_id
                 ,Created_Date, security_group_mobi, security_group_online
                 ,json_object.account_relationship_meta_data
            From dbo.Accounts_Organisation ar
                     cross apply (
                Select Other_Info1,Other_Info2,Other_Info3,Other_Info4,Other_Info5,Other_Info6,Other_Info7,Other_Info8,Other_Info9,Other_Info10
                     ,Other_Info11,Other_Info12,Other_Info13,Other_Info14,Other_Info15,Other_Info16,Other_Info17,Other_Info18,Other_Info19,Other_Info20
                From dbo.Accounts_Organisation acrmd
                Where acrmd.Account_ID=ar.Account_ID and acrmd.Partner_ID=ar.Partner_ID
                for json auto ) json_object (account_relationship_meta_data)
            Where patindex('%Other%',json_object.account_relationship_meta_data) <> 0 --exclude blank values
            """;

    private final JdbcTemplate jdbcTemplate;
    private final MetaDataRepository metadataRepository;
    private final AccountRelationshipRepository accountRelationshipRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final CurrencyRepository currencyRepository;
    private final EntityRepository entityRepository;
    private final AccountRepository accountRepository;
    private final SecurityGroupRepository securityGroupRepository;

    @Transactional
    @Override
    public void migrateData() {
        log.debug("Start migrating Accounts Relationship");
        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 1; i <= 20; i++) {
            metadataRepository.save(new MetaData().setTable("account_relationship").setName(OTHER_INFO + i));
        }
        AccountType primaryAccountType = getPrimaryAccountType();
        JsonToMapConverter jsonToMapConverter = new JsonToMapConverter();
        PerfStopwatch rowWatch = new PerfStopwatch();
        PerfStopwatch writeWatch = new PerfStopwatch();
        PerfStopwatch accountWatch = new PerfStopwatch();
        PerfStopwatch existsWatch = new PerfStopwatch();

        log.debug("Prepare accounts");
        Set<String> handledRelations = new HashSet<>();
        Map<Integer, Integer> legacyToEntityId = entityRepository.findAll().stream().collect(toMap(
                EntityClass::getLegacyAccountId, EntityClass::getId, (key1, key2) -> key1));
        Map<Integer, Integer> entityIdToPrimaryAccountId = accountRepository.findAll().stream()
                .filter(account -> primaryAccountType.getId().equals(account.getAccountTypeId()))
                .collect(toMap(Account::getEntityId, Account::getId));
        Map<String, Integer> securityGroupIdsMap = securityGroupRepository.findAll().stream()
                .collect(toMap(SecurityGroup::getLegacyId, SecurityGroup::getId, (key1, key2) -> key1));

        log.debug("Migrating data");
        jdbcTemplate.query(ACCOUNTS_ORGANISATION_SQL, rs -> {
            rowWatch.start();
            Integer entityId = legacyToEntityId.get(rs.getInt("user_account_id"));
            Integer partnerEntityId = legacyToEntityId.get(rs.getInt("partner_account_id"));
            accountWatch.start();
            Integer partnerId = entityIdToPrimaryAccountId.get(partnerEntityId);
            accountWatch.stop();
            if (entityId == null) {
                log.warn("  Skipping relationship for unknown Account_ID: {}", rs.getInt("user_account_id"));
            } else if (partnerId == null) {
                log.warn("  Skipping relationship for unknown Partner_ID: {}", rs.getInt("partner_account_id"));
            } else {
                existsWatch.start();
                boolean exists = handledRelations.contains(entityId + "," + partnerId);
                handledRelations.add(entityId + "," + partnerId);
                existsWatch.stop();
                if (!exists) {
                    writeWatch.start();
                    accountRelationshipRepository.save(new AccountRelationship()
                            .setEntityId(entityId)
                            .setPartnerAccountId(partnerId)
                            .setSecurityGroupsMap(toSecurityGroups(securityGroupIdsMap,
                                    rs.getObject("security_group_mobi", Integer.class),
                                    rs.getObject("security_group_online", Integer.class)))
                            .setMetaDataMap(getAccountsRelationshipMetaData(rs, jsonToMapConverter))
                            .setCreatedDate(rs.getTimestamp(CREATED_DATE) == null ? null : rs.getTimestamp(CREATED_DATE).toLocalDateTime()));
                    writeWatch.stop();
                } else {
                    log.warn("  Duplicated Account-Partner relationship! Account_ID: {}, Partner_ID: {}",
                            rs.getInt("user_account_id"), rs.getInt("partner_account_id"));
                }
                rowWatch.stop();
                counter.incrementAndGet();
                if (counter.incrementAndGet() % 5000 == 0) {
                    log.debug("  {} relationships processed", counter.get());
                    logAndStop("      relationship account: {}", accountWatch);
                    logAndStop("      relationship exists: {}", existsWatch);
                    logAndStop("      relationship write: {}", writeWatch);
                    logAndStop("      relationship row:   {}", rowWatch);
                }
            }
        });
        log.info("Finished migrating Accounts Relationship: {} processed, {} total", counter.get(), accountRelationshipRepository.count());
    }

    private Map<String, Object> toSecurityGroups(Map<String, Integer> securityGroupIdsMap, Integer securityGroupMobi, Integer securityGroupOnline) {
        return securityGroupMobi == null && securityGroupOnline == null ? null :
                new Tool.MapBuilder<String, Object>(new HashMap<>())
                        .putIfNonNull("mobi", securityGroupIdsMap.get("mobi:" + securityGroupMobi))
                        .putIfNonNull("online", securityGroupIdsMap.get("online:" + securityGroupOnline)).build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getAccountsRelationshipMetaData(ResultSet rs, JsonToMapConverter jsonToMapConverter) throws SQLException {
        String metaStr = rs.getString("account_relationship_meta_data");
        List<Map<String, Object>> list = (List<Map<String, Object>>) jsonToMapConverter.convertToEntityAttribute(metaStr);
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }

    private void logAndStop(String message, PerfStopwatch stopwatch) {
        if (stopwatch.getCount() > 0) {
            log.debug(message, stopwatch.finishStopwatch());
        }
        stopwatch.clear();
    }

    private Integer getEntityIdFor(int legacyAccountId) {
        EntityClass entity = entityRepository.findByLegacyAccountId(legacyAccountId).orElse(null);
        return entity != null ? entity.getId() : null;
    }

    private Integer getAccountIdFor(int legacyAccountId, AccountType accountType) {
        EntityClass entity1 = entityRepository.findByLegacyAccountId(legacyAccountId).orElse(null);
        if (entity1 != null) {
            Account account1 = accountRepository.findByEntityIdAndAccountTypeId(entity1.getId(), accountType.getId()).orElseThrow();
            return account1.getId();
        } else {
            return null;
        }
    }

    private AccountType getPrimaryAccountType() {
        Currency zwlCurrency = currencyRepository.findByIsoCode("ZWL").orElseThrow();
        return accountTypeRepository.findByNameAndCurrencyId(PRIMARY_ACCOUNT, zwlCurrency.getId()).orElseThrow();
    }

    public void update(RelationshipChange relationshipChange) {
        Integer entityId = getEntityIdFor(relationshipChange.getLegacyAccountId());
        Integer partnerId = getAccountIdFor(relationshipChange.getLegacyPartnerId(), getPrimaryAccountType());
        AccountRelationship relationship = accountRelationshipRepository.findByEntityIdAndPartnerAccountId(entityId, partnerId).orElse(null);
        if (relationshipChange.getAction() == ChangeAction.DELETE) {
            if (relationship != null) {
                accountRelationshipRepository.delete(relationship);
            } else {
                log.warn("Cannot delete AccountRelationship with AccountId: {}, PartnerId: {}, it is absent",
                        relationshipChange.getLegacyAccountId(), relationshipChange.getLegacyPartnerId());
            }
        } else {                // update
            if (relationship == null) {
                relationship = new AccountRelationship().setEntityId(entityId).setPartnerAccountId(partnerId);
            }
            fillAccountFields(relationship, relationshipChange.getData());
            accountRelationshipRepository.save(relationship);
        }
    }

    private void fillAccountFields(AccountRelationship relationship, Map<String, Object> data) {
        data.forEach((key, val) -> {
            if ("SecurityGroupOnline".equals(key) || "SecurityGroupMobi".equals(key)) {
                String realm = "SecurityGroupOnline".equals(key) ? "online" : "mobi";
                if (val != null) {
                    String legacyId = "%s:%s".formatted(realm, val);
                    SecurityGroup securityGroup = securityGroupRepository.findByLegacyId(legacyId).orElse(null);
                    if (securityGroup != null) {
                        relationship.updateSecurityGroupFor(realm, securityGroup.getId());
                    } else {
                        log.warn("Cannot update accountRelationship, security group {} does not exist", legacyId);
                    }
                } else {
                    relationship.updateSecurityGroupFor(realm, null);
                }
            } else if (key.startsWith(OTHER_INFO)) {
                if (relationship.getMetaData() == null) {
                    relationship.setMetaDataMap(new HashMap<>());
                }
                relationship.getMetaData().put(key, val);
            }
        });
    }
}

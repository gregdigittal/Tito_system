package cash.ice.sync.service;

import cash.ice.common.performance.PerfStopwatch;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import cash.ice.sync.component.DateTimeParser;
import cash.ice.sync.dto.ChangeAction;
import cash.ice.sync.dto.DataChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static cash.ice.sync.task.Utils.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class EntitiesSyncService implements DataMigrator {
    @SuppressWarnings("SqlResolve")
    private static final String ACCOUNTS_SQL = "select * from dbo.Accounts";
    private static final Map<String, String> LEGACY_ID_MAPPING = Map.of("Zimbabwean National ID", "Zim ID",
            "South African National ID", "SA ID", "Business Registration Number", "Co Reg Docs");
    private static final String CREATED_DATE = "Created_Date";
    private static final String MSISDN = "MSISDN";
    private static final String TOWN = "Town";
    private static final String POST_CODE = "Post_Code";
    private static final String ADDRESS_1 = "Address1";
    private static final String ADDRESS_2 = "Address2";
    private static final String ALT_CONTACT_NAME = "Alt_Contact_Name";
    private static final String ALT_CONTACT_MOBILE = "Alt_Contact_Mobile";

    private final EntityTypeGroupsSyncService entityTypeGroupsSyncService;
    private final EntityTypesSyncService entityTypesSyncService;
    private final DateTimeParser dateTimeParser;
    private final CountryRepository countryRepository;
    private final EntityIdTypeRepository entityIdTypeRepository;
    private final EntityRepository entityRepository;
    private final AddressRepository addressRepository;
    private final EntityMsisdnRepository entityMsisdnRepository;
    private final EntityTypeRepository entityTypeRepository;
    private final JdbcTemplate jdbcTemplate;

    private final Map<String, EntityIdType> entityIdTypeMap = new HashMap<>();
    private Map<String, Country> citizenshipMap;

    @Transactional
    @Override
    public void migrateData() {
        Map<Integer, EntityTypeGroup> entityTypeGroups = entityTypeGroupsSyncService.migrateEntityTypeGroups();
        Map<String, EntityType> entityTypes = entityTypesSyncService.migrateEntityTypes(entityTypeGroups);

        log.debug("Start migrating Entities");
        Country zimCountry = countryRepository.findByIsoCode("ZIM").orElseThrow();
        Map<String, EntityIdType> entityIdTypes = entityIdTypeRepository.findAll().stream().collect(Collectors.toMap(t -> LEGACY_ID_MAPPING.get(t.getDescription()), t -> t));
        AtomicInteger counter = new AtomicInteger(0);

        PerfStopwatch rowWatch = new PerfStopwatch();
        PerfStopwatch accWriteWatch = new PerfStopwatch();
        PerfStopwatch addressWriteWatch = new PerfStopwatch();

        jdbcTemplate.query(ACCOUNTS_SQL, rs -> {
            rowWatch.start();
            EntityType entityType = getValNotNull(entityTypes, rs.getString("Type"), "dbo.Accounts.Type");
            EntityIdType idType = getVal(entityIdTypes, rs.getString("ID_Type"));
            EntityClass entity = new EntityClass()
                    .setLegacyAccountId(getInt(rs, "Account_ID"))
                    .setInternalId(getString(rs, "ID_Number_Converted"))
                    .setPvv(getString(rs, "PVV"))
                    .setIdNumber(getString(rs, "ID_Number"))
                    .setIdType(idType == null ? null : idType.getId())
                    .setEntityTypeId(entityType.getId())
                    .setCreatedDate(rs.getTimestamp(CREATED_DATE).toLocalDateTime())
                    .setStatus(EntityStatus.of(rs.getBoolean("Active")))
                    .setFirstName(getString(rs, "First_Name"))
                    .setLastName(getString(rs, "Last_Name"))
                    .setCitizenshipCountryId(getCitizenshipCountryId(rs.getString("Citizenship")))
                    .setBirthDate(dateTimeParser.parseBirthDate(rs.getString("Birth_Date")))
                    .setGender(Gender.of(rs.getString("Gender")))
                    .setEmail(getString(rs, "Email"))
                    .setKycStatusId(rs.getInt("KYC"));
            saveEntity(entity, accWriteWatch);

            Address address = new Address()
                    .setAddressType(AddressType.PRIMARY)
                    .setEntityId(entity.getId())
                    .setCountryId(zimCountry.getId())
                    .setCity(getString(rs, TOWN))
                    .setPostalCode(getString(rs, POST_CODE))
                    .setAddress1(getString(rs, ADDRESS_1))
                    .setAddress2(getString(rs, ADDRESS_2));
            saveAddress(address, addressWriteWatch);

            if (!ObjectUtils.isEmpty(rs.getString(MSISDN))) {
                EntityMsisdn entityMsisdn = new EntityMsisdn()
                        .setEntityId(entity.getId())
                        .setMsisdnType(MsisdnType.PRIMARY)
                        .setCreatedDate(rs.getTimestamp(CREATED_DATE).toLocalDateTime())
                        .setMsisdn(rs.getString(MSISDN))
                        .setDescription(getString(rs, MSISDN));
                entityMsisdnRepository.save(entityMsisdn);
            }
            if (!ObjectUtils.isEmpty(rs.getString(ALT_CONTACT_MOBILE))) {
                EntityMsisdn entityMsisdn2 = new EntityMsisdn()
                        .setEntityId(entity.getId())
                        .setMsisdnType(MsisdnType.SECONDARY)
                        .setCreatedDate(rs.getTimestamp(CREATED_DATE).toLocalDateTime())
                        .setMsisdn(rs.getString(ALT_CONTACT_MOBILE))
                        .setDescription(getString(rs, ALT_CONTACT_NAME));
                entityMsisdnRepository.save(entityMsisdn2);
            }
            rowWatch.stop();
            if (counter.incrementAndGet() % 20000 == 0) {
                log.debug("  {} entities processed", counter.get());
                logAndStop("      entityWrite:  {}", accWriteWatch);
                logAndStop("      addressWrite: {}", accWriteWatch);
                logAndStop("      entity row:   {}", rowWatch);
            }
        });
        log.info("Finished migrating Entities: {} processed, Total: {} entities, {} addresses, {} msisdns", counter.get(),
                entityRepository.count(), addressRepository.count(), entityMsisdnRepository.count());
    }

    private Integer getCitizenshipCountryId(String citizenshipStr) {
        if (citizenshipMap == null) {
            Country zimCountry = countryRepository.findByIsoCode("ZIM").orElseThrow();
            Country saCountry = countryRepository.findByIsoCode("RSA").orElseThrow();
            citizenshipMap = Map.of("RSA", saCountry, "South African", saCountry, "Zimbabwean", zimCountry);
        }
        Country country = getVal(citizenshipMap, citizenshipStr);
        return country == null ? null : country.getId();
    }

    private void saveEntity(EntityClass entity, PerfStopwatch accWriteWatch) {
        accWriteWatch.start();
        entityRepository.save(entity);
        accWriteWatch.stop();
    }

    private void saveAddress(Address address, PerfStopwatch addressWriteWatch) {
        addressWriteWatch.start();
        addressRepository.save(address);
        addressWriteWatch.stop();
    }

    private void logAndStop(String message, PerfStopwatch stopwatch) {
        if (stopwatch.getCount() > 0) {
            log.debug(message, stopwatch.finishStopwatch());
        }
        stopwatch.clear();
    }

    public EntityClass update(DataChange dataChange) {
        EntityClass entity = entityRepository.findByLegacyAccountId(Integer.parseInt(dataChange.getIdentifier())).orElse(null);
        if (dataChange.getAction() == ChangeAction.DELETE) {
            if (entity != null) {
                entityRepository.delete(entity);
            } else {
                log.warn("Cannot delete Entity with legacyAccountId: {}, it is absent", dataChange.getIdentifier());
            }
        } else {                // update
            if (entity == null) {
                entity = new EntityClass().setLegacyAccountId(Integer.parseInt(dataChange.getIdentifier()));
            }
            fillEntityFields(entity, dataChange.getData());
            entity = entityRepository.save(entity);
            updateAddressIfNeed(dataChange.getData(), entity.getId());
            updatePhonesIfNeed(dataChange.getData(), entity);
        }
        return entity;
    }

    private void fillEntityFields(EntityClass entity, Map<String, Object> data) {
        data.forEach((column, value) -> {
            switch (column) {
                case "ID_Number_Converted" -> entity.setInternalId((String) value);
                case "PVV" -> entity.setPvv((String) value);
                case "ID_Number" -> entity.setIdNumber((String) value);
                case "ID_Type" -> {
                    EntityIdType idType = getEntityIdType((String) value);
                    entity.setIdType(idType == null ? null : idType.getId());
                }
                case "Type" -> {
                    EntityType entityType = entityTypeRepository.findByDescription((String) value).orElseThrow();
                    entity.setEntityTypeId(entityType.getId());
                }
                case CREATED_DATE -> entity.setCreatedDate(dateTimeParser.parseDateTime((String) value));
                case "Active" -> entity.setStatus(EntityStatus.of((Boolean) value));
                case "First_Name" -> entity.setFirstName((String) value);
                case "Last_Name" -> entity.setLastName((String) value);
                case "Citizenship" -> entity.setCitizenshipCountryId(getCitizenshipCountryId((String) value));
                case "Birth_Date" -> entity.setBirthDate(dateTimeParser.parseBirthDate((String) value));
                case "Gender" -> entity.setGender(Gender.of((String) value));
                case "Email" -> entity.setEmail((String) value);
                case "KYC" -> entity.setKycStatusId((Integer) value);
                default -> {
                    if (!List.of(TOWN, POST_CODE, ADDRESS_1, ADDRESS_2, MSISDN, ALT_CONTACT_MOBILE, ALT_CONTACT_NAME).contains(column)) {
                        log.warn("Unknown entity field: '{}' has value: '{}'", column, value);
                    }
                }
            }
        });
    }

    private EntityIdType getEntityIdType(String idTypeStr) {
        return entityIdTypeMap.computeIfAbsent(idTypeStr, idTypeStr1 -> {
            String idTypeDescription = LEGACY_ID_MAPPING.entrySet().stream()
                    .filter(entry -> Objects.equals(entry.getValue(), idTypeStr1))
                    .map(Map.Entry::getKey)
                    .findAny().orElseThrow();
            return entityIdTypeRepository.findByDescription(idTypeDescription).orElse(null);
        });
    }

    private void updateAddressIfNeed(Map<String, Object> data, Integer entityId) {
        if (containsKey(data, List.of(TOWN, POST_CODE, ADDRESS_1, ADDRESS_2))) {
            Address address = addressRepository.findByEntityIdAndAddressType(entityId, AddressType.PRIMARY)
                    .orElse(new Address().setEntityId(entityId).setAddressType(AddressType.PRIMARY)
                            .setCountryId(countryRepository.findByIsoCode("ZIM").orElseThrow().getId()));
            if (data.containsKey(TOWN)) {
                address.setCity((String) data.get(TOWN));
            }
            if (data.containsKey(POST_CODE)) {
                address.setPostalCode((String) data.get(POST_CODE));
            }
            if (data.containsKey(ADDRESS_1)) {
                address.setAddress1((String) data.get(ADDRESS_1));
            }
            if (data.containsKey(ADDRESS_2)) {
                address.setAddress2((String) data.get(ADDRESS_2));
            }
            addressRepository.save(address);
        }
    }

    private void updatePhonesIfNeed(Map<String, Object> data, EntityClass entity) {
        if (data.containsKey(MSISDN)) {
            EntityMsisdn msisdn = entityMsisdnRepository.findByEntityIdAndPrimaryMsisdn(entity.getId())
                    .orElse(new EntityMsisdn().setEntityId(entity.getId()).setMsisdnType(MsisdnType.PRIMARY)
                            .setCreatedDate(entity.getCreatedDate()));
            msisdn.setMsisdn((String) data.get(MSISDN));
            msisdn.setDescription((String) data.get(MSISDN));
            entityMsisdnRepository.save(msisdn);
        }
        if (containsKey(data, List.of(ALT_CONTACT_MOBILE, ALT_CONTACT_NAME))) {
            List<EntityMsisdn> msisdnList = entityMsisdnRepository.findByEntityIdAndMsisdnType(entity.getId(), MsisdnType.SECONDARY);
            EntityMsisdn msisdn = !msisdnList.isEmpty() ? msisdnList.get(0) : new EntityMsisdn()
                    .setEntityId(entity.getId()).setMsisdnType(MsisdnType.SECONDARY).setCreatedDate(entity.getCreatedDate());
            msisdn.setMsisdn((String) data.get(ALT_CONTACT_MOBILE));
            msisdn.setDescription((String) data.get(ALT_CONTACT_NAME));
            entityMsisdnRepository.save(msisdn);
        }
    }
}

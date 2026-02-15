package cash.ice.api.controller;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.RegisterEntityRequest;
import cash.ice.api.dto.SortInput;
import cash.ice.api.dto.backoffice.EntitiesSearchCriteria;
import cash.ice.api.service.*;
import cash.ice.api.util.MappingUtil;
import cash.ice.sqldb.converter.JsonToMapConverter;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class EntityController {
    private static final JsonToMapConverter JSON_TO_MAP_CONVERTER = new JsonToMapConverter();

    private final EntityRegistrationService entityRegistrationService;
    private final EntityRegistrationMozService entityRegistrationMozService;
    private final EntityService entityService;
    private final StaffMemberService staffMemberService;
    private final EntityMozService entityMozService;
    private final EntitySearchService entitySearchService;
    private final AuthUserService authUserService;
    private final EntityTypeGroupRepository entityTypeGroupRepository;
    private final EntityTypeRepository entityTypeRepository;
    private final EntityRepository entityRepository;
    private final CountryRepository countryRepository;
    private final AccountRepository accountRepository;
    private final EntityIdTypeRepository entityIdTypeRepository;
    private final EntityMsisdnRepository entityMsisdnRepository;
    private final AddressRepository addressRepository;
    private final AccountRelationshipRepository accountRelationshipRepository;
    private final SecurityGroupRepository securityGroupRepository;

    @MutationMapping
    public EntityClass registerEntity(@Argument RegisterEntityRequest entity) {
        log.info("Received Register new entity request: {}", entity);
        return entityRegistrationService.registerEntity(entity).getEntity();
    }

    @MutationMapping
    public EntityClass simpleRegisterEntity(@Argument RegisterEntityRequest entity) {
        log.info("Received simple Register new entity request: {}", entity);
        return entityRegistrationService.registerEntity(entity).getEntity();
    }

    @QueryMapping
    public boolean existsUserId(@Argument Integer idTypeId, @Argument String idNumber, @Argument boolean isEntity, @Argument boolean forceCheck) {
        log.info("> exists user ID: typeId: {}, number: {}, isEntity: {}, forceCheck: {}", idTypeId, idNumber, isEntity, forceCheck);
        if (isEntity) {
            return entityRegistrationService.isExistsId(idTypeId, idNumber, forceCheck);
        } else {
            return staffMemberService.isExistsId(idTypeId, idNumber);
        }
    }

    @QueryMapping
    public boolean existsUserEmail(@Argument String email, @Argument boolean isEntity, @Argument boolean forceCheck) {
        log.info("> exists user email: {}, isEntity: {}, forceCheck: {}", email, isEntity, forceCheck);
        if (isEntity) {
            return entityRegistrationService.isExistsEmail(email, forceCheck);
        } else {
            return staffMemberService.isExistsEmail(email);
        }
    }

    @QueryMapping
    public boolean existsUserMsisdn(@Argument String msisdn, @Argument boolean checkOnlyPrimary, @Argument boolean forceCheck) {
        log.info("> exists user msisdn: {}, checkOnlyPrimary: {}, forceCheck: {}", msisdn, checkOnlyPrimary, forceCheck);
        return entityRegistrationService.isExistsMsisdn(msisdn, checkOnlyPrimary, forceCheck);
    }

    @MutationMapping
    @PreAuthorize("@EntitiesProperties.securityDisabled || isAuthenticated()")
    public EntityClass updateCurrentEntity(@Argument EntityClass entity) {
        return entityService.updateEntity(getAuthUser(), entity);
    }

    @MutationMapping
    @PreAuthorize("@EntitiesProperties.securityDisabled || isAuthenticated()")
    public EntityClass updateEntityLocale(@Argument Locale locale) {
        log.info("> Update entity locale: {}", locale);
        return entityService.updateEntityLocale(getAuthUser(), locale);
    }

    @MutationMapping
    @PreAuthorize("@EntitiesProperties.securityDisabled || isAuthenticated()")
    public EntityClass generateNewBackupCodesForEntityId(@Argument Integer id) {
        log.info("> Generate new backup codes for entity: {}", id);
        return entityService.generateNewBackupCodes(id);
    }

    @MutationMapping
    @PreAuthorize("@EntitiesProperties.securityDisabled || isAuthenticated()")
    public EntityClass generateNewBackupCodesForCurrentEntity() {
        AuthUser authUser = getAuthUser();
        log.info("> Generate new backup codes, authUser: {}", authUser);
        return entityService.generateNewBackupCodes(authUser);
    }

    @MutationMapping
    @PreAuthorize("@EntitiesProperties.securityDisabled || isAuthenticated()")
    public EntityClass deleteCurrentEntity() {
        AuthUser authUser = getAuthUser();
        log.info("> Delete current entity: {}", authUser);
        return entityService.deleteEntity(authUser);
    }

    @MutationMapping
    public Optional<EntityClass> deleteEntity(@Argument Integer id) {
        Optional<EntityClass> entity = entityRepository.findById(id);
        entity.ifPresent(entityRepository::delete);
        return entity;
    }

    @QueryMapping
    public Page<EntityClass> allEntities(@Argument int page, @Argument int size, @Argument SortInput sort) {
        return entityRepository.findAll(PageRequest.of(page, size, SortInput.toSort(sort)));
    }

    @QueryMapping
    public Page<EntityClass> searchEntities(@Argument EntitiesSearchCriteria searchBy, @Argument String searchInput,
                                            @Argument boolean exactMatch, @Argument int page, @Argument int size, @Argument SortInput sort) {
        return entitySearchService.searchEntities(searchBy, searchInput, exactMatch, page, size, sort);
    }

    @QueryMapping
    @PreAuthorize("@EntitiesProperties.securityDisabled || isAuthenticated()")
    public EntityClass entity(@Argument Integer id) {
        if (id != null) {
            return entityService.getEntityById(id);
        } else {
            return entityService.getEntity(getAuthUser());
        }
    }

    @QueryMapping
    public Iterable<EntityType> allEntityTypes(@Argument int page, @Argument int size, @Argument SortInput sort) {
        return entityTypeRepository.findAll(PageRequest.of(page, size, SortInput.toSort(sort)));
    }

    @QueryMapping
    public Iterable<EntityTypeGroup> allEntityTypeGroups(@Argument int page, @Argument int size, @Argument SortInput sort) {
        return entityTypeGroupRepository.findAll(PageRequest.of(page, size, SortInput.toSort(sort)));
    }

    @SchemaMapping(typeName = "Entity", field = "meta")
    public String entityMeta(EntityClass entity) {
        return JSON_TO_MAP_CONVERTER.convertToDatabaseColumn((Serializable) entity.getMeta());
    }

    @BatchMapping(typeName = "Entity", field = "entityType")
    public Map<EntityClass, EntityType> entityType(List<EntityClass> entities) {
        return MappingUtil.itemsToCategoriesMap(entities, EntityClass::getEntityTypeId,
                EntityType::getId, entityTypeRepository);
    }

    @BatchMapping(typeName = "EntityType", field = "entities")
    public Map<EntityType, List<EntityClass>> entities(List<EntityType> entityType) {
        return MappingUtil.categoriesToItemsListMap(entityType, EntityType::getId, EntityClass::getEntityTypeId,
                entityRepository::findByEntityTypeIdIn);
    }

    @BatchMapping(typeName = "EntityType", field = "entityTypeGroup")
    public Map<EntityType, EntityTypeGroup> entityTypeGroups(List<EntityType> entityTypes) {
        return MappingUtil.itemsToCategoriesMap(entityTypes, EntityType::getEntityTypeGroupId,
                EntityTypeGroup::getId, entityTypeGroupRepository);
    }

    @BatchMapping(typeName = "EntityTypeGroup", field = "entityTypes")
    public Map<EntityTypeGroup, List<EntityType>> entityTypes(List<EntityTypeGroup> entityTypeGroups) {
        return MappingUtil.categoriesToItemsListMap(entityTypeGroups, EntityTypeGroup::getId,
                EntityType::getEntityTypeGroupId, entityTypeRepository::findByEntityTypeGroupIdIn);
    }

    @BatchMapping(typeName = "Entity", field = "accounts")
    public Map<EntityClass, List<Account>> accounts(List<EntityClass> entities) {
        return MappingUtil.categoriesToItemsListMap(entities, EntityClass::getId, Account::getEntityId,
                accountRepository::findByEntityIdIn);
    }

    @SchemaMapping(typeName = "Entity", field = "idTypeId")
    public Integer entityIdTypeId(EntityClass entity) {
        return entity.getIdType();
    }

    @BatchMapping(typeName = "Entity", field = "idType")
    public Map<EntityClass, EntityIdType> entityIdType(List<EntityClass> entities) {
        return MappingUtil.itemsToCategoriesMap(entities, EntityClass::getIdType,
                EntityIdType::getId, entityIdTypeRepository);
    }

    @BatchMapping(typeName = "Entity", field = "citizenshipCountry")
    public Map<EntityClass, Country> citizenshipCountry(List<EntityClass> entities) {
        return MappingUtil.itemsToCategoriesMap(entities, EntityClass::getCitizenshipCountryId,
                Country::getId, countryRepository);
    }

    @BatchMapping(typeName = "Entity", field = "msisdn")
    public Map<EntityClass, List<EntityMsisdn>> msisdn(List<EntityClass> entities) {
        return MappingUtil.categoriesToItemsListMap(entities, EntityClass::getId, EntityMsisdn::getEntityId,
                entityMsisdnRepository::findByEntityIdIn);
    }

    @BatchMapping(typeName = "Entity", field = "address")
    public Map<EntityClass, List<Address>> address(List<EntityClass> entities) {
        return MappingUtil.categoriesToItemsListMap(entities, EntityClass::getId, Address::getEntityId,
                addressRepository::findByEntityIdIn);
    }

    @BatchMapping(typeName = "Address", field = "country")
    public Map<Address, Country> addressCountry(List<Address> entities) {
        return MappingUtil.itemsToCategoriesMap(entities, Address::getCountryId,
                Country::getId, countryRepository);
    }

    @BatchMapping(typeName = "Entity", field = "relationships")
    public Map<EntityClass, List<AccountRelationship>> relationships(List<EntityClass> entities) {
        return MappingUtil.categoriesToItemsListMap(entities, EntityClass::getId, AccountRelationship::getEntityId,
                accountRelationshipRepository::findByEntityIdIn);
    }

    @BatchMapping(typeName = "AccountRelationship", field = "entity")
    public Map<AccountRelationship, EntityClass> accountRelationshipEntity(List<AccountRelationship> accountRelationships) {
        return MappingUtil.itemsToCategoriesMap(accountRelationships, AccountRelationship::getEntityId,
                EntityClass::getId, entityRepository);
    }

    @BatchMapping(typeName = "AccountRelationship", field = "partnerAccount")
    public Map<AccountRelationship, Account> accountRelationshipPartner(List<AccountRelationship> accountRelationships) {
        return MappingUtil.itemsToCategoriesMap(accountRelationships, AccountRelationship::getEntityId,
                Account::getId, accountRepository);
    }

    @BatchMapping(typeName = "AccountRelationship", field = "securityGroupMoz")
    public Map<AccountRelationship, List<SecurityGroup>> accountRelationshipMozSecurityGroup(List<AccountRelationship> accountRelationships) {
        List<Integer> allNeededSecurityGroupsIds = new ArrayList<>();
        accountRelationships.forEach(accountRelationship -> {
            Object groupIds = accountRelationship.getSecurityGroups().get("MOZ");
            if (groupIds instanceof List<?>) {
                allNeededSecurityGroupsIds.addAll((List<Integer>) groupIds);
            } else if (groupIds instanceof Integer) {
                allNeededSecurityGroupsIds.add((Integer) groupIds);
            }
        });
        Map<Integer, SecurityGroup> allNeededSecurityGroups = securityGroupRepository.findAllById(allNeededSecurityGroupsIds).stream().collect(Collectors.toMap(SecurityGroup::getId, item -> item));
        return accountRelationships.stream().collect(Collectors.toMap(item -> item, accountRelationship -> {
            Object groupIds = accountRelationship.getSecurityGroups().get("MOZ");
            if (groupIds instanceof List<?>) {
                return ((List<Integer>) groupIds).stream().map(allNeededSecurityGroups::get).toList();
            } else if (groupIds instanceof Integer) {
                return List.of(allNeededSecurityGroups.get((Integer) groupIds));
            } else {
                return List.of();
            }
        }));
    }

    protected AuthUser getAuthUser() {
        return authUserService.getAuthUser();
    }
}

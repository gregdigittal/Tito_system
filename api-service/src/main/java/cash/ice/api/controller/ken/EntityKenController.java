package cash.ice.api.controller.ken;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.SortInput;
import cash.ice.api.dto.moz.AccountTypeKen;
import cash.ice.api.dto.moz.IdTypeKen;
import cash.ice.api.dto.moz.RegisterEntityKenRequest;
import cash.ice.api.entity.ken.EntityProduct;
import cash.ice.api.entity.ken.Product;
import cash.ice.api.entity.moz.ProductRelationshipType;
import cash.ice.api.repository.ken.EntityKenRepository;
import cash.ice.api.repository.ken.EntityProductRepository;
import cash.ice.api.repository.ken.ProductRepository;
import cash.ice.api.service.AuthUserService;
import cash.ice.api.service.EntityKenService;
import cash.ice.api.service.EntityRegistrationKenService;
import cash.ice.api.service.PermissionsService;
import cash.ice.api.util.MappingUtil;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;
import cash.ice.common.error.ICEcashException;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cash.ice.common.error.ErrorCodes.EC1062;
import static cash.ice.common.error.ErrorCodes.EC1063;
import static cash.ice.sqldb.entity.AccountType.FNDS_ACCOUNT;
import static cash.ice.sqldb.entity.Currency.KES;

@Controller
@RequiredArgsConstructor
@Slf4j
public class EntityKenController {
    private final EntityRegistrationKenService entityRegistrationKenService;
    private final EntityKenService entityKenService;
    private final AuthUserService authUserService;
    private final PermissionsService permissionsService;
    private final CurrencyRepository currencyRepository;
    private final ProductRepository productRepository;
    private final EntityProductRepository entityProductRepository;
    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final EntityKenRepository entityKenRepository;
    private final EntityTypeRepository entityTypeRepository;
    private final EntityIdTypeRepository entityIdTypeRepository;
    private final EntityMsisdnRepository entityMsisdnRepository;
    private final AccountRelationshipRepository accountRelationshipRepository;

    @MutationMapping
    public EntityClass registerUserFNDS(@Argument RegisterEntityKenRequest user, @Argument String otp, @Argument boolean removeDocumentsOnFail) {
        AuthUser authUser = getAuthUser();
        log.info("> register user (FNDS): {}, otp: {}, in transaction: {}, regUser: {}", user, otp, removeDocumentsOnFail, authUser);
        return entityRegistrationKenService.registerUser(user, authUser, otp, removeDocumentsOnFail).getEntity();
    }

    @QueryMapping
    public Page<EntityClass> entitiesSearchFNDS(@Argument AccountTypeKen accountType, @Argument IdTypeKen idType, @Argument String idNumber,
                                                @Argument String mobile, @Argument int page, @Argument int size, @Argument SortInput sort) {
        log.info("> GET EntitiesSearch (FNDS), accountType: {}, idType: {}, idNumber: {}, mobile: {}, page: {}, size: {}, sort: {}",
                accountType, idType, idNumber, mobile, page, size, sort);
        PageRequest pageable = PageRequest.of(page, size, SortInput.toSort(sort));
        return entityKenService.entitiesSearchFNDS(accountType, idType, idNumber, mobile, pageable);
    }

    @QueryMapping
    public List<EntityProduct> deviceProductsFNDS(@Argument String deviceSerialOrCode) {
        log.info("> GET products (FNDS) for device: " + deviceSerialOrCode);
        return entityKenService.getDeviceProductsFNDS(deviceSerialOrCode);
    }

    @MutationMapping
    public PaymentResponse makePaymentFNDS(@Argument PaymentRequest paymentRequest) {
        log.info("> " + paymentRequest);
        return entityKenService.makePayment(paymentRequest);
    }

    @MutationMapping
    public PaymentResponse makeBulkPaymentFNDS(@Argument List<PaymentRequest> payments) {
        log.info("> bulk payment: " + payments);
        return entityKenService.makeBulkPayment(payments);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public EntityClass userFNDS() {
        log.info("> GET user (FNDS): {}", getAuthUser());
        return permissionsService.getAuthEntity(getAuthUser());
    }

    @QueryMapping
    public List<Product> productsFNDS() {
        log.info("> GET products (FNDS)");
        return productRepository.findAll();
    }

    @BatchMapping(typeName = "EntityProductFNDS", field = "entity")
    public Map<EntityProduct, EntityClass> entityProductEntities(List<EntityProduct> entityProducts) {
        return MappingUtil.itemsToCategoriesMap(entityProducts, EntityProduct::getEntityId,
                EntityClass::getId, entityKenRepository);
    }

    @BatchMapping(typeName = "EntityProductFNDS", field = "product")
    public Map<EntityProduct, Product> entityProductProducts(List<EntityProduct> entityProducts) {
        return MappingUtil.itemsToCategoriesMap(entityProducts, EntityProduct::getProductId,
                Product::getId, productRepository);
    }

    @SchemaMapping(typeName = "EntityProductFNDS", field = "relationship")
    public ProductRelationshipType entityProductRelationship(EntityProduct entityProduct) {
        return entityProduct.getRelationshipType();
    }


    @BatchMapping(typeName = "ProductFNDS", field = "currency")
    public Map<Product, Currency> ProductCurrencies(List<Product> products) {
        return MappingUtil.itemsToCategoriesMap(products, Product::getCurrencyId,
                Currency::getId, currencyRepository);
    }

    @BatchMapping(typeName = "UserFNDS", field = "fndsKesAccountBalance")
    public Map<EntityClass, BigDecimal> accountBalance(List<EntityClass> entities) {
        Map<Integer, EntityClass> entitiesMap = entities.stream().collect(Collectors.toMap(EntityClass::getId, e -> e));
        Currency currency = currencyRepository.findByIsoCode(KES).orElseThrow(() ->
                new ICEcashException(EC1062, String.format("Currency '%s' does not exist", KES), false));
        AccountType accountType = accountTypeRepository.findByNameAndCurrencyId(FNDS_ACCOUNT, currency.getId()).orElseThrow(() ->
                new ICEcashException(EC1063, String.format("'%s' account type for '%s' currency does not exist", FNDS_ACCOUNT, KES), false));
        List<Account> accounts = accountRepository.findByAccountTypeIdAndEntityIdIn(accountType.getId(),
                entities.stream().map(EntityClass::getId).toList());
        Map<Account, BigDecimal> balanceMap = MappingUtil.accountBalancesMap(accounts, accountBalanceRepository::findByAccountIdIn, () -> BigDecimal.ZERO);
        return balanceMap.entrySet().stream().collect(Collectors.toMap(entry -> entitiesMap.get(entry.getKey().getEntityId()), Map.Entry::getValue));
    }

    @BatchMapping(typeName = "UserFNDS", field = "products")
    public Map<EntityClass, List<EntityProduct>> userProducts(List<EntityClass> entities) {
        return MappingUtil.categoriesToItemsListMap(entities, EntityClass::getId, EntityProduct::getEntityId,
                entityProductRepository::findByEntityIdIn);
    }

    @BatchMapping(typeName = "UserFNDS", field = "entityType")
    public Map<EntityClass, EntityType> entityType(List<EntityClass> entities) {
        return MappingUtil.itemsToCategoriesMap(entities, EntityClass::getEntityTypeId,
                EntityType::getId, entityTypeRepository);
    }

    @SchemaMapping(typeName = "UserFNDS", field = "accounts")
    public List<Account> accounts(EntityClass entity) {
        return accountRepository.findByEntityId(entity.getId());
    }

    @SchemaMapping(typeName = "UserFNDS", field = "idTypeId")
    public Integer entityIdTypeId(EntityClass entity) {
        return entity.getIdType();
    }

    @BatchMapping(typeName = "UserFNDS", field = "idType")
    public Map<EntityClass, EntityIdType> entityIdType(List<EntityClass> entities) {
        return MappingUtil.itemsToCategoriesMap(entities, EntityClass::getIdType,
                EntityIdType::getId, entityIdTypeRepository);
    }

    @BatchMapping(typeName = "UserFNDS", field = "msisdn")
    public Map<EntityClass, List<EntityMsisdn>> msisdn(List<EntityClass> entities) {
        return MappingUtil.categoriesToItemsListMap(entities, EntityClass::getId, EntityMsisdn::getEntityId,
                entityMsisdnRepository::findByEntityIdIn);
    }

    @BatchMapping(typeName = "UserFNDS", field = "relationships")
    public Map<EntityClass, List<AccountRelationship>> relationships(List<EntityClass> entities) {
        return MappingUtil.categoriesToItemsListMap(entities, EntityClass::getId, AccountRelationship::getEntityId,
                accountRelationshipRepository::findByEntityIdIn);
    }

    protected AuthUser getAuthUser() {
        return authUserService.getAuthUser();
    }
}

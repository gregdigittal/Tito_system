package cash.ice.api.controller.moz;

import cash.ice.api.config.property.MozProperties;
import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.ConfigInput;
import cash.ice.api.dto.SortInput;
import cash.ice.api.dto.TransactionView;
import cash.ice.api.dto.moz.*;
import cash.ice.api.entity.moz.Route;
import cash.ice.api.repository.moz.RouteRepository;
import cash.ice.api.service.*;
import cash.ice.api.dto.moz.TripMoz;
import cash.ice.api.dto.moz.TripsPageableMoz;
import cash.ice.api.util.MappingUtil;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.sqldb.entity.*;
import cash.ice.api.dto.moz.LinkedBankAccountMoz;
import cash.ice.sqldb.entity.moz.Device;
import cash.ice.sqldb.entity.moz.Vehicle;
import cash.ice.sqldb.repository.*;
import cash.ice.sqldb.repository.moz.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cash.ice.api.util.MappingUtil.accountBalancesMap;
import static cash.ice.api.util.MappingUtil.itemsToCategoriesMap;
import static cash.ice.common.error.ErrorCodes.EC1022;
import static cash.ice.common.error.ErrorCodes.EC1072;
import static org.springframework.http.HttpStatus.OK;

@Controller
@RequiredArgsConstructor
@Slf4j
public class EntityMozController {
    private final EntityRegistrationMozService entityRegistrationMozService;
    private final EntityMozService entityMozService;
    private final TransactionStatisticsService transactionStatisticsService;
    private final EntityService entityService;
    private final AuthUserService authUserService;
    private final AccountTransferMozService accountTransferMozService;
    private final Me60MozService me60MozService;
    private final DeviceLinkMozService deviceLinkMozService;
    private final LinkedBankAccountMozService linkedBankAccountMozService;
    private final TripMozService tripMozService;
    private final PermissionsGroupService permissionsGroupService;
    private final EntityTypeRepository entityTypeRepository;
    private final EntityIdTypeRepository entityIdTypeRepository;
    private final EntityMsisdnRepository entityMsisdnRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final AccountRepository accountRepository;
    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountRelationshipRepository accountRelationshipRepository;
    private final InitiatorRepository initiatorRepository;
    private final InitiatorTypeRepository initiatorTypeRepository;
    private final TransactionCodeRepository transactionCodeRepository;
    private final CurrencyRepository currencyRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionLinesRepository transactionLinesRepository;
    private final MozProperties mozProperties;

    @MutationMapping
    public EntityClass registerIndividualUserMoz(@Argument RegisterEntityMozRequest user, @Argument OptionalEntityRegisterData optionalData, @Argument String otp, @Argument boolean removeDocumentsOnFail) {
        AuthUser authUser = getAuthUser();
        log.info("> register user (moz): {}, optional: {}, otp: {}, in transaction: {}, regUser: {}", user, optionalData, otp, removeDocumentsOnFail, authUser);
        return entityRegistrationMozService.registerUser(user, optionalData, authUser, otp, removeDocumentsOnFail).getEntity();
    }

    @MutationMapping
    public EntityClass registerCorporateUserMoz(@Argument RegisterCompanyMozRequest company, @Argument RegisterEntityMozRequest representative, @Argument OptionalEntityRegisterData optionalData, @Argument String otp, @Argument boolean removeDocumentsOnFail) {
        AuthUser authUser = getAuthUser();
        log.info("> register corporate user (moz): company: {}, representative: {}, optional: {}, otp: {}, in transaction: {}, regUser: {}", company, representative, optionalData, otp, removeDocumentsOnFail, authUser);
        return entityRegistrationMozService.registerCorporateUser(company, representative, optionalData, authUser, otp, removeDocumentsOnFail).getEntity();
    }

    @QueryMapping
    public Dictionary userRegistrationAgreement(@Argument Locale locale, @Argument AccountTypeMoz accountType) {
        return entityRegistrationMozService.getRegistrationAgreement(locale, accountType);
    }

    @QueryMapping
    public Iterable<EntityClass> lookupEntityMoz(@Argument LookupEntityType lookupBy, @Argument IdTypeMoz idType, @Argument String value) {
        return entityMozService.lookupEntity(lookupBy, idType, value);
    }

    @QueryMapping
    @ResponseStatus(code = OK)
    public MozAccountInfoResponse userSimpleAccountInfoMoz(@Argument String device) {
        log.info("> GET simple account info (moz): " + device);
        return me60MozService.getAccountInfo(device);
    }

    @SchemaMapping(typeName = "SimpleAccountInfoMoz", field = "route")
    public Route getRoute(MozAccountInfoResponse simpleAccountInfoMoz) {
        if (simpleAccountInfoMoz.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(simpleAccountInfoMoz.getVehicleId()).orElseThrow(() ->
                    new ICEcashException(String.format("Vehicle with ID: %s does not exist", simpleAccountInfoMoz.getVehicleId()), ErrorCodes.EC1076));
            if (vehicle.getRouteId() != null) {
                return routeRepository.findById(vehicle.getRouteId()).orElseThrow(() -> new ICEcashException(EC1072, "Invalid route"));
            }
        }
        return null;
    }

    @MutationMapping
    public String registerDeviceMoz(@Argument MozAutoRegisterDeviceRequest request) {
        log.info("> device register (moz): " + request);
        return me60MozService.registerDevice(request);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Device linkPosDeviceMoz(@Argument String posDeviceSerial, @Argument Integer entityId, @Argument String otp) {
        EntityClass authEntity = entityMozService.getAuthEntity(getAuthUser(), null);
        log.info("> link POS device. Serial: {}, entityId: {}, otp: {}, regUser: {}({} {})", posDeviceSerial, entityId, otp, authEntity.getId(), authEntity.getFirstName(), authEntity.getLastName());
        if (mozProperties.isLinkPosValidateAgent()) {
            permissionsGroupService.validateUserMozSecurityGroup(authEntity, AccountTypeMoz.AgentFematro.getSecurityGroupId());
        }
        return deviceLinkMozService.linkPosDevice(posDeviceSerial, entityId, otp);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Device linkVehicleToPosDeviceMoz(@Argument String posDeviceSerial, @Argument Integer vehicleId) {
        EntityClass authEntity = entityMozService.getAuthEntity(getAuthUser(), null);
        Objects.requireNonNull(authEntity);
        log.info("> link POS device to vehicleId: {}. Serial: {}, entityId: {} ({} {})", vehicleId, posDeviceSerial, authEntity.getId(), authEntity.getFirstName(), authEntity.getLastName());
        return deviceLinkMozService.linkPosDeviceToVehicle(posDeviceSerial, authEntity.getId(), vehicleId);
    }

    @MutationMapping
    public TagInfoMoz linkNfcTagMoz(@Argument LinkNfcTagRequest nfcTag, @Argument String identifier, @Argument String accountNumber, @Argument String otp) {
        LinkNfcTagRequest request = nfcTag;
        if (request == null && identifier != null && accountNumber != null) {
            request = new LinkNfcTagRequest().setDevice("").setTagNumber(identifier).setAccountNumber(accountNumber);
        }
        if (request == null) {
            throw new ICEcashException("Provide either nfcTag or both identifier and accountNumber", EC1022);
        }
        log.info("> link nfc tag: {}, otp: {}", request, otp);
        return deviceLinkMozService.linkNfcTag(request, otp != null ? otp : "");
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public TagInfoMoz delinkNfcTagMoz(@Argument String identifier) {
        EntityClass authEntity = entityMozService.getAuthEntity(getAuthUser(), null);
        Objects.requireNonNull(authEntity);
        log.info("> delink nfc tag: {}, entity: {} ({})", identifier, authEntity.getId(), authEntity.getFirstName());
        return deviceLinkMozService.delinkNfcTag(identifier, authEntity.getId());
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public EntityClass addOrUpdateMsisdnMoz(@Argument MsisdnType type, @Argument String msisdn, @Argument String oldMsisdn, @Argument String description, @Argument String otp) {
        EntityClass authEntity = entityMozService.getAuthEntity(getAuthUser(), null);
        Objects.requireNonNull(authEntity);
        log.info("> add or update Msisdn: {}, type: {}, entity: (id={}), oldMsisdn: {}, description: {}, otp: {}",
                msisdn, type, authEntity.getId(), oldMsisdn, description, otp);
        return entityMozService.addOrUpdateMsisdn(authEntity, type, msisdn, oldMsisdn, description, otp);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public PaymentResponse topupAccountMoz(@Argument String accountNumber, @Argument MoneyProviderMoz provider, @Argument String mobile, @Argument BigDecimal amount) {
        EntityClass authEntity = entityMozService.getAuthEntity(getAuthUser(), null);
        Objects.requireNonNull(authEntity);
        log.info("> topup account: {}, entity: (id={}), provider: {}, mobile: {}, amount: {}",
                accountNumber, authEntity.getId(), provider, mobile, amount);
        return entityMozService.topupAccount(authEntity, accountNumber, provider, mobile, amount);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public PaymentResponse cashOutToMobileMoney(@Argument String accountNumber, @Argument MoneyProviderMoz provider, @Argument String mobile, @Argument BigDecimal amount) {
        EntityClass authEntity = entityMozService.getAuthEntity(getAuthUser(), null);
        Objects.requireNonNull(authEntity);
        log.info("> cash-out to mobile money: account: {}, entity: (id={}), provider: {}, mobile: {}, amount: {}",
                accountNumber, authEntity.getId(), provider, mobile, amount);
        return entityMozService.cashOutToMobileMoney(authEntity, accountNumber, provider, mobile, amount);
    }

    @MutationMapping
    public PaymentResponse makePaymentMoz(@Argument PaymentRequest paymentRequest) {
        log.info("> " + paymentRequest);
        return me60MozService.makePayment(paymentRequest);
    }

    @MutationMapping
    public PaymentResponse makeBulkPaymentMoz(@Argument List<PaymentRequest> payments) {
        log.info("> bulk payment: " + payments);
        return me60MozService.makeBulkPayments(payments);
    }

    @QueryMapping
    @PreAuthorize("@MozProperties.securityDisabled || isAuthenticated()")
    public EntityClass userMoz(@Argument Integer id, @Argument ConfigInput config) {
        log.info("> GET user (moz): {}{}", id != null ? id : "current", config != null ? ", config: " + config : "");
        EntityClass authEntity = entityMozService.getAuthEntity(getAuthUser(), config);
        return id == null ? authEntity : entityMozService.getEntityById(id);
    }

    @QueryMapping
    @PreAuthorize("@MozProperties.securityDisabled || isAuthenticated()")
    public Page<TransactionView> userStatementMoz(@Argument Integer id, @Argument String accountType, @Argument String currency,
                                                  @Argument Integer vrnId, @Argument Integer tagId, @Argument String transactionCode, @Argument String description,
                                                  @Argument ConfigInput config, @Argument int page, @Argument int size, @Argument SortInput sort) {
        log.info("> GET user statement (moz): {}{}, accountType: {}, currency: {}, vrnId: {}, tagId: {}, tx: {}, descr: {}, page: {}, size: {}, sort: {}",
                id != null ? id : "current", config != null ? ", config: " + config : "", accountType, currency, vrnId, tagId, transactionCode, description, page, size, sort);
        EntityClass authEntity = entityMozService.getAuthEntity(getAuthUser(), config);
        return entityService.getEntityTransactions(authEntity, accountType, currency, vrnId, tagId, transactionCode, description, page, size, sort);
    }

    @QueryMapping
    @PreAuthorize("@MozProperties.securityDisabled || isAuthenticated()")
    public Page<Initiator> userPaymentDevicesMoz(@Argument Integer id, @Argument ConfigInput config, @Argument int page, @Argument int size, @Argument SortInput sort) {
        log.info("> GET user payment devices (moz): {}{}, page: {}, size: {}, sort: {}", id != null ? id : "current", config != null ? ", config: " + config : "", page, size, sort);
        EntityClass authEntity = entityMozService.getAuthEntity(getAuthUser(), config);
        return entityMozService.getEntityInitiators(authEntity, page, size, sort);
    }

    @QueryMapping
    public TagInfoMoz tagInfoMoz(@Argument String tagNumber) {
        log.info("> GET tag info (moz): {}", tagNumber);
        return entityMozService.getTagInfo(tagNumber);
    }

    @QueryMapping
    @PreAuthorize("@MozProperties.securityDisabled || isAuthenticated()")
    public Page<Device> userPosDevicesMoz(@Argument boolean linkedToVehicle, @Argument Integer id, @Argument ConfigInput config, @Argument int page, @Argument int size, @Argument SortInput sort) {
        log.info("> GET pos devices (moz): {}{}, linkedToVehicle: {}, page: {}, size: {}, sort: {}", id != null ? id : "current", config != null ? ", config: " + config : "", linkedToVehicle, page, size, sort);
        EntityClass authEntity = entityMozService.getAuthEntity(getAuthUser(), config);
        return entityMozService.getEntityDevices(authEntity, linkedToVehicle, page, size, sort);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<TransactionStatisticsMoz> transactionStatisticsMoz(@Argument StatisticsTypeMoz statisticsType, @Argument int days) {
        EntityClass authEntity = entityMozService.getAuthEntity(getAuthUser(), null);
        log.info("> GET transaction statistics (moz): type: {}, days: {}, entity: {} {} {}", statisticsType, days, authEntity.getId(), authEntity.getFirstName(), authEntity.getLastName());
        return transactionStatisticsService.getTransactionStatistics(authEntity, statisticsType, days);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<LinkedBankAccountMoz> listLinkedBankAccounts() {
        EntityClass authEntity = entityMozService.getAuthEntity(getAuthUser(), null);
        Objects.requireNonNull(authEntity);
        return linkedBankAccountMozService.listByEntityId(authEntity.getId());
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public TripsPageableMoz userTripsMoz(@Argument int page, @Argument int size) {
        EntityClass authEntity = entityMozService.getAuthEntity(getAuthUser(), null);
        Objects.requireNonNull(authEntity);
        return tripMozService.getTrips(authEntity.getId(), page, size);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public TripMoz currentTripMoz() {
        EntityClass authEntity = entityMozService.getAuthEntity(getAuthUser(), null);
        Objects.requireNonNull(authEntity);
        return tripMozService.getCurrentTrip(authEntity.getId());
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public LinkedBankAccountMoz linkBankAccount(@Argument String bankId, @Argument String branchCode, @Argument String accountNumber, @Argument String accountName, @Argument String currency) {
        EntityClass authEntity = entityMozService.getAuthEntity(getAuthUser(), null);
        Objects.requireNonNull(authEntity);
        return linkedBankAccountMozService.link(authEntity.getId(), bankId, branchCode, accountNumber, accountName, currency);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Boolean unlinkBankAccount(@Argument Integer id) {
        EntityClass authEntity = entityMozService.getAuthEntity(getAuthUser(), null);
        Objects.requireNonNull(authEntity);
        return linkedBankAccountMozService.unlink(id, authEntity.getId());
    }

    @BatchMapping(typeName = "Device", field = "account")
    public Map<Device, Account> deviceAccountNumber(List<Device> devices) {
        return MappingUtil.itemsToCategoriesMap(devices, Device::getAccountId, Account::getId, accountRepository);
    }

    @BatchMapping(typeName = "Device", field = "vehicle")
    public Map<Device, Vehicle> deviceVehicle(List<Device> devices) {
        return MappingUtil.itemsToCategoriesMap(devices, Device::getVehicleId, Vehicle::getId, vehicleRepository);
    }

    @MutationMapping
    public EntityClass interAccountTransferMoz(@Argument String fromAccountType, @Argument String toAccountType, @Argument String currency, @Argument BigDecimal amount, @Argument ConfigInput config) {
        log.info("> Inter account transfer (moz): {}, fromAccountType: {}, toAccountType: {}, amount: {}",
                config != null ? ", config: " + config : "", fromAccountType, toAccountType, amount);
        EntityClass authEntity = entityMozService.getAuthEntity(getAuthUser(), config);
        return accountTransferMozService.interAccountTransfer(authEntity, fromAccountType, toAccountType, currency, amount);
    }

    @BatchMapping(typeName = "UserMoz", field = "entityType")
    public Map<EntityClass, EntityType> entityType(List<EntityClass> entities) {
        return MappingUtil.itemsToCategoriesMap(entities, EntityClass::getEntityTypeId,
                EntityType::getId, entityTypeRepository);
    }

    @SchemaMapping(typeName = "UserMoz", field = "accounts")
    public List<Account> accounts(EntityClass entity) {
        return accountRepository.findByEntityId(entity.getId());
    }

    @SchemaMapping(typeName = "UserMoz", field = "idTypeId")
    public Integer entityIdTypeId(EntityClass entity) {
        return entity.getIdType();
    }

    @BatchMapping(typeName = "UserMoz", field = "idType")
    public Map<EntityClass, EntityIdType> entityIdType(List<EntityClass> entities) {
        return MappingUtil.itemsToCategoriesMap(entities, EntityClass::getIdType,
                EntityIdType::getId, entityIdTypeRepository);
    }

    @BatchMapping(typeName = "UserMoz", field = "msisdn")
    public Map<EntityClass, List<EntityMsisdn>> msisdn(List<EntityClass> entities) {
        return MappingUtil.categoriesToItemsListMap(entities, EntityClass::getId, EntityMsisdn::getEntityId,
                entityMsisdnRepository::findByEntityIdIn);
    }

    @BatchMapping(typeName = "UserMoz", field = "relationships")
    public Map<EntityClass, List<AccountRelationship>> relationships(List<EntityClass> entities) {
        return MappingUtil.categoriesToItemsListMap(entities, EntityClass::getId, AccountRelationship::getEntityId,
                accountRelationshipRepository::findByEntityIdIn);
    }

    @BatchMapping(typeName = "AccountMoz", field = "accountType")
    public Map<Account, AccountType> accountType(List<Account> accounts) {
        return itemsToCategoriesMap(accounts, Account::getAccountTypeId, AccountType::getId, accountTypeRepository);
    }

    @BatchMapping(typeName = "AccountMoz", field = "balance")
    public Map<Account, BigDecimal> accountBalance(List<Account> accounts) {
        return accountBalancesMap(accounts, accountBalanceRepository::findByAccountIdIn, () -> BigDecimal.ZERO);
    }

    @BatchMapping(typeName = "AccountMoz", field = "initiators")
    public Map<Account, List<Initiator>> initiators(List<Account> accounts) {
        return MappingUtil.categoriesToItemsListMap(accounts, Account::getId, Initiator::getAccountId,
                initiatorRepository::findByAccountIdIn);
    }

    @BatchMapping(typeName = "AccountMoz", field = "transactions")
    public Map<Account, List<TransactionView>> transactions(List<Account> accounts) {
        List<TransactionLines> lines = transactionLinesRepository.findByEntityAccountIdIn(accounts.stream().map(Account::getId).toList());
        List<Transaction> transactions = transactionRepository.findAllById(lines.stream().map(TransactionLines::getTransactionId).distinct().toList());

        Map<Integer, Account> idToAccount = accounts.stream().collect(Collectors.toMap(Account::getId, a -> a));
        Map<Integer, Transaction> idToTransaction = transactions.stream().collect(Collectors.toMap(Transaction::getId, t -> t));
        Map<Account, Map<Transaction, List<TransactionLines>>> result = lines.stream().collect(Collectors.groupingBy(
                line -> idToAccount.get(line.getEntityAccountId()),
                Collectors.groupingBy(line1 -> idToTransaction.get(line1.getTransactionId()))));
        return result.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getValue().entrySet().stream().map(ee -> TransactionView.create(ee.getKey(), ee.getValue())).toList()));
    }

    @BatchMapping(typeName = "TransactionMoz", field = "transactionCode")
    public Map<TransactionView, TransactionCode> transactionTransactionCode(List<TransactionView> transaction) {
        return itemsToCategoriesMap(transaction, TransactionView::getTransactionCodeId, TransactionCode::getId, transactionCodeRepository);
    }

    @BatchMapping(typeName = "TransactionMoz", field = "currency")
    public Map<TransactionView, Currency> transactionCurrency(List<TransactionView> transaction) {
        return itemsToCategoriesMap(transaction, TransactionView::getCurrencyId, Currency::getId, currencyRepository);
    }

    @BatchMapping(typeName = "TransactionMoz", field = "initiator")
    public Map<TransactionView, Initiator> transactionInitiator(List<TransactionView> transaction) {
        return itemsToCategoriesMap(transaction, TransactionView::getInitiatorId, Initiator::getId, initiatorRepository);
    }

    @BatchMapping(typeName = "TransactionMoz", field = "initiatorType")
    public Map<TransactionView, InitiatorType> transactionInitiatorType(List<TransactionView> transaction) {
        return itemsToCategoriesMap(transaction, TransactionView::getInitiatorTypeId, InitiatorType::getId, initiatorTypeRepository);
    }

    @BatchMapping(typeName = "TransactionLine", field = "transactionCode")
    public Map<TransactionLines, TransactionCode> transactionLineTransactionCode(List<TransactionLines> transactionLine) {
        return itemsToCategoriesMap(transactionLine, TransactionLines::getTransactionCodeId, TransactionCode::getId, transactionCodeRepository);
    }

    @BatchMapping(typeName = "TransactionLine", field = "entityAccount")
    public Map<TransactionLines, Account> transactionLineAccount(List<TransactionLines> transactionLine) {
        return itemsToCategoriesMap(transactionLine, TransactionLines::getTransactionCodeId, Account::getId, accountRepository);
    }

    protected AuthUser getAuthUser() {
        return authUserService.getAuthUser();
    }
}

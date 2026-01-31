package cash.ice.api.controller;

import cash.ice.api.dto.AccountInput;
import cash.ice.api.dto.ConfigInput;
import cash.ice.api.dto.SortInput;
import cash.ice.api.dto.TransactionView;
import cash.ice.api.entity.zim.Payment;
import cash.ice.api.repository.zim.PaymentRepository;
import cash.ice.api.service.*;
import cash.ice.api.util.MappingUtil;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static cash.ice.api.util.MappingUtil.accountBalancesMap;
import static cash.ice.api.util.MappingUtil.itemsToCategoriesMap;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AccountController {
    private final SecurityPvvService securityPvvService;
    private final NotificationService notificationService;
    private final AuthUserService authUserService;
    private final EntityService entityService;
    private final LoggerService loggerService;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountRepository accountRepository;
    private final EntityRepository entityRepository;
    private final EntityMsisdnRepository entityMsisdnRepository;
    private final PaymentRepository paymentRepository;
    private final InitiatorRepository initiatorRepository;
    private final InitiatorTypeRepository initiatorTypeRepository;
    private final InitiatorCategoryRepository initiatorCategoryRepository;
    private final InitiatorStatusRepository initiatorStatusRepository;

    @MutationMapping
    public Account addAccount(@Argument AccountInput account) {
        return accountRepository.save(account.toAccount().setCreatedDate(Tool.currentDateTime()));
    }

    @MutationMapping
    public Optional<Account> updateAccount(@Argument Integer id, @Argument AccountInput account) {
        return accountRepository.findById(id).map(account1 -> accountRepository.save(account.updateAccount(account1)));
    }

    @MutationMapping
    public Optional<Account> deleteAccount(@Argument Integer id) {
        Optional<Account> account = accountRepository.findById(id);
        account.ifPresent(accountRepository::delete);
        return account;
    }

    @MutationMapping
    public Initiator updateInitiatorStatus(@Argument Integer initiatorId, @Argument String status) {
        log.debug("> update initiator status: {}, initiatorId: {}", status, initiatorId);
        InitiatorStatus newStatus = initiatorStatusRepository.findByName(status).orElseThrow(() ->
                new ICEcashException(String.format("InitiatorStatus '%s' does not exist", status), ErrorCodes.EC1059, true));
        Initiator initiator = initiatorRepository.findById(initiatorId).orElseThrow(() ->
                new ICEcashException(String.format("Initiator with ID: %s wasn't found", initiatorId), ErrorCodes.EC1012));
        return initiatorRepository.save(initiator.setInitiatorStatusId(newStatus.getId()));
    }

    @MutationMapping
    public Initiator resetInitiatorPIN(@Argument Integer initiatorId) {
        log.debug("> reset initiator PIN: {}", initiatorId);
        Initiator initiator = initiatorRepository.findById(initiatorId).orElseThrow(() ->
                new ICEcashException(String.format("Initiator with ID: %s wasn't found", initiatorId), ErrorCodes.EC1012));
        Account account = accountRepository.findById(initiator.getAccountId()).orElseThrow(() ->
                new ICEcashException(String.format("Account with ID: %s wasn't found", initiator.getAccountId()), ErrorCodes.EC1022));
        EntityMsisdn msisdn = entityMsisdnRepository.findByEntityIdAndPrimaryMsisdn(account.getEntityId()).orElseThrow(() ->
                new ICEcashException(String.format("Msisdn for entity ID: %s wasn't found", account.getEntityId()), ErrorCodes.EC1046));

        String pin = Tool.generateDigits(4, false);
        Initiator savedInitiator = initiatorRepository.save(initiator
                .setPvv(securityPvvService.acquirePvv(initiator.getIdentifier(), pin)));
        notificationService.sendSmsMessage(pin, msisdn.getMsisdn());
        return savedInitiator;
    }

    @QueryMapping
    public Iterable<Account> allAccounts(@Argument int page, @Argument int size, @Argument SortInput sort) {
        return accountRepository.findAll(PageRequest.of(page, size, SortInput.toSort(sort)));
    }

    @QueryMapping
    public Account accountById(@Argument Integer id) {
        return accountRepository.findById(id).orElseThrow();
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Iterable<Account> accounts(@Argument int page, @Argument int size, @Argument SortInput sort) {
        EntityClass entity = entityService.getEntity(authUserService.getAuthUser());
        return entityService.getAccountsFor(entity, PageRequest.of(page, size, SortInput.toSort(sort)));
    }

    @QueryMapping
    @PreAuthorize("@EntitiesProperties.securityDisabled || isAuthenticated()")
    public Page<TransactionView> accountStatement(@Argument Integer id, @Argument String accountType, @Argument String currency, @Argument ConfigInput config, @Argument int page, @Argument int size, @Argument SortInput sort) {
        log.info("> GET account statement (moz): {}{}, accountType: {}, currency: {}, page: {}, size: {}, sort: {}",
                id != null ? id : "current", config != null ? ", config: " + config : "", accountType, currency, page, size, sort);
        EntityClass authEntity = entityService.getEntity(authUserService.getAuthUser());
        return entityService.getEntityTransactions(authEntity, accountType, currency, null, null, null, null, page, size, sort);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Account setAccountActive(@Argument Integer id, @Argument boolean active) {
        EntityClass entity = entityService.getEntity(authUserService.getAuthUser());
        return entityService.setAccountActive(entity, id, active);
    }

    @BatchMapping(typeName = "TransactionMoz", field = "metaData")
    public Map<TransactionView, Map<String, Object>> transactionMetadata(List<TransactionView> transactions) {
        List<PaymentRequest> requests = loggerService.getRequests(transactions.stream().map(TransactionView::getSessionId).toList(), PaymentRequest.class);
        Map<String, PaymentRequest> requestsMap = requests.stream().collect(Collectors.toMap(PaymentRequest::getVendorRef, e -> e));
        return transactions.stream().collect(Collectors.toMap(t -> t, t -> requestsMap.containsKey(t.getSessionId()) ? requestsMap.get(t.getSessionId()).getMetaData() : Map.of()));
    }

    @BatchMapping(typeName = "Account", field = "entity")
    public Map<Account, EntityClass> accountEntity(List<Account> accounts) {
        return itemsToCategoriesMap(accounts, Account::getEntityId, EntityClass::getId, entityRepository);
    }

    @BatchMapping(typeName = "Account", field = "balance")
    public Map<Account, BigDecimal> accountBalance(List<Account> accounts) {
        return accountBalancesMap(accounts, accountBalanceRepository::findByAccountIdIn, () -> BigDecimal.ZERO);
    }

    @BatchMapping(typeName = "Account", field = "payments")
    public Map<Account, List<Payment>> pendingPayments(List<Account> accounts) {
        return MappingUtil.categoriesToItemsListMap(accounts, Account::getId, Payment::getAccountId,
                paymentRepository::findByAccountIdIn);
    }

    @BatchMapping(typeName = "Account", field = "initiators")
    public Map<Account, List<Initiator>> initiators(List<Account> accounts) {
        return MappingUtil.categoriesToItemsListMap(accounts, Account::getId, Initiator::getAccountId,
                initiatorRepository::findByAccountIdIn);
    }

    @BatchMapping(typeName = "AccountBalance", field = "account")
    public Map<AccountBalance, Account> account(List<AccountBalance> accountBalances) {
        return itemsToCategoriesMap(accountBalances, AccountBalance::getAccountId, Account::getId, accountRepository);
    }

    @BatchMapping(typeName = "Initiator", field = "initiatorType")
    public Map<Initiator, InitiatorType> initiatorType(List<Initiator> initiators) {
        return itemsToCategoriesMap(initiators, Initiator::getInitiatorTypeId, InitiatorType::getId, initiatorTypeRepository);
    }

    @BatchMapping(typeName = "Initiator", field = "initiatorCategory")
    public Map<Initiator, InitiatorCategory> initiatorCategory(List<Initiator> initiators) {
        return itemsToCategoriesMap(initiators, Initiator::getInitiatorCategoryId, InitiatorCategory::getId, initiatorCategoryRepository);
    }

    @BatchMapping(typeName = "Initiator", field = "initiatorStatus")
    public Map<Initiator, InitiatorStatus> initiatorStatus(List<Initiator> initiators) {
        return itemsToCategoriesMap(initiators, Initiator::getInitiatorStatusId, InitiatorStatus::getId, initiatorStatusRepository);
    }

    @BatchMapping(typeName = "InitiatorType", field = "entity")
    public Map<InitiatorType, EntityClass> initiatorTypeEntity(List<InitiatorType> initiatorTypes) {
        return itemsToCategoriesMap(initiatorTypes, InitiatorType::getEntityId, EntityClass::getId, entityRepository);
    }
}

package cash.ice.api.service.impl;

import cash.ice.api.config.property.EntitiesProperties;
import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.SortInput;
import cash.ice.api.dto.TransactionView;
import cash.ice.api.entity.backoffice.StaffMember;
import cash.ice.api.errors.UnexistingUserException;
import cash.ice.api.service.EntityService;
import cash.ice.api.service.MfaService;
import cash.ice.api.service.PermissionsService;
import cash.ice.api.util.CsvUtil;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.Currency;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import jakarta.ws.rs.NotAuthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cash.ice.common.error.ErrorCodes.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntityServiceImpl implements EntityService {
    private static final String[] STATEMENT_CSV_HEADER = {"ID", "Transaction ID", "Code", "Currency", "Amount", "Initiator Type", "Channel", "Created Date", "Statement Date"};

    private final EntityRepository entityRepository;
    private final PermissionsService permissionsService;
    private final MfaService mfaService;
    private final AccountRepository accountRepository;
    private final LanguageRepository languageRepository;
    private final EntitiesProperties entitiesProperties;
    private final AccountTypeRepository accountTypeRepository;
    private final CurrencyRepository currencyRepository;
    private final TransactionCodeRepository transactionCodeRepository;
    private final InitiatorTypeRepository initiatorTypeRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionLinesRepository transactionLinesRepository;

    @Override
    public EntityClass getEntityById(Integer entityId) {
        return entityRepository.findById(entityId).orElseThrow(() -> new UnexistingUserException("id: " + entityId));
    }

    @Override
    @PreAuthorize("@EntitiesProperties.securityDisabled || isAuthenticated()")
    public EntityClass getEntity(AuthUser authUser) {
        return permissionsService.getAuthEntity(authUser);
    }

    @Override
    public EntityClass updateEntity(AuthUser authUser, EntityClass entityDetails) {
        EntityClass entity = permissionsService.getAuthEntity(authUser);
        return entityRepository.save(entity.updateData(entityDetails));
    }

    @Override
    public EntityClass updateEntity(Integer entityId, EntityClass details, StaffMember updater) {
        if (updater == null) {
            throw new NotAuthorizedException("Operation is allowed only for backoffice user");
        }
        EntityClass entity = getEntityById(entityId);
        return entityRepository.save(entity.updateData(details));
    }

    @Override
    public EntityClass updateEntityLocale(AuthUser authUser, Locale locale) {
        EntityClass entity = permissionsService.getAuthEntity(authUser);
        return entityRepository.save(entity.setLocale(locale));
    }

    @Override
    public EntityClass generateNewBackupCodes(Integer id) {
        return entityRepository.save(getEntityById(id)
                .setMfaBackupCodes(mfaService.generateBackupCodes(entitiesProperties.getMfa())));
    }

    @Override
    public EntityClass generateNewBackupCodes(AuthUser authUser) {
        EntityClass entity = permissionsService.getAuthEntity(authUser);
        return entityRepository.save(entity
                .setMfaBackupCodes(mfaService.generateBackupCodes(entitiesProperties.getMfa())));
    }

    @Override
    public EntityClass deleteEntity(AuthUser authUser) {
        EntityClass entity = permissionsService.getAuthEntity(authUser);
        entityRepository.delete(entity);
        return entity;
    }

    @Override
    public Language getEntityLanguage(EntityClass entity) {
        Locale locale = entity.getLocale() != null ? entity.getLocale() : Locale.ENGLISH;
        return languageRepository.findByLanguageKey(locale.getLanguage())
                .orElseThrow(() -> new ICEcashException("Unknown language is assigned to user: " +
                        locale.getLanguage(), ErrorCodes.EC1039));
    }

    @Override
    public Iterable<Account> getAccountsFor(EntityClass entity, PageRequest pageRequest) {
        return accountRepository.findByEntityId(entity.getId(), pageRequest);
    }

    @Override
    public Account setAccountActive(EntityClass entity, Integer accountId, boolean active) {
        Account account = accountRepository.findById(accountId).orElseThrow(() ->
                new ICEcashException("Account is absent", ErrorCodes.EC1022));
        if (!Objects.equals(account.getEntityId(), entity.getId())) {
            throw new ICEcashException("Forbidden operation", ErrorCodes.EC1070);
        }
        return accountRepository.save(account.setAccountStatus(active ? AccountStatus.ACTIVE : AccountStatus.FROZEN));
    }

    @Override
    public Page<TransactionView> getEntityTransactions(EntityClass authEntity, String accountTypeName, String currencyCode,
                                                       Integer vrnId, Integer tagId, String transactionCode, String description,
                                                       int page, int size, SortInput sort) {
        Currency currency = currencyRepository.findByIsoCode(currencyCode).orElseThrow(() ->
                new ICEcashException(String.format("Currency '%s' does not exist", currencyCode), EC1062));
        AccountType accountType = accountTypeRepository.findByNameAndCurrencyId(accountTypeName, currency.getId()).orElseThrow(() ->
                new ICEcashException(String.format("AccountType '%s' for currencyId: %s does not exist", accountTypeName, currency.getId()), EC1063));
        Account account = accountRepository.findByEntityIdAndAccountTypeId(authEntity.getId(), accountType.getId()).stream().findFirst().orElseThrow(() ->
                new ICEcashException(String.format("Account does not exist, entityId: %s, type: %s", authEntity.getId(), accountType.getId()), EC1022));
        Integer transactionCodeId = transactionCode == null ? null : transactionCodeRepository.getTransactionCodeByCode(transactionCode).map(TransactionCode::getId)
                .orElseThrow(() -> new ICEcashException(String.format("Transaction code '%s' does not exist!", transactionCode), EC1061));
        Page<Transaction> transactions = transactionRepository.findTransactionsByAccount(account.getId(), vrnId, tagId, transactionCodeId, description,
                PageRequest.of(page, size, SortInput.toSort(sort, transactionRepository.getFieldsRewriterMap())));
        Map<Integer, List<TransactionLines>> transactionIdToLines = transactionLinesRepository.findByEntityAccountIdAndTransactionIdIn(
                        account.getId(), transactions.stream().map(Transaction::getId).toList())
                .stream().collect(Collectors.groupingBy(TransactionLines::getTransactionId));
        return transactions.map(transaction -> TransactionView.create(transaction, transactionIdToLines.get(transaction.getId())));
    }

    public String getStatementCsv(EntityClass authEntity, String accountTypeName, String currencyCode, boolean header, Character delimiter, String rowDelimiter) {
        LocalDateTime minDateTime = LocalDateTime.now().minusDays(entitiesProperties.getStatementExportCsv().getMaxDays());
        Page<TransactionView> transactionViews = getEntityTransactions(authEntity, accountTypeName, currencyCode, null, null, null, null,
                0, entitiesProperties.getStatementExportCsv().getMaxRecords(), null);
        List<TransactionView> transactions = transactionViews.stream().filter(transaction ->
                transaction.getCreatedDate().isAfter(minDateTime)).toList();
        log.debug("  csv export after: {}, transactions: {}, total: {}", minDateTime, transactions.size(), transactionViews.getTotalElements());
        Map<Integer, String> transactionCodeMap = transactionCodeRepository.findAll().stream()
                .collect(Collectors.toMap(TransactionCode::getId, TransactionCode::getCode));
        Map<Integer, String> currencyMap = currencyRepository.findAll().stream()
                .collect(Collectors.toMap(Currency::getId, Currency::getIsoCode));
        Map<Integer, String> initiatorTypesMap = initiatorTypeRepository.findAll().stream()
                .collect(Collectors.toMap(InitiatorType::getId, InitiatorType::getDescription));
        CSVFormat csvFormat = CsvUtil.createCsvFormat(header, STATEMENT_CSV_HEADER, delimiter, rowDelimiter);
        try {
            return CsvUtil.listToCsv(transactions, csvFormat, (transaction, index) -> {
                List<String> list = new ArrayList<>();
                list.add(String.valueOf(index));
                list.add(String.valueOf(transaction.getId()));
                list.add(transaction.getTransactionCodeId() != null ? transactionCodeMap.get(transaction.getTransactionCodeId()) : null);
                list.add(transaction.getCurrencyId() != null ? currencyMap.get(transaction.getCurrencyId()) : null);
                list.add(transaction.getAmount().toString());
                list.add(transaction.getInitiatorTypeId() != null ? initiatorTypesMap.get(transaction.getInitiatorTypeId()) : null);
                list.add(Objects.toString(transaction.getChannelId()));
                list.add(transaction.getCreatedDate() != null ? Tool.getZimDateTimeString(transaction.getCreatedDate()) : null);
                list.add(transaction.getStatementDate() != null ? Tool.getZimDateTimeString(transaction.getStatementDate()) : null);
                return list;
            });
        } catch (IOException e) {
            throw new ICEcashException(e.getMessage(), ErrorCodes.EC1051);
        }
    }
}

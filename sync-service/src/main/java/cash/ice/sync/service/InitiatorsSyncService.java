package cash.ice.sync.service;

import cash.ice.common.performance.PerfStopwatch;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import cash.ice.sync.component.DateTimeParser;
import cash.ice.sync.dto.ChangeAction;
import cash.ice.sync.dto.DataChange;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static cash.ice.sync.task.Utils.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class InitiatorsSyncService implements DataMigrator {
    private static final Map<Integer, Integer> CATEGORY_TO_WALLET_MAP =
            Map.of(5, 46, 11, 46, 27, 99, 10, 10);
    @SuppressWarnings("SqlResolve")
    private static final String CARDS_SQL = "select Card_Number, Card_Category_ID, Account_ID, Status_ID, Notes, Created_Date from dbo.Cards";
    @SuppressWarnings("SqlResolve")
    private static final String ACCOUNTS_CARDS_SQL = "select top 1 PVV, Account_Fund_ID from dbo.Accounts_Cards where Card_Number = '%s' and Active = 1 order by Created_Date desc";
    @SuppressWarnings("SqlResolve")
    private static final String TRANSACTIONS_CARD_SQL = "select distinct Wallet_ID from dbo.Transactions_Card where Card_Number = '%s' and Wallet_ID != 1";
    private static final String ACCOUNT_ID = "Account_ID";
    private static final String WALLET_ID = "Wallet_ID";
    private static final String CREATED_DATE = "Created_Date";

    private final InitiatorCategoriesSyncService initiatorCategoriesSyncService;
    private final InitiatorStatusesSyncService initiatorStatusesSyncService;
    private final InitiatorTypeRepository initiatorTypeRepository;
    private final InitiatorCategoryRepository initiatorCategoryRepository;
    private final InitiatorStatusRepository initiatorStatusRepository;
    private final InitiatorRepository initiatorRepository;
    private final EntityRepository entityRepository;
    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final JdbcTemplate jdbcTemplate;
    private final JdbcTemplate newJdbcTemplate;
    private final DateTimeParser dateTimeParser;

    @Transactional
    @Override
    public void migrateData() {
        Map<Integer, InitiatorCategory> initiatorCategories = initiatorCategoriesSyncService.migrateInitiatorCategories();
        Map<Integer, InitiatorStatus> initiatorStatuses = initiatorStatusesSyncService.migrateInitiatorStatuses();

        log.debug("Start migrating Initiators");
        Map<Integer, Integer> legacyToEntityMap = entityRepository.findAll().stream().collect(
                Collectors.toMap(EntityClass::getLegacyAccountId, EntityClass::getId));
        log.debug("  collected {} entities", legacyToEntityMap.size());
        Map<String, Integer> accountNumberToIdMap = selectAllAccountNumbers();

        AtomicInteger counter = new AtomicInteger(0);
        InitiatorType cardType = initiatorTypeRepository.findByDescription("card").orElseThrow();
        Map<Integer, AccountType> accountTypeMap = new HashMap<>();

        PerfStopwatch rowWatch = new PerfStopwatch();
        PerfStopwatch accountsCardsWatch = new PerfStopwatch();
        PerfStopwatch transactionsCardWatch = new PerfStopwatch();
        PerfStopwatch cardWriteWatch = new PerfStopwatch();
        PerfStopwatch accWriteWatch = new PerfStopwatch();

        jdbcTemplate.query(CARDS_SQL, rs -> {
            rowWatch.start();
            String cardNum = rs.getString("Card_Number");
            Integer categoryId = getInt(rs, "Card_Category_ID");
            Integer legacyAccountId = getInt(rs, ACCOUNT_ID);

            Integer walletId = null;
            AccountsCardsRow accountsCardsRow = getAccountsCardsRow(cardNum, accountsCardsWatch);
            List<Integer> walletIds = getTransactionsCardWallets(cardNum, transactionsCardWatch);
            if (walletIds.size() == 1) {
                walletId = walletIds.get(0);
            } else if (categoryId != null && CATEGORY_TO_WALLET_MAP.containsKey(categoryId)) {
                walletId = CATEGORY_TO_WALLET_MAP.get(categoryId);
            } else if (accountsCardsRow.getAccountFundId() != null) {
                legacyAccountId = accountsCardsRow.getAccountFundId();
                walletId = 1;
            }
            if (walletId == null && walletIds.size() > 1) {
                log.warn("Card {} with category {} has many wallets: {}", cardNum, categoryId, walletIds);
            }
            Integer entityId = getEntityId(legacyAccountId, legacyToEntityMap);
            AccountType accountType = findAccountTypeBy(walletId, accountTypeMap);
            String accountNumber = makeAccountNumber(legacyAccountId, walletId);
            Integer accountId = findOrCreateAccount(accountType, entityId, accountNumber, accountNumberToIdMap, accWriteWatch);
            InitiatorCategory category = getVal(initiatorCategories, categoryId);
            InitiatorStatus status = getVal(initiatorStatuses, rs.getInt("Status_ID"));
            Initiator initiator = new Initiator()
                    .setAccountId(accountId)
                    .setInitiatorTypeId(cardType.getId())
                    .setIdentifier(cardNum)
                    .setInitiatorCategoryId(getCategoryId(category))
                    .setInitiatorStatusId(getStatusId(status))
                    .setPvv(accountsCardsRow.getPvv())
                    .setNotes(getString(rs, "Notes"))
                    .setCreatedDate(rs.getTimestamp(CREATED_DATE).toLocalDateTime());
            saveInitiator(initiator, cardWriteWatch);
            rowWatch.stop();
            if (counter.incrementAndGet() % 10000 == 0) {
                log.debug("  {} cards processed", counter.get());
                logAndStop("      ACselect:  {}", accountsCardsWatch);
                logAndStop("      TCselect:  {}", transactionsCardWatch);
                logAndStop("      accWrite:  {}", accWriteWatch);
                logAndStop("      cardWrite: {}", cardWriteWatch);
                logAndStop("      card row:  {}", rowWatch);
            }
        });
    }

    private void logAndStop(String message, PerfStopwatch stopwatch) {
        if (stopwatch.getCount() > 0) {
            log.debug(message, stopwatch.finishStopwatch());
        }
        stopwatch.clear();
    }

    private Map<String, Integer> selectAllAccountNumbers() {
        Map<String, Integer> accountNumberMap = new HashMap<>();
        newJdbcTemplate.query("select account_number, id from account", rs -> {
            accountNumberMap.put(rs.getString("account_number"), rs.getInt("id"));
        });
        log.debug("  collected {} accounts", accountNumberMap.size());
        return accountNumberMap;
    }

    private AccountsCardsRow getAccountsCardsRow(String cardNum, PerfStopwatch accountsCardsWatch) {
        AccountsCardsRow accountsCardsRow = new AccountsCardsRow();
        accountsCardsWatch.start();
        jdbcTemplate.query(String.format(ACCOUNTS_CARDS_SQL, cardNum), rs -> {
            accountsCardsRow.setPvv(getString(rs, "PVV"));
            accountsCardsRow.setAccountFundId(getInt(rs, "Account_Fund_ID"));
        });
        accountsCardsWatch.stop();
        return accountsCardsRow;
    }

    private List<Integer> getTransactionsCardWallets(String cardNum, PerfStopwatch transactionsCardWatch) {
        transactionsCardWatch.start();
        List<Integer> walletIds = jdbcTemplate.queryForList(String.format(TRANSACTIONS_CARD_SQL, cardNum), Integer.class);
        transactionsCardWatch.stop();
        return walletIds;
    }

    private void saveInitiator(Initiator initiator, PerfStopwatch cardWriteWatch) {
        cardWriteWatch.start();
        initiatorRepository.save(initiator);
        cardWriteWatch.stop();
    }

    private Integer getStatusId(InitiatorStatus status) {
        return status == null ? null : status.getId();
    }

    private Integer getCategoryId(InitiatorCategory category) {
        return category == null ? null : category.getId();
    }

    private Integer getEntityId(Integer legacyAccountId, Map<Integer, Integer> legacyToEntityMap) {
        return legacyToEntityMap.get(legacyAccountId);
    }

    private Integer findOrCreateAccount(AccountType accountType, Integer entityId, String accountNumber, Map<String, Integer> accountNumberToIdMap, PerfStopwatch accWriteWatch) {
        if (entityId == null || accountType == null) {
            return null;
        }
        return accountNumberToIdMap.computeIfAbsent(accountNumber, accountNumber1 -> {
            try {
                EntityClass entity = entityRepository.getById(entityId);
                Account account = new Account()
                        .setEntityId(entityId)
                        .setAccountTypeId(accountType.getId())
                        .setAccountNumber(accountNumber1)
                        .setAccountStatus(AccountStatus.of(entity.getStatus() == EntityStatus.ACTIVE))
                        .setCreatedDate(entity.getCreatedDate())
                        .setAutoDebit(false);
                accWriteWatch.start();
                accountRepository.save(account);
                accWriteWatch.stop();
                log.debug("  created account for entity: {}, accountNumber: {}, id: {}", entity.getId(), accountNumber, account.getId());
                return account.getId();
            } catch (Exception e) {
                log.warn("Exception: {}, accountNumber: {}, entity: {}, accountType: {}, accountNumber: {}", e.getMessage(), accountNumber, entityId, accountType, accountNumber1);
                throw e;
            }
        });
    }

    private String makeAccountNumber(Integer legacyAccountId, Integer walletId) {
        return legacyAccountId + String.format("%03d", (walletId == null ? 0 : walletId));
    }

    private AccountType findAccountTypeBy(Integer walletId, Map<Integer, AccountType> accountTypeMap) {
        if (walletId == null) {
            return null;
        }
        return accountTypeMap.computeIfAbsent(walletId, walletId1 ->
                accountTypeRepository.findByLegacyWalletId(String.valueOf(walletId1)).orElseThrow());
    }

    public void update(DataChange dataChange) {
        Initiator initiator = initiatorRepository.findByIdentifier(dataChange.getIdentifier()).orElse(null);
        if (dataChange.getAction() == ChangeAction.DELETE) {
            if (initiator != null) {
                initiatorRepository.delete(initiator);
            } else {
                log.warn("Cannot delete Initiator with identifier: {}, it is absent", dataChange.getIdentifier());
            }
        } else {                // update
            if (initiator == null) {
                initiator = new Initiator().setIdentifier(dataChange.getIdentifier());
            }
            fillInitiatorFields(initiator, dataChange.getData());
            initiatorRepository.save(initiator);
        }
    }

    private void fillInitiatorFields(Initiator initiator, Map<String, Object> data) {
        data.forEach((column, value) -> {
            switch (column) {
                case "Type" -> initiator.setInitiatorTypeId(initiatorTypeRepository.findByDescription((String) value).orElseThrow().getId());
                case "Category" -> initiator.setInitiatorCategoryId(initiatorCategoryRepository.findByCategory((String) value).orElseThrow().getId());
                case "Status" -> initiator.setInitiatorStatusId(initiatorStatusRepository.findByName((String) value).orElseThrow().getId());
                case "PVV" -> initiator.setPvv((String) value);
                case "Notes" -> initiator.setNotes((String) value);
                case CREATED_DATE -> initiator.setCreatedDate(dateTimeParser.parseDateTime((String) value));
                default -> {
                    if (!List.of(ACCOUNT_ID, WALLET_ID).contains(column)) {
                        log.warn("Unknown initiator field: '{}' has value: '{}'", column, value);
                    }
                }
            }
        });
        Account account = getOrCreateAccount((Integer) data.get(ACCOUNT_ID), (Integer) data.get(WALLET_ID));
        initiator.setAccountId(account == null ? null : account.getId());
    }

    private Account getOrCreateAccount(Integer legacyAccountId, Integer walletId) {
        Account account = null;
        if (legacyAccountId != null && walletId != null) {
            EntityClass entity = entityRepository.findByLegacyAccountId(legacyAccountId).orElse(null);
            AccountType accountType = accountTypeRepository.findByLegacyWalletId(String.valueOf(walletId)).orElse(null);
            if (entity != null && accountType != null) {
                account = accountRepository.findByEntityIdAndAccountTypeId(entity.getId(), accountType.getId()).orElse(null);
                if (account == null) {
                    account = new Account()
                            .setEntityId(entity.getId())
                            .setAccountTypeId(accountType.getId())
                            .setAccountNumber(makeAccountNumber(legacyAccountId, walletId))
                            .setAccountStatus(AccountStatus.of(entity.getStatus() == EntityStatus.ACTIVE))
                            .setCreatedDate(entity.getCreatedDate())
                            .setAutoDebit(false);
                    accountRepository.save(account);
                }
            }
        }
        return account;
    }

    @Data
    private static class AccountsCardsRow {
        private String pvv;
        private Integer accountFundId;
    }
}

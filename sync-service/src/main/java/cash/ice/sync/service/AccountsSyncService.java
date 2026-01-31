package cash.ice.sync.service;

import cash.ice.common.performance.PerfStopwatch;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.AccountRepository;
import cash.ice.sqldb.repository.AccountTypeRepository;
import cash.ice.sqldb.repository.CurrencyRepository;
import cash.ice.sqldb.repository.EntityRepository;
import cash.ice.sync.component.DateTimeParser;
import cash.ice.sync.dto.AccountChange;
import cash.ice.sync.dto.ChangeAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static cash.ice.sqldb.entity.AccountType.PRIMARY_ACCOUNT;
import static cash.ice.sync.task.Utils.getVal;
import static cash.ice.sync.task.Utils.getValNotNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountsSyncService implements DataMigrator {
    @SuppressWarnings("SqlResolve")
    private static final String ACCOUNTS_PROFILE_TRANSACTIONS_SQL = "select p.*, a.Daily_Limit, a.Active, a.Created_Date " +
            "from dbo.Accounts_Profile_Transactions p join dbo.Accounts a on p.Account_ID = a.Account_ID order by p.Accounts_Profile_Transactions_ID";
    private static final String WALLET_ID = "Wallet_ID";
    private static final String ACCOUNT_ID = "Account_ID";
    private static final String CREATED_DATE = "Created_Date";

    private final JdbcTemplate jdbcTemplate;
    private final EntityRepository entityRepository;
    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final CurrencyRepository currencyRepository;
    private final DateTimeParser dateTimeParser;

    @Transactional
    @Override
    public void migrateData() {
        log.debug("Start migrating Accounts");
        AtomicInteger counter = new AtomicInteger(0);
        PerfStopwatch rowWatch = new PerfStopwatch();
        PerfStopwatch accWriteWatch = new PerfStopwatch();

        Map<String, AccountType> accountTypes = accountTypeRepository.findAll().stream()
                .collect(Collectors.toMap(AccountType::getLegacyWalletId, at -> at));
        Map<Integer, EntityClass> entities = entityRepository.findAll().stream()
                .collect(Collectors.toMap(EntityClass::getLegacyAccountId, e -> e));
        Set<String> accountNumbers = new HashSet<>();

        log.debug("Start migrating accounts from Accounts_Profile_Transactions");
        jdbcTemplate.query(ACCOUNTS_PROFILE_TRANSACTIONS_SQL, rs -> {
            rowWatch.start();
            String accountNumber = rs.getString(ACCOUNT_ID) + String.format("%03d", rs.getInt(WALLET_ID));
            if (!accountNumbers.contains(accountNumber)) {
                AccountType accountType = getVal(accountTypes, rs.getString(WALLET_ID));
                if (accountType != null) {
                    Account account = new Account()
                            .setEntityId(getValNotNull(entities, rs.getInt(ACCOUNT_ID), "dbo.Accounts_Profile_Transactions.Account_ID").getId())
                            .setAccountTypeId(accountType.getId())
                            .setAccountNumber(accountNumber)
                            .setAccountStatus(AccountStatus.of(rs.getBoolean("Active")))
                            .setCreatedDate(rs.getTimestamp(CREATED_DATE).toLocalDateTime())
                            .setDailyLimit(rs.getBigDecimal("Daily_Limit"))
                            .setOverdraftLimit(rs.getBigDecimal("Overdraft"))
                            .setBalanceMinimum(rs.getBigDecimal("Balance_Minimum"))
                            .setBalanceWarning(rs.getBigDecimal("Balance_Warning"))
                            .setBalanceMinimumEnforce(rs.getBoolean("Balance_Minimum_Enforce"))
                            .setNotificationEnabled(rs.getBoolean("Send_Notification"))
                            .setAutoDebit(rs.getBoolean("Auto_Suck_Enabled"));
                    saveAccount(account, accWriteWatch);
                    accountNumbers.add(accountNumber);
                    rowWatch.stop();
                    if (counter.incrementAndGet() % 5000 == 0) {
                        log.debug("  {} accounts processed", counter.get());
                        logAndStop("      account write: {}", accWriteWatch);
                        logAndStop("      account row:   {}", rowWatch);
                    }
                } else {
                    log.warn("    Skipping account, Unknown Wallet_ID: " + rs.getString(WALLET_ID));
                }
            } else {
                log.warn("    Skipping duplicate accountNumber: {} for Account_ID: {}", accountNumber, rs.getInt(ACCOUNT_ID));
            }
        });
        createPrimaryAccounts(entities, accountNumbers, counter, accWriteWatch);
        log.info("Finished migrating Accounts: {} processed, {} total", counter.get(), accountRepository.count());
    }

    private void saveAccount(Account account, PerfStopwatch accWriteWatch) {
        accWriteWatch.start();
        accountRepository.save(account);
        accWriteWatch.stop();
    }

    private AccountType getPrimaryAccountType() {
        Currency zwlCurrency = currencyRepository.findByIsoCode("ZWL").orElseThrow();
        return accountTypeRepository.findByNameAndCurrencyId(PRIMARY_ACCOUNT, zwlCurrency.getId()).orElseThrow();
    }

    private void createPrimaryAccounts(Map<Integer, EntityClass> entities, Set<String> accountNumbers, AtomicInteger counter, PerfStopwatch accWriteWatch) {
        log.debug("Creating remaining Primary Accounts.");
        AccountType primaryAccountType = getPrimaryAccountType();
        String suffix = String.format("%03d", Integer.valueOf(primaryAccountType.getLegacyWalletId()));
        entities.values().stream()
                .filter(e -> !accountNumbers.contains(e.getLegacyAccountId() + suffix))
                .forEach(entity -> {
                    Account account = new Account()
                            .setEntityId(entity.getId())
                            .setAccountTypeId(primaryAccountType.getId())
                            .setAccountNumber(entity.getLegacyAccountId() +
                                    String.format("%03d", Integer.parseInt(primaryAccountType.getLegacyWalletId())))
                            .setAccountStatus(AccountStatus.of(entity.getStatus() == EntityStatus.ACTIVE))
                            .setCreatedDate(entity.getCreatedDate())
                            .setAutoDebit(false);
                    saveAccount(account, accWriteWatch);
                    accountNumbers.add(account.getAccountNumber());
                    if (counter.incrementAndGet() % 50000 == 0) {
                        log.debug("  {} accounts processing performed", counter.get());
                        logAndStop("      accWrite:  {}", accWriteWatch);
                    }
                });
        log.info("Finished creating Primary Account for every entity: {} processed", counter.get());
    }

    private void logAndStop(String message, PerfStopwatch stopwatch) {
        if (stopwatch.getCount() > 0) {
            log.debug(message, stopwatch.finishStopwatch());
        }
        stopwatch.clear();
    }

    public void update(AccountChange accountChange) {
        String accountNumber = accountChange.getLegacyAccountId() + String.format("%03d", accountChange.getLegacyWalletId());
        List<Account> accountsByNumber = accountRepository.findByAccountNumber(accountNumber);
        Account account = accountsByNumber.isEmpty() ? null : accountsByNumber.get(0);
        if (accountChange.getAction() == ChangeAction.DELETE) {
            if (account != null) {
                accountRepository.delete(account);
            } else {
                log.warn("Cannot delete Account with accountNumber: {}, it is absent", accountNumber);
            }
        } else {                // update
            if (account == null) {
                account = new Account().setAccountNumber(accountNumber)
                        .setEntityId(entityRepository.findByLegacyAccountId(accountChange.getLegacyAccountId()).map(EntityClass::getId).orElseThrow())
                        .setAccountTypeId(accountTypeRepository.findByLegacyWalletId(String.valueOf(accountChange.getLegacyWalletId())).orElseThrow().getId());
            }
            fillAccountFields(account, accountChange.getData());
            accountRepository.save(account);
        }
    }

    private void fillAccountFields(Account account, Map<String, Object> data) {
        data.forEach((column, value) -> {
            switch (column) {
                case "Active" -> account.setAccountStatus(AccountStatus.of((Boolean) value));
                case CREATED_DATE -> account.setCreatedDate(dateTimeParser.parseDateTime((String) value));
                case "Daily_Limit" -> account.setDailyLimit(new BigDecimal(String.valueOf(value)));
                case "Overdraft" -> account.setOverdraftLimit(new BigDecimal(String.valueOf(value)));
                case "Balance_Minimum" -> account.setBalanceMinimum(new BigDecimal(String.valueOf(value)));
                case "Balance_Warning" -> account.setBalanceWarning(new BigDecimal(String.valueOf(value)));
                case "Balance_Minimum_Enforce" -> account.setBalanceMinimumEnforce((Boolean) value);
                case "Send_Notification" -> account.setNotificationEnabled((Boolean) value);
                case "Auto_Suck_Enabled" -> account.setAutoDebit((Boolean) value);
                default -> log.warn("Unknown account field: '{}' has value: '{}'", column, value);
            }
        });
    }
}

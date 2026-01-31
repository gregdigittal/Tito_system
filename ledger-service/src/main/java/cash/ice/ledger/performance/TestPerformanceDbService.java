package cash.ice.ledger.performance;

import cash.ice.common.performance.IceDuration;
import cash.ice.common.performance.PerfStopwatch;
import cash.ice.common.utils.Tool;
import cash.ice.ledger.entity.PerformanceStatistics;
import cash.ice.ledger.repository.PerformanceStatisticsRepository;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
@Profile({"local-perf", "dev-k8s-perf", "dev-mysql-k8s-perf", "uat-k8s-perf"})
@AllArgsConstructor
@Slf4j
public class TestPerformanceDbService {
    private TransactionRepository transactionRepository;
    private TransactionLinesRepository transactionLinesRepository;
    private EntityRepository entityRepository;
    private EntityTypeRepository entityTypeRepository;
    private EntityTypeGroupRepository entityTypeGroupRepository;
    private InitiatorTypeRepository initiatorTypeRepository;
    private CurrencyRepository currencyRepository;
    private ChannelRepository channelRepository;
    private AccountTypeRepository accountTypeRepository;
    private AccountRepository accountRepository;
    private PerformanceStatisticsRepository performanceStatisticsRepository;

    public void fillDatabase(TestPerformanceInitRequest request) {
        log.info("Preparing mysql accounts: {}", request.getTotalRecords());
        int nThreads = getThreadsCount(request.getThreads());
        int capacity = getTasksQueueCapacity(request.getTasksQueueCapacity());
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        log.debug("Prepared executorService with {} threads, capacity: {}", nThreads, capacity);
        Map<Integer, Account> accountsMap = new ConcurrentHashMap<>();
        Instant startTime = Instant.now();
        long currentCount = transactionLinesRepository.count();
        EntityTypeGroup entityTypeGroup = entityTypeGroupRepository.findByDescription("Test entity type group")
                .orElseGet(() -> entityTypeGroupRepository.save(new EntityTypeGroup().setDescription("Test entity type group")));
        EntityType entityType = entityTypeRepository.findByDescription("Test entity type")
                .orElseGet(() -> entityTypeRepository.save(new EntityType().setDescription("Test entity type").setEntityTypeGroupId(entityTypeGroup.getId())));
        List<EntityClass> entities = entityRepository.findByFirstNameIn(List.of("perf test entity"));
        EntityClass entity = !entities.isEmpty() ? entities.getFirst() : entityRepository.save(new EntityClass()
                .setFirstName("perf test entity").setEntityTypeId(entityType.getId()).setStatus(EntityStatus.ACTIVE)
                .setLoginStatus(LoginStatus.ACTIVE).setCreatedDate(Tool.currentDateTime()));
        InitiatorType initiatorType = initiatorTypeRepository.findByDescription("test initiator type")
                .orElseGet(() -> initiatorTypeRepository.save(new InitiatorType().setActive(true).setDescription("test initiator type")));
        Currency currency = currencyRepository.findByIsoCode("TST")
                .orElseGet(() -> currencyRepository.save(new Currency().setActive(true).setIsoCode("TST").setPostilionCode(0)));
        Channel channel = channelRepository.findByCode("TCH")
                .orElseGet(() -> channelRepository.save(new Channel().setCode("TCH").setDescription("Test channel")));
        AccountType accountType = accountTypeRepository.findByNameAndCurrencyId("Perf Test account type", currency.getId())
                .orElseGet(() -> accountTypeRepository.save(new AccountType().setActive(true).setName("Perf Test account type").setDescription("Perf Test account type").setCurrencyId(currency.getId())));
        List<Transaction> transactions = transactionRepository.findBySessionIdIn(List.of("tst1"));
        Transaction transaction = !transactions.isEmpty() ? transactions.getFirst() : transactionRepository.save(new Transaction().setSessionId("tst1").setTransactionCodeId(1)
                .setChannelId(channel.getId()).setCurrencyId(currency.getId()).setInitiatorTypeId(initiatorType.getId())
                .setCreatedDate(Tool.currentDateTime()).setStatementDate(Tool.currentDateTime()));
        List<CompletableFuture<?>> submittedTasks = new ArrayList<>();
        int count = 0;
        for (int records = 10; records < request.getTotalRecords(); records *= 10) {
            if (request.getTotalRecords() / records > 1) {
                int accounts = Math.min((request.getTotalRecords() / records) - 1, 9);
                log.debug("Saving {} accounts with {} records", accounts, records);
                for (int rec = 1; rec <= records; rec++) {
                    for (int acc = 1; acc <= accounts; acc++, count++) {
                        if (count >= currentCount) {
                            int accountNumber = records + acc;
                            double amount = rec;
                            submittedTasks.add(CompletableFuture.runAsync(() ->
                                            createTransactionLine(transaction.getId(), entity, accountNumber, amount, accountType, accountsMap),
                                    executorService));
                            waitIfNeedUntilCompletion(submittedTasks, capacity, count);
                        }
                    }
                }
            }
        }
        saveLastRecordsTask(count, currentCount, request.getTotalRecords(), transaction.getId(), entity, accountType, accountsMap, executorService);
        shutdownExecutor(executorService);
        Duration duration = Duration.between(startTime, Instant.now());
        log.debug("Finished preparing transaction lines: {}, time: {}", transactionLinesRepository.count(), IceDuration.format(duration));
    }

    private void waitIfNeedUntilCompletion(List<CompletableFuture<?>> submittedTasks, int capacity, int count) {
        if (submittedTasks.size() >= capacity) {
            log.debug("Submitted {} tasks, waiting", count + 1);
            CompletableFuture.allOf(submittedTasks.toArray(new CompletableFuture[0])).join();
            submittedTasks.clear();
        }
    }

    private void saveLastRecordsTask(int count, long currentCount, int totalRecords, Integer transactionId, EntityClass entity, AccountType accountType, Map<Integer, Account> accountsMap, ExecutorService executorService) {
        if (count < totalRecords) {
            log.debug("Saving last {} records", (totalRecords - count));
            for (; count < totalRecords; count++) {
                if (count >= currentCount) {
                    executorService.submit(() ->
                            createTransactionLine(transactionId, entity, 1, 1, accountType, accountsMap));
                }
            }
        }
    }

    private void shutdownExecutor(ExecutorService executorService) {
        executorService.shutdown();
        try {
            log.debug("All tasks started, awaiting them finish");
            if (!executorService.awaitTermination(30, TimeUnit.DAYS)) {
                throw new IllegalStateException("Saving db data has not finished!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Saving db data has not finished, interrupted!", e);
        }
    }

    private int getThreadsCount(Integer requestedThreads) {
        return requestedThreads != null ? requestedThreads : Runtime.getRuntime().availableProcessors();
    }

    private int getTasksQueueCapacity(Integer requestedCapacity) {
        return requestedCapacity != null ? requestedCapacity : Integer.MAX_VALUE;
    }

    private void createTransactionLine(Integer transactionId, EntityClass entity, int accountNumber, double amount, AccountType accountType, Map<Integer, Account> accountsMap) {
        Account account = accountsMap.get(accountNumber);
        if (account == null) {
            synchronized (this) {
                account = accountsMap.get(accountNumber);
                if (account == null) {
                    List<Account> accounts = accountRepository.findByAccountNumber(String.valueOf(accountNumber));
                    account = accounts.isEmpty() ? null : accounts.get(0);
                    if (account == null) {
                        account = accountRepository.save(new Account().setEntityId(entity.getId()).setAccountTypeId(accountType.getId())
                                .setAccountNumber(String.valueOf(accountNumber)).setAccountStatus(AccountStatus.ACTIVE)
                                .setAuthorisationType(AuthorisationType.SINGLE).setCreatedDate(Tool.currentDateTime()));
                    }
                    accountsMap.put(accountNumber, account);
                }
            }
        }
        saveTransactionLine(transactionId, 1, account.getId(), amount);
    }

    public TransactionLines saveTransactionLine(int transactionId, int transactionCodeId, int accountId, double amount) {
        return transactionLinesRepository.save(new TransactionLines(transactionId, transactionCodeId, accountId,
                "Payment description", BigDecimal.valueOf(amount)));
    }

    @Transactional
    public void performLedgerService(int accountId, Integer transactionLines, Map<Integer, Transaction> createdTransactions, Map<Integer, TransactionLines> createdLines) {
        transactionLinesRepository.getBalance(accountId);
        Transaction transaction = transactionRepository.save(new Transaction().setTransactionCodeId(1).setSessionId("tst1")
                .setChannelId(2).setCurrencyId(1).setInitiatorTypeId(3).setCreatedDate(Tool.currentDateTime()).setStatementDate(Tool.currentDateTime()));
        createdTransactions.put(transaction.getId(), transaction);
        int lines = transactionLines != null ? transactionLines : 2;
        for (int i = 0; i < lines; i++) {
            TransactionLines line = saveTransactionLine(transaction.getId(), 1, accountId, 1);
            createdLines.put(line.getId(), line);
        }
    }

    @Transactional
    public void clearDatabase() {
        transactionLinesRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        channelRepository.findByCode("TCH").ifPresent(channel1 -> channelRepository.delete(channel1));
        currencyRepository.findByIsoCode("TST").ifPresent(currency1 -> currencyRepository.delete(currency1));
        initiatorTypeRepository.findByDescription("test initiator type").ifPresent(initiatorType1 -> initiatorTypeRepository.delete(initiatorType1));
        entityRepository.deleteAll(entityRepository.findByFirstNameIn(List.of("perf test entity")));
        entityTypeRepository.findByDescription("Test entity type").ifPresent(entityType1 -> entityTypeRepository.delete(entityType1));
        entityTypeGroupRepository.findByDescription("Test entity type group").ifPresent(entityTypeGroup1 -> entityTypeGroupRepository.delete(entityTypeGroup1));
        log.debug("Cleared database, lines: {}, accounts: {}", transactionLinesRepository.count(), accountRepository.count());
    }

    public void saveStatistics(TestPerformanceRequest request, PerfStopwatch stat) {
        log.debug("Saving performance mysql tests results to db");
        performanceStatisticsRepository.save(new PerformanceStatistics().setInfo(request.getLogic())
                .setTotalRecords((int) transactionLinesRepository.count())
                .setAccountRecords(request.getRecordsPerAccount()).setMin(bigDecimal(stat.getMin()))
                .setAvg(bigDecimal(stat.getAvg())).setMax(bigDecimal(stat.getMax()))
                .setTotalDuration(bigDecimal(stat.getTotal())).setRealDuration(bigDecimal(stat.getRealDuration()))
                .setParallelism(request.getThreads()).setCreatedDate(Tool.currentDateTime()));
    }

    private BigDecimal bigDecimal(Duration duration) {
        return new BigDecimal(String.format("%d.%09d", duration.getSeconds(), duration.getNano()));
    }
}

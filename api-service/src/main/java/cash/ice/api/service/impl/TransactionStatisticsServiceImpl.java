package cash.ice.api.service.impl;

import cash.ice.api.dto.moz.StatisticsTypeMoz;
import cash.ice.api.dto.moz.TransactionStatisticsData;
import cash.ice.api.dto.moz.TransactionStatisticsMoz;
import cash.ice.api.errors.MozRegistrationException;
import cash.ice.api.service.TransactionStatisticsService;
import cash.ice.common.dto.fee.FeeEntry;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.Currency;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static cash.ice.api.dto.moz.TransactionStatisticsData.Stat;
import static cash.ice.common.error.ErrorCodes.EC1061;
import static cash.ice.common.error.ErrorCodes.EC1062;
import static cash.ice.sqldb.entity.AccountType.*;
import static cash.ice.sqldb.entity.TransactionCode.TSF;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionStatisticsServiceImpl implements TransactionStatisticsService {
    private static final String ENTITY_ID = "entityId";

    private final AccountRepository accountRepository;
    private final EntityRepository entityRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final CurrencyRepository currencyRepository;
    private final TransactionCodeRepository transactionCodeRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionLinesRepository transactionLinesRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public void addPayment(FeesData feesData) {
        if (TSF.equals(feesData.getPaymentRequest().getTx())) {
            Map<Integer, TransactionStatisticsData> statisticsCache = new HashMap<>();
            Map<Integer, AccountType> accountTypes = gatherAccountTypes(feesData.getFeeEntries());
            Set<Integer> addedCache = new HashSet<>();
            LocalDate day = Tool.currentDateTime().toLocalDate();
            for (FeeEntry feeEntry : feesData.getFeeEntries()) {
                handleTransactionStatistics(feeEntry.getDrEntityId(), feeEntry.getDrAccountTypeId(), feeEntry.getDrEntityFirstName(), feeEntry.getDrEntityLastName(), feeEntry.getAmount().negate(), day,
                        !addedCache.contains(feeEntry.getDrAccountId()), feesData.getCurrencyCode(), accountTypes, statisticsCache);
                addedCache.add(feeEntry.getDrAccountId());
                handleTransactionStatistics(feeEntry.getCrEntityId(), feeEntry.getCrAccountTypeId(), feeEntry.getCrEntityFirstName(), feeEntry.getCrEntityLastName(), feeEntry.getAmount(), day,
                        !addedCache.contains(feeEntry.getCrAccountId()), feesData.getCurrencyCode(), accountTypes, statisticsCache);
                addedCache.add(feeEntry.getCrAccountId());
            }
            removeOldRecords(statisticsCache, LocalDate.now().minusDays(31));
            statisticsCache.values().forEach(mongoTemplate::save);
        }
    }

    private void removeOldRecords(Map<Integer, TransactionStatisticsData> statisticsCache, LocalDate minDate) {
        statisticsCache.values().forEach(transactionStatisticsData ->
                transactionStatisticsData.getTransactions().forEach((key, statMap) ->
                        statMap.entrySet().removeIf(e -> e.getKey().isBefore(minDate))));
    }

    private void handleTransactionStatistics(Integer entityId, Integer accountTypeId, String firstName, String lastName, BigDecimal amount, LocalDate day, boolean increaseCount, String currencyCode, Map<Integer, AccountType> accountTypes, Map<Integer, TransactionStatisticsData> statisticsCache) {
        TransactionStatisticsData transactionStatistics = statisticsCache.get(entityId);
        if (transactionStatistics == null) {
            Query query = query(where(ENTITY_ID).is(entityId));
            List<TransactionStatisticsData> transactionStatisticsList = mongoTemplate.find(query, TransactionStatisticsData.class);
            transactionStatistics = !transactionStatisticsList.isEmpty() ?
                    transactionStatisticsList.getLast() :
                    new TransactionStatisticsData();
            transactionStatistics.setEntityId(entityId);
            transactionStatistics.setEntityFirstName(firstName);
            transactionStatistics.setEntityLastName(lastName);
            statisticsCache.put(entityId, transactionStatistics);
        }
        AccountType accountType = accountTypes.get(accountTypeId);
        String accountKey = String.format("%s_%s", accountType.getName(), currencyCode);
        transactionStatistics.add(accountKey, day, amount, increaseCount);
    }

    private Map<Integer, AccountType> gatherAccountTypes(List<FeeEntry> feeEntries) {
        List<Integer> accountTypeIds = Stream.concat(
                feeEntries.stream().map(FeeEntry::getDrAccountTypeId),
                feeEntries.stream().map(FeeEntry::getCrAccountTypeId)).distinct().toList();
        return accountTypeRepository.findAllById(accountTypeIds).stream().collect(Collectors.toMap(AccountType::getId, at -> at));
    }

    @Override
    public List<TransactionStatisticsMoz> getTransactionStatistics(EntityClass entity, StatisticsTypeMoz statisticsType, int days) {
        Map<LocalDate, TransactionStatisticsMoz> statMap = new HashMap<>();
        LocalDateTime dateFrom = Tool.currentDateTime().minusDays(days);
        IntStream.range(1, days + 1).forEach(day -> statMap.computeIfAbsent(dateFrom.plusDays(day).toLocalDate(), date -> new TransactionStatisticsMoz().setDay(date)));

        Query query = query(where(ENTITY_ID).is(entity.getId()));
        List<TransactionStatisticsData> transactionStatisticsList = mongoTemplate.find(query, TransactionStatisticsData.class);
        TransactionStatisticsData transactionStatistics = !transactionStatisticsList.isEmpty() ? transactionStatisticsList.getLast() : null;
        if (transactionStatistics != null) {
            if (statisticsType == StatisticsTypeMoz.INCOME) {
                updateStatistics(statMap, transactionStatistics, PRIMARY_ACCOUNT, Currency.MZN,
                        (statistics, total) -> statistics.setIncome(statistics.getIncome().add(total)));
            } else {                        // EXPENSES
                updateStatistics(statMap, transactionStatistics, SUBSIDY_ACCOUNT, Currency.MZN,
                        (statistics, total) -> statistics.setSubsidyExpenses(statistics.getSubsidyExpenses().add(total)));
                updateStatistics(statMap, transactionStatistics, PREPAID_TRANSPORT, Currency.MZN,
                        (statistics, total) -> statistics.setPrepaidExpenses(statistics.getPrepaidExpenses().add(total)));
            }
        } else {
            log.debug("  no statistics data available");
        }
        return statMap.values().stream().sorted(Comparator.comparing(TransactionStatisticsMoz::getDay)).toList();
    }

    private void updateStatistics(Map<LocalDate, TransactionStatisticsMoz> statMap, TransactionStatisticsData transactionStatistics, String accountType, String currency, BiConsumer<TransactionStatisticsMoz, BigDecimal> incomeConsumer) {
        String accountKey = String.format("%s_%s", accountType, currency);
        Map<LocalDate, Stat> stats = transactionStatistics.getTransactions().get(accountKey);
        if (stats != null) {
            log.debug("  available {} {} transactions: {}", accountType, currency, stats.entrySet().stream().map(s -> s.getKey().toString() + ": " + s.getValue().getCount() + " " + s.getValue().getTotal()).collect(Collectors.joining(", ")));
            statMap.forEach((day, statistics) -> {
                Stat stat = stats.get(day);
                if (stat != null) {
                    statistics.setTrips(stat.getCount());
                    incomeConsumer.accept(statistics, stat.getTotal());
                }
            });
        } else {
            log.debug("  no statistics transactions for {} {} account", accountType, currency);
        }
    }

    @Override
    public int recalculateTransactionsStatistics() {
        LocalDateTime dateFrom = Tool.currentDateTime().minusDays(30);
        Currency mznCurrency = currencyRepository.findByIsoCode(Currency.MZN).orElseThrow(() ->
                new ICEcashException(String.format("'%s' currency does not exist", Currency.MZN), EC1062));
        TransactionCode transactionCode = transactionCodeRepository.getTransactionCodeByCode(TSF).orElseThrow(() ->
                new MozRegistrationException(EC1061, String.format("Transaction code '%s' does not exist", TSF), false));
        List<Transaction> transactions = transactionRepository.findByTransactionCodeIdAndCurrencyIdAndStatementDateAfter(
                transactionCode.getId(), mznCurrency.getId(), dateFrom);
        List<TransactionLines> lines = transactionLinesRepository.findByTransactionIdIn(
                transactions.stream().map(Transaction::getId).toList());

        Map<Integer, AccountType> accountTypes = accountTypeRepository.findAll().stream().collect(Collectors.toMap(AccountType::getId, at -> at));
        Map<Integer, Account> accounts = accountRepository.findAllById(lines.stream().map(TransactionLines::getEntityAccountId).distinct().toList())
                .stream().collect(Collectors.toMap(Account::getId, a -> a));
        Map<Integer, EntityClass> entities = entityRepository.findAllById(accounts
                        .values().stream().map(Account::getEntityId).filter(Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(EntityClass::getId, e -> e));
        mongoTemplate.remove(new Query(), TransactionStatisticsData.class);

        Map<Integer, TransactionStatisticsData> statisticsCache = new HashMap<>();
        Set<String> addedCache = new HashSet<>();
        for (Transaction transaction : transactions) {
            List<TransactionLines> trLines = lines.stream().filter(tl -> Objects.equals(tl.getTransactionId(), transaction.getId())).toList();
            for (TransactionLines line : trLines) {
                Account account = accounts.get(line.getEntityAccountId());
                EntityClass entity = entities.get(account.getEntityId());
                handleTransactionStatistics(account.getEntityId(), account.getAccountTypeId(), entity.getFirstName(), entity.getLastName(), line.getAmount(), transaction.getStatementDate().toLocalDate(),
                        !addedCache.contains(transaction.getId() + "_" + account.getId()), mznCurrency.getIsoCode(), accountTypes, statisticsCache);
                addedCache.add(transaction.getId() + "_" + account.getId());
            }
        }
        statisticsCache.values().forEach(mongoTemplate::save);
        return transactions.size();
    }
}

package cash.ice.api.service.impl;

import cash.ice.api.dto.moz.StatisticsTypeMoz;
import cash.ice.api.dto.moz.TransactionStatisticsData;
import cash.ice.api.dto.moz.TransactionStatisticsMoz;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.fee.FeeEntry;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.AccountType;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static cash.ice.sqldb.entity.Currency.MZN;
import static cash.ice.sqldb.entity.TransactionCode.TSF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@ExtendWith(MockitoExtension.class)
class TransactionStatisticsServiceImplTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountTypeRepository accountTypeRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private TransactionCodeRepository transactionCodeRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionLinesRepository transactionLinesRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @Captor
    private ArgumentCaptor<TransactionStatisticsData> statisticsDataCaptor;
    @InjectMocks
    private TransactionStatisticsServiceImpl service;

    @Test
    void testAddPayment() {
        FeesData feesData = new FeesData().setCurrencyCode(MZN)
                .setPaymentRequest(new PaymentRequest().setTx(TSF)).setFeeEntries(List.of(
                        new FeeEntry().setAmount(new BigDecimal("8.0")).setCrEntityFirstName("first1").setCrEntityLastName("last1")
                                .setDrEntityFirstName("first2").setDrEntityLastName("last2")
                                .setCrAccountId(10).setCrAccountTypeId(1).setCrEntityId(2)
                                .setDrAccountId(11).setDrAccountTypeId(2).setDrEntityId(3),
                        new FeeEntry().setAmount(new BigDecimal("2.0")).setCrEntityFirstName("first1").setCrEntityLastName("last1")
                                .setDrEntityFirstName("first2").setDrEntityLastName("last2")
                                .setCrAccountId(10).setCrAccountTypeId(1).setCrEntityId(2)
                                .setDrAccountId(12).setDrAccountTypeId(3).setDrEntityId(3)));
        LocalDate currentDate = Tool.currentDateTime().toLocalDate();

        when(accountTypeRepository.findAllById(List.of(2, 3, 1))).thenReturn(List.of(
                new AccountType().setId(1).setName("Primary"),
                new AccountType().setId(2).setName("Prepaid"),
                new AccountType().setId(3).setName("Subsidy")));

        service.addPayment(feesData);
        verify(mongoTemplate, times(2)).save(statisticsDataCaptor.capture());
        List<TransactionStatisticsData> savedData = statisticsDataCaptor.getAllValues();
        assertThat(savedData.getFirst().getEntityId()).isEqualTo(2);
        assertThat(savedData.getFirst().getEntityFirstName()).isEqualTo("first1");
        assertThat(savedData.get(0).getEntityLastName()).isEqualTo("last1");
        assertThat(savedData.get(0).getTransactions()).isEqualTo(Map.of(
                "Primary_MZN", Map.of(currentDate, new TransactionStatisticsData.Stat().setCount(1).setTotal(new BigDecimal("10.0")))));
        assertThat(savedData.get(1).getEntityId()).isEqualTo(3);
        assertThat(savedData.get(1).getEntityFirstName()).isEqualTo("first2");
        assertThat(savedData.get(1).getEntityLastName()).isEqualTo("last2");
        assertThat(savedData.get(1).getTransactions()).isEqualTo(Map.of(
                "Prepaid_MZN", Map.of(currentDate, new TransactionStatisticsData.Stat().setCount(1).setTotal(new BigDecimal("-8.0"))),
                "Subsidy_MZN", Map.of(currentDate, new TransactionStatisticsData.Stat().setCount(1).setTotal(new BigDecimal("-2.0")))));
    }

    @Test
    void getTransactionStatisticsIncome() {
        LocalDate currentDate = Tool.currentDateTime().toLocalDate();
        when(mongoTemplate.find(query(where("entityId").is(1)), TransactionStatisticsData.class)).thenReturn(List.of(createTransactionStatisticsData(currentDate)));

        List<TransactionStatisticsMoz> actualResult = service.getTransactionStatistics(new EntityClass().setId(1), StatisticsTypeMoz.INCOME, 7);
        assertThat(actualResult.size()).isEqualTo(7);
        assertThat(actualResult.get(0)).isEqualTo(new TransactionStatisticsMoz().setDay(currentDate.minusDays(6)).setTrips(1).setIncome(new BigDecimal("10.0")));
        assertThat(actualResult.get(1)).isEqualTo(new TransactionStatisticsMoz().setDay(currentDate.minusDays(5)).setTrips(2).setIncome(new BigDecimal("20.0")));
        assertThat(actualResult.get(2)).isEqualTo(new TransactionStatisticsMoz().setDay(currentDate.minusDays(4)).setTrips(0).setIncome(BigDecimal.ZERO));
        assertThat(actualResult.get(3)).isEqualTo(new TransactionStatisticsMoz().setDay(currentDate.minusDays(3)).setTrips(0).setIncome(BigDecimal.ZERO));
        assertThat(actualResult.get(4)).isEqualTo(new TransactionStatisticsMoz().setDay(currentDate.minusDays(2)).setTrips(0).setIncome(BigDecimal.ZERO));
        assertThat(actualResult.get(5)).isEqualTo(new TransactionStatisticsMoz().setDay(currentDate.minusDays(1)).setTrips(0).setIncome(BigDecimal.ZERO));
        assertThat(actualResult.get(6)).isEqualTo(new TransactionStatisticsMoz().setDay(currentDate).setTrips(0).setIncome(BigDecimal.ZERO));
    }

    @Test
    void getTransactionStatisticsExpenses() {
        LocalDate currentDate = Tool.currentDateTime().toLocalDate();
        when(mongoTemplate.find(query(where("entityId").is(1)), TransactionStatisticsData.class)).thenReturn(List.of(createTransactionStatisticsData(currentDate)));

        List<TransactionStatisticsMoz> actualResult = service.getTransactionStatistics(new EntityClass().setId(1), StatisticsTypeMoz.EXPENSES, 7);
        assertThat(actualResult.size()).isEqualTo(7);
        assertThat(actualResult.get(0)).isEqualTo(new TransactionStatisticsMoz().setDay(currentDate.minusDays(6)).setTrips(0).setIncome(BigDecimal.ZERO));
        assertThat(actualResult.get(1)).isEqualTo(new TransactionStatisticsMoz().setDay(currentDate.minusDays(5)).setTrips(0).setIncome(BigDecimal.ZERO));
        assertThat(actualResult.get(2)).isEqualTo(new TransactionStatisticsMoz().setDay(currentDate.minusDays(4))
                .setTrips(2).setPrepaidExpenses(new BigDecimal("-16.0")).setSubsidyExpenses(new BigDecimal("-4.0")));
        assertThat(actualResult.get(3)).isEqualTo(new TransactionStatisticsMoz().setDay(currentDate.minusDays(3))
                .setTrips(1).setPrepaidExpenses(new BigDecimal("-8.0")).setSubsidyExpenses(new BigDecimal("-2.0")));
        assertThat(actualResult.get(4)).isEqualTo(new TransactionStatisticsMoz().setDay(currentDate.minusDays(2)).setTrips(0).setIncome(BigDecimal.ZERO));
        assertThat(actualResult.get(5)).isEqualTo(new TransactionStatisticsMoz().setDay(currentDate.minusDays(1)).setTrips(0).setIncome(BigDecimal.ZERO));
        assertThat(actualResult.get(6)).isEqualTo(new TransactionStatisticsMoz().setDay(currentDate).setTrips(0).setIncome(BigDecimal.ZERO));
    }

    private TransactionStatisticsData createTransactionStatisticsData(LocalDate currentDate) {
        return new TransactionStatisticsData().setTransactions(Map.of(
                "Primary_MZN", Map.of(
                        currentDate.minusDays(6), new TransactionStatisticsData.Stat().setCount(1).setTotal(new BigDecimal("10.0")),
                        currentDate.minusDays(5), new TransactionStatisticsData.Stat().setCount(2).setTotal(new BigDecimal("20.0"))),
                "Prepaid_MZN", Map.of(
                        currentDate.minusDays(4), new TransactionStatisticsData.Stat().setCount(2).setTotal(new BigDecimal("-16.0")),
                        currentDate.minusDays(3), new TransactionStatisticsData.Stat().setCount(1).setTotal(new BigDecimal("-8.0"))),
                "Subsidy_MZN", Map.of(
                        currentDate.minusDays(4), new TransactionStatisticsData.Stat().setCount(2).setTotal(new BigDecimal("-4.0")),
                        currentDate.minusDays(3), new TransactionStatisticsData.Stat().setCount(1).setTotal(new BigDecimal("-2.0"))))
        );
    }
}
package cash.ice.ledger.service.impl;

import cash.ice.common.dto.EmailRequest;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.fee.FeeEntry;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.AccountBalance;
import cash.ice.sqldb.entity.AccountType;
import cash.ice.common.error.ICEcashWrongBalanceException;
import cash.ice.sqldb.repository.AccountBalanceRepository;
import cash.ice.sqldb.repository.AccountTypeRepository;
import cash.ice.sqldb.repository.TransactionLinesRepository;
import cash.ice.common.service.KafkaSender;
import cash.ice.ledger.config.BalanceWarningEmailProperties;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static cash.ice.common.error.ErrorCodes.EC4002;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountBalanceServiceImplTest {
    private static final int ACCOUNT_ID = 1;

    @Mock
    private AccountBalanceRepository accountBalanceRepository;
    @Mock
    private TransactionLinesRepository transactionLinesRepository;
    @Mock
    private AccountTypeRepository accountTypeRepository;
    @Mock
    private KafkaSender kafkaSender;
    @Captor
    private ArgumentCaptor<AccountBalance> accountBalanceCaptor;

    private BalanceWarningEmailProperties balanceWarningEmailProperties;
    private AccountBalanceServiceImpl service;

    @BeforeEach
    void init() {
        balanceWarningEmailProperties = new BalanceWarningEmailProperties();
        service = new AccountBalanceServiceImpl(accountBalanceRepository, transactionLinesRepository,
                accountTypeRepository, balanceWarningEmailProperties, kafkaSender);
    }

    @Test
    void testCanCharge() {
        assertTrue(service.canCharge(new BigDecimal("10"), new BigDecimal("5"), BigDecimal.ZERO));
        assertTrue(service.canCharge(new BigDecimal("10"), new BigDecimal("10.00"), BigDecimal.ZERO));
        assertFalse(service.canCharge(new BigDecimal("10"), new BigDecimal("10.01"), BigDecimal.ZERO));
        assertTrue(service.canCharge(new BigDecimal("10"), new BigDecimal("5"), new BigDecimal("5")));
        assertFalse(service.canCharge(new BigDecimal("10"), new BigDecimal("5.01"), new BigDecimal("5")));
        assertTrue(service.canCharge(new BigDecimal("10"), new BigDecimal("15"), new BigDecimal("-5")));
        assertFalse(service.canCharge(new BigDecimal("10"), new BigDecimal("15.01"), new BigDecimal("-5")));
    }

    @Test
    void testAccountBalanceAffordability() {
        when(accountBalanceRepository.findByAccountId(1)).thenReturn(Optional.of(new AccountBalance()
                .setBalance(new BigDecimal("20"))));
        service.checkAccountBalanceAffordability(createAffordabilityRequestData());
    }

    @Test
    void testAccountBalanceAffordabilityWithOverdraft() {
        FeesData request = createAffordabilityRequestData();
        request.getFeeEntries().get(0).setDrAccountOverdraftLimit(new BigDecimal("-20"));
        request.getFeeEntries().get(1).setDrAccountOverdraftLimit(new BigDecimal("-20"));

        when(accountBalanceRepository.findByAccountId(1)).thenReturn(Optional.of(new AccountBalance()
                .setBalance(new BigDecimal("1"))));
        service.checkAccountBalanceAffordability(request);
    }

    @Test
    void testAccountBalanceAffordabilityFail() {
        when(accountBalanceRepository.findByAccountId(1)).thenReturn(Optional.of(new AccountBalance()
                .setBalance(new BigDecimal("10"))));
        ICEcashWrongBalanceException actualException = assertThrows(ICEcashWrongBalanceException.class,
                () -> service.checkAccountBalanceAffordability(createAffordabilityRequestData()));
        AssertionsForClassTypes.assertThat(actualException.getErrorCode()).isEqualTo(EC4002);
    }

    @Test
    void testAccountBalanceAffordabilityFailWithOverdraft() {
        FeesData request = createAffordabilityRequestData();
        request.getFeeEntries().get(0).setDrAccountOverdraftLimit(new BigDecimal("-10"));
        request.getFeeEntries().get(1).setDrAccountOverdraftLimit(new BigDecimal("-10"));

        when(accountBalanceRepository.findByAccountId(1)).thenReturn(Optional.of(new AccountBalance()
                .setBalance(new BigDecimal("1"))));
        ICEcashWrongBalanceException actualException = assertThrows(ICEcashWrongBalanceException.class,
                () -> service.checkAccountBalanceAffordability(request));
        AssertionsForClassTypes.assertThat(actualException.getErrorCode()).isEqualTo(EC4002);
    }

    @Test
    void testAccountBalanceAffordabilityFailWithLimit() {
        FeesData request = createAffordabilityRequestData();
        request.getFeeEntries().get(0).setDrAccountBalanceMinimum(new BigDecimal("10"));
        request.getFeeEntries().get(1).setDrAccountBalanceMinimum(new BigDecimal("10"));

        when(accountBalanceRepository.findByAccountId(1)).thenReturn(Optional.of(new AccountBalance()
                .setBalance(new BigDecimal("20"))));
        ICEcashWrongBalanceException actualException = assertThrows(ICEcashWrongBalanceException.class,
                () -> service.checkAccountBalanceAffordability(request));
        AssertionsForClassTypes.assertThat(actualException.getErrorCode()).isEqualTo(EC4002);
    }

    private FeesData createAffordabilityRequestData() {
        return new FeesData().setPaymentRequest(new PaymentRequest()).setFeeEntries(List.of(
                new FeeEntry().setAmount(new BigDecimal("10")).setAffordabilityCheck(false).setDrAccountId(1),
                new FeeEntry().setAmount(new BigDecimal("5")).setAffordabilityCheck(true).setDrAccountId(1),
                new FeeEntry().setAmount(new BigDecimal("10")).setAffordabilityCheck(false).setDrAccountId(2)
        ));
    }

    @Test
    void testFindOrCalculateAccountBalance() {
        BigDecimal balance = new BigDecimal("100.0");

        when(accountBalanceRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());
        when(transactionLinesRepository.getBalance(ACCOUNT_ID)).thenReturn(balance);

        AccountBalance accountBalance = service.findOrCalculateAccountBalance(ACCOUNT_ID);
        verify(accountBalanceRepository).save(accountBalanceCaptor.capture());
        AccountBalance actualAccountBalance = accountBalanceCaptor.getValue();
        assertThat(actualAccountBalance.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(actualAccountBalance.getBalance()).isEqualTo(balance);
        assertThat(accountBalance).isEqualTo(actualAccountBalance);
    }

    @Test
    void testUpdateAccountBalances() {
        service.setBalanceAuditAsynchronous(true);
        AccountBalance accountBalance1 = new AccountBalance().setBalance(new BigDecimal("10.0"));
        AccountBalance accountBalance2 = new AccountBalance().setBalance(new BigDecimal("100.0")).setVersion(3);
        when(accountBalanceRepository.findByAccountId(1)).thenReturn(Optional.of(accountBalance1));
        when(accountBalanceRepository.findByAccountId(2)).thenReturn(Optional.of(accountBalance2));
        when(accountBalanceRepository.save(accountBalance1)).thenReturn(accountBalance1);
        when(accountBalanceRepository.save(accountBalance2)).thenReturn(accountBalance2);

        when(accountTypeRepository.getById(11)).thenReturn(new AccountType()
                .setAuditTransactionValue(new BigDecimal("1.0")));
        when(accountTypeRepository.getById(12)).thenReturn(new AccountType()
                .setAuditTransactionInterval(3));

        Map<Account, BigDecimal> accountBalanceChangeMap = new LinkedHashMap<>();
        accountBalanceChangeMap.put(new Account().setId(1).setAccountTypeId(11), new BigDecimal("2.0"));
        accountBalanceChangeMap.put(new Account().setId(2).setAccountTypeId(12), new BigDecimal("-2.0"));
        service.updateAccountBalances(accountBalanceChangeMap);

        verify(accountBalanceRepository, times(2)).save(accountBalanceCaptor.capture());
        List<AccountBalance> actualAccountBalances = accountBalanceCaptor.getAllValues();
        assertThat(actualAccountBalances.get(0).getBalance()).isEqualTo(new BigDecimal("12.0"));
        assertThat(actualAccountBalances.get(1).getBalance()).isEqualTo(new BigDecimal("98.0"));

        verify(kafkaSender).sendBalanceAudit(1);
        verify(kafkaSender).sendBalanceAudit(2);
    }

    @Test
    void testCheckBalanceCorrectness() {
        balanceWarningEmailProperties.setFrom("from@test.com");
        balanceWarningEmailProperties.setSubject("TestSubject");
        balanceWarningEmailProperties.setBody("Message: account=%s, correctBalance=%s, actualBalance=%s");
        balanceWarningEmailProperties.setRecipients("User <user@test.com>,user2@test.com");

        BigDecimal correctBalance = new BigDecimal("10.0");
        when(transactionLinesRepository.getBalance(ACCOUNT_ID)).thenReturn(correctBalance);
        when(accountBalanceRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(
                new AccountBalance().setBalance(new BigDecimal("10000.0"))));

        service.checkBalanceCorrectness(ACCOUNT_ID);
        verify(accountBalanceRepository).save(accountBalanceCaptor.capture());
        assertThat(accountBalanceCaptor.getValue().getBalance()).isEqualTo(correctBalance);
        verify(kafkaSender).sendEmailNotification(new EmailRequest()
                .setFrom("from@test.com")
                .setSubject("TestSubject")
                .setMessageBody("Message: account=1, correctBalance=10.0, actualBalance=10000.0")
                .setRecipients(List.of(
                        new EmailRequest.Recipient().setName("User").setEmailAddress("user@test.com"),
                        new EmailRequest.Recipient().setEmailAddress("user2@test.com"))));
    }
}
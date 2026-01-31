package cash.ice.ledger.service.impl;

import cash.ice.common.constant.PaymentMetaKey;
import cash.ice.common.dto.EmailRequest;
import cash.ice.common.dto.fee.FeeEntry;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.error.ICEcashWrongBalanceException;
import cash.ice.common.service.KafkaSender;
import cash.ice.ledger.config.BalanceWarningEmailProperties;
import cash.ice.ledger.service.AccountBalanceService;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.AccountBalance;
import cash.ice.sqldb.entity.AccountType;
import cash.ice.sqldb.repository.AccountBalanceRepository;
import cash.ice.sqldb.repository.AccountTypeRepository;
import cash.ice.sqldb.repository.TransactionLinesRepository;
import cash.ice.sqldb.util.DbUtil;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cash.ice.common.error.ErrorCodes.EC4002;

@Service
@Slf4j
@RequiredArgsConstructor
@Setter
public class AccountBalanceServiceImpl implements AccountBalanceService {
    private final AccountBalanceRepository accountBalanceRepository;
    private final TransactionLinesRepository transactionLinesRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final BalanceWarningEmailProperties balanceWarningEmailProperties;
    private final KafkaSender kafkaSender;

    @Value("${ice.cash.ledger.balance-audit-asynchronous}")
    private boolean balanceAuditAsynchronous;

    @Override
    public void checkAccountBalanceAffordability(FeesData feesData) {
        if (feesData.getPaymentRequest().getMeta() != null && feesData.getPaymentRequest().getMeta().containsKey(PaymentMetaKey.OffloadTransaction)) {
            return;
        }
        Set<Integer> checkingAccountIds = feesData.getFeeEntries().stream()
                .filter(FeeEntry::isAffordabilityCheck)
                .map(FeeEntry::getDrAccountId)
                .collect(Collectors.toSet());
        if (!checkingAccountIds.isEmpty()) {
            Map<Account, BigDecimal> accountToChargingAmountMap = feesData.getFeeEntries().stream()
                    .filter(entry -> checkingAccountIds.contains(entry.getDrAccountId()))
                    .collect(Collectors.groupingBy(f -> new Account().setId(f.getDrAccountId())
                                    .setBalanceMinimum(f.getDrAccountBalanceMinimum())
                                    .setOverdraftLimit(f.getDrAccountOverdraftLimit()),
                            Collectors.reducing(BigDecimal.ZERO, FeeEntry::getAmount, BigDecimal::add)));
            accountToChargingAmountMap.forEach(this::checkAccountAffordability);
        }
    }

    private void checkAccountAffordability(Account account, BigDecimal chargingAmount) {
        AccountBalance accountBalance = findOrCalculateAccountBalance(account.getId());
        BigDecimal minimumBalance = getBalanceMinimum(account);
        if (!canCharge(accountBalance.getBalance(), chargingAmount, minimumBalance)) {
            throw new ICEcashWrongBalanceException(String.format("Insufficient balance for the transaction. Account: %s, balance: %s, charging: %s, limit: %s",
                    account.getId(), accountBalance.getBalance(), chargingAmount, minimumBalance), EC4002);
        }
    }

    boolean canCharge(BigDecimal currentBalance, BigDecimal chargingAmount, BigDecimal minimumBalance) {
        return currentBalance.subtract(chargingAmount).compareTo(minimumBalance) >= 0;
    }

    private BigDecimal getBalanceMinimum(Account account) {
        if (account.getBalanceMinimum() != null) {
            return account.getBalanceMinimum();
        } else if (account.getOverdraftLimit() != null) {
            return account.getOverdraftLimit();
        } else {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public AccountBalance findOrCalculateAccountBalance(int accountId) {
        AccountBalance accountBalance = accountBalanceRepository.findByAccountId(accountId).orElse(null);
        if (accountBalance == null) {
            accountBalance = createAndCalculateAccountBalance(accountId);
            accountBalanceRepository.save(accountBalance);
        }
        return accountBalance;
    }

    @Override
    public void updateAccountBalances(Map<Account, BigDecimal> accountBalanceChangeMap) {
        accountBalanceChangeMap.forEach((account, balanceChange) -> {
            AccountBalance accountBalance = updateAccountBalanceFor(account, balanceChange);
            performBalanceAuditIfNeed(accountBalance, balanceChange, account);
        });
    }

    private AccountBalance updateAccountBalanceFor(Account account, BigDecimal balanceChange) {
        return DbUtil.optimisticLockProtection(() -> {
            AccountBalance accountBalance = accountBalanceRepository.findByAccountId(account.getId()).orElse(null);
            if (accountBalance != null) {
                if (accountBalance.getBalance() == null) {
                    accountBalance.setBalance(new BigDecimal("0.00"));
                }
                accountBalance.setBalance(accountBalance.getBalance().add(balanceChange));
            } else {
                accountBalance = createAndCalculateAccountBalance(account.getId());
            }
            log.debug("Updating {}", accountBalance);
            return accountBalanceRepository.save(accountBalance);
        }, account, log);
    }

    private void performBalanceAuditIfNeed(AccountBalance accountBalance, BigDecimal balanceChange, Account account) {
        AccountType accountType = accountTypeRepository.getById(account.getAccountTypeId());
        log.debug("  performBalanceAuditIfNeed: change: {}, version: {}, account {}, type: {}, ", balanceChange, accountBalance.getVersion(), account, accountType);
        BigDecimal auditValue = accountType.getAuditTransactionValue();
        Integer auditInterval = accountType.getAuditTransactionInterval();
        if (auditValue != null && balanceChange.compareTo(auditValue) > 0
                || auditInterval != null && accountBalance.getVersion() % auditInterval == 0) {

            if (balanceAuditAsynchronous) {
                log.debug("  Sending account balance for audit message. Account: {}, change: {}, auditValue: {}, version: {}, auditInterval: {}",
                        account.getId(), balanceChange, auditValue, accountBalance.getVersion(), auditInterval);
                kafkaSender.sendBalanceAudit(account.getId());
            } else {
                checkBalanceCorrectness(account.getId());
            }
        }
    }

    private AccountBalance createAndCalculateAccountBalance(int accountId) {
        BigDecimal balance = transactionLinesRepository.getBalance(accountId);
        return new AccountBalance()
                .setAccountId(accountId)
                .setBalance(balance != null ? balance : BigDecimal.ZERO);
    }

    @Override
    public void checkBalanceCorrectness(Integer accountId) {
        log.debug("  checking balance correctness for account: {}", accountId);
        DbUtil.optimisticLockProtection(() -> {
            BigDecimal correctBalance = transactionLinesRepository.getBalance(accountId);
            AccountBalance accountBalance = accountBalanceRepository.findByAccountId(accountId).orElseThrow();
            BigDecimal actualBalance = accountBalance.getBalance();
            if (!actualBalance.equals(correctBalance)) {
                accountBalance.setBalance(correctBalance);
                log.debug("Restoring accountBalance: {}", accountBalance);
                accountBalanceRepository.save(accountBalance);
                signalIncorrectBalance(accountId, correctBalance, actualBalance);
            }
        }, log, accountId);
    }

    private void signalIncorrectBalance(Integer accountId, BigDecimal correctBalance, BigDecimal actualBalance) {
        log.warn("Wrong balance for account: {}, expected: {}, actual: {}", accountId, correctBalance, actualBalance);
        EmailRequest emailRequest = balanceWarningEmailProperties.createEmailRequest(accountId, correctBalance, actualBalance);
        if (!emailRequest.getRecipients().isEmpty()) {
            kafkaSender.sendEmailNotification(emailRequest);
        } else {
            log.warn("  recipients list is empty: " + balanceWarningEmailProperties);
        }
    }
}

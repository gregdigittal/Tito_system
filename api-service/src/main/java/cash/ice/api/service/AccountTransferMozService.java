package cash.ice.api.service;

import cash.ice.api.config.property.MozProperties;
import cash.ice.api.errors.MozRegistrationException;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static cash.ice.common.error.ErrorCodes.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountTransferMozService {
    private static final int CHANNEL_ID = 2;         //API
    private static final String DEP = "DEP";
    private static final String CASH = "cash";
    private static final String TRANSACTION_DESCR = "Test deposit";
    private static final BigDecimal PRIMARY_CHARGE_AMOUNT = new BigDecimal("100000.0");
    private static final BigDecimal SUBSIDY_CHARGE_AMOUNT = new BigDecimal("885.0");

    private final EntityRepository entityRepository;
    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final CurrencyRepository currencyRepository;
    private final InitiatorTypeRepository initiatorTypeRepository;
    private final TransactionCodeRepository transactionCodeRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionLinesRepository transactionLinesRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final MozProperties mozProperties;

    @Deprecated
    public void topUpAccounts(Account primaryAccount, Account subsidyAccount, Currency currency) {
        TransactionCode transactionCode = transactionCodeRepository.getTransactionCodeByCode(DEP).orElseThrow(() ->
                new MozRegistrationException(EC1061, String.format("Transaction code '%s' does not exist", DEP), false));
        InitiatorType initiatorType = initiatorTypeRepository.findByDescription(CASH).orElseThrow(() ->
                new MozRegistrationException(EC1057, String.format("Initiator type '%s' does not exist", CASH), false));
        Account suspenseAccount = accountRepository.findByAccountNumber(mozProperties.getSuspenseAccountNumber()).stream().findFirst().orElseThrow(() ->
                new MozRegistrationException(EC1022, String.format("Suspense account %s does not exist", mozProperties.getSuspenseAccountNumber()), false));
        transferMoney(suspenseAccount, primaryAccount, PRIMARY_CHARGE_AMOUNT, currency.getId(), initiatorType.getId(), transactionCode.getId(), TRANSACTION_DESCR);
        Account subsidyPoolAccount = accountRepository.findByAccountNumber(mozProperties.getSubsidyPoolAccountNumber()).stream().findFirst().orElseThrow(() ->
                new MozRegistrationException(EC1022, String.format("Subsidy pool account %s does not exist", mozProperties.getSubsidyPoolAccountNumber()), false));
        transferMoney(subsidyPoolAccount, subsidyAccount, SUBSIDY_CHARGE_AMOUNT, currency.getId(), initiatorType.getId(), transactionCode.getId(), TRANSACTION_DESCR);
    }

    private void transferMoney(Account drAccount, Account crAccount, BigDecimal amount, int currencyId, int initiatorTypeId, int transactionCodeId, String description) {
        EntityClass drAccountEntity = entityRepository.findById(drAccount.getEntityId())
                .orElseThrow(() -> new ICEcashException("Entity with id=" + drAccount.getEntityId() + " does not exist", EC1048));
        EntityClass crAccountEntity = entityRepository.findById(crAccount.getEntityId())
                .orElseThrow(() -> new ICEcashException("Entity with id=" + crAccount.getEntityId() + " does not exist", EC1048));

        log.debug("Transfer money fromAccount: {} toAccount: {} amount: {}, currencyId: {}, transactionCodeId: {}, description: {}",
                drAccount.getId(), crAccount.getId(), amount, currencyId, transactionCodeId, description);
        Transaction transaction = transactionRepository.save(new Transaction().setTransactionCodeId(transactionCodeId)
                .setSessionId(UUID.randomUUID().toString())
                .setChannelId(CHANNEL_ID)
                .setCurrencyId(currencyId)
                .setInitiatorTypeId(initiatorTypeId)
                .setCreatedDate(Tool.currentDateTime())
                .setStatementDate(Tool.currentDateTime()));

        transactionLinesRepository.save(new TransactionLines(               // debit
                transaction.getId(),
                transactionCodeId,
                drAccount.getId(),
                String.format("%s done by %s %s", description, drAccountEntity.getFirstName(), drAccountEntity.getLastName()),
                amount.negate()));
        AccountBalance drAccountBalance = accountBalanceRepository.findByAccountId(drAccount.getId())
                .orElse(new AccountBalance().setAccountId(drAccount.getId()).setBalance(new BigDecimal("0")));
        accountBalanceRepository.save(drAccountBalance.setBalance(drAccountBalance.getBalance().subtract(amount)));

        transactionLinesRepository.save(new TransactionLines(               // credit
                transaction.getId(),
                transactionCodeId,
                crAccount.getId(),
                String.format("%s received for %s %s", description, crAccountEntity.getFirstName(), crAccountEntity.getLastName()),
                amount));
        AccountBalance crAccountBalance = accountBalanceRepository.findByAccountId(crAccount.getId())
                .orElse(new AccountBalance().setAccountId(crAccount.getId()).setBalance(new BigDecimal("0")));
        accountBalanceRepository.save(crAccountBalance.setBalance(crAccountBalance.getBalance().add(amount)));
    }

    public EntityClass interAccountTransfer(EntityClass entity, String fromAccountType, String toAccountType, String currency, BigDecimal amount) {
        Currency requiredCurrency = currencyRepository.findByIsoCode(currency).orElseThrow(() ->
                new ICEcashException(currency + " currency does not exist", EC1062));
        AccountType accountType1 = accountTypeRepository.findByNameAndCurrencyId(fromAccountType, requiredCurrency.getId()).orElseThrow(() ->
                new ICEcashException(String.format("'%s' account type for %s currency does not exist", fromAccountType, requiredCurrency.getId()), EC1060));
        AccountType accountType2 = accountTypeRepository.findByNameAndCurrencyId(toAccountType, requiredCurrency.getId()).orElseThrow(() ->
                new ICEcashException(String.format("'%s' account type for %s currency does not exist", toAccountType, requiredCurrency.getId()), EC1060));
        Account fromAccount = accountRepository.findByEntityIdAndAccountTypeId(entity.getId(), accountType1.getId()).stream().findFirst().orElseThrow(() ->
                new ICEcashException("Account does not exist", EC1022));
        Account toAccount = accountRepository.findByEntityIdAndAccountTypeId(entity.getId(), accountType2.getId()).stream().findFirst().orElseThrow(() ->
                new ICEcashException("Account does not exist", EC1022));
        TransactionCode transactionCode = transactionCodeRepository.getTransactionCodeByCode(DEP).orElseThrow(() ->
                new MozRegistrationException(EC1061, String.format("Transaction code '%s' does not exist", DEP), false));
        InitiatorType initiatorType = initiatorTypeRepository.findByDescription(CASH).orElseThrow(() ->
                new MozRegistrationException(EC1057, String.format("Initiator type '%s' does not exist", CASH), false));
        transferMoney(fromAccount, toAccount, amount, requiredCurrency.getId(), initiatorType.getId(), transactionCode.getId(), "Inter account transfer");
        return entity;
    }

    public void topUpAccount(String accountIdOrNumber, BigDecimal amount, String reference) {
        List<Account> accounts = accountRepository.findByAccountNumber(accountIdOrNumber);
        Account account = !accounts.isEmpty() ? accounts.getFirst() : accountRepository.findById(Tool.parseInteger(accountIdOrNumber, -1)).orElseThrow(() ->
                new ICEcashException(EC1022, String.format("Account %s does not exist", accountIdOrNumber)));
        AccountType accountType = accountTypeRepository.findById(account.getAccountTypeId()).orElseThrow(() ->
                new ICEcashException("Account Type does not exist: " + account.getAccountTypeId(), EC1060, true));
        Account drAccount;
        if (AccountType.SUBSIDY_ACCOUNT.equals(accountType.getName())) {
            drAccount = accountRepository.findByAccountNumber(mozProperties.getSubsidyPoolAccountNumber()).stream().findFirst().orElseThrow(() ->
                    new MozRegistrationException(EC1022, String.format("Subsidy pool account %s does not exist", mozProperties.getSubsidyPoolAccountNumber()), false));
        } else if (AccountType.FNDS_ACCOUNT.equals(accountType.getName())) {
            drAccount = accountRepository.findByAccountNumber(mozProperties.getFndsSuspenseAccountNumber()).stream().findFirst().orElseThrow(() ->
                    new MozRegistrationException(EC1022, String.format("FNDS Suspense account %s does not exist", mozProperties.getFndsSuspenseAccountNumber()), false));
        } else {
            drAccount = accountRepository.findByAccountNumber(mozProperties.getSuspenseAccountNumber()).stream().findFirst().orElseThrow(() ->
                    new MozRegistrationException(EC1022, String.format("Suspense account %s does not exist", mozProperties.getSuspenseAccountNumber()), false));
        }
        TransactionCode transactionCode = transactionCodeRepository.getTransactionCodeByCode(DEP).orElseThrow(() ->
                new MozRegistrationException(EC1061, String.format("Transaction code '%s' does not exist", DEP), false));
        InitiatorType initiatorType = initiatorTypeRepository.findByDescription(CASH).orElseThrow(() ->
                new MozRegistrationException(EC1057, String.format("Initiator type '%s' does not exist", CASH), false));
        transferMoney(drAccount, account, amount, accountType.getCurrencyId(), initiatorType.getId(), transactionCode.getId(), reference);
    }
}

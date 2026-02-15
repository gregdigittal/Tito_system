package cash.ice.ledger.service.impl;

import cash.ice.common.constant.PaymentMetaKey;
import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeeEntry;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.service.PaymentService;
import cash.ice.common.utils.Tool;
import cash.ice.ledger.service.AccountBalanceService;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.AccountBalance;
import cash.ice.sqldb.entity.Transaction;
import cash.ice.sqldb.entity.TransactionLines;
import cash.ice.sqldb.repository.TransactionLinesRepository;
import cash.ice.sqldb.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class LedgerPaymentService implements PaymentService {
    private static final int CHANNEL_ID = 2;         //API

    private final TransactionRepository transactionRepository;
    private final TransactionLinesRepository transactionLinesRepository;
    private final AccountBalanceService accountBalanceService;

    @Transactional(timeout = 30)
    @Override
    public void processPayment(FeesData feesData, Headers headers) {
        log.info("Ledger process payment for vendorRef: {}", feesData.getVendorRef());

        Transaction transaction = new Transaction();
        if (feesData.getPaymentRequest().containsMetaKey(PaymentMetaKey.RollbackExistingTransaction)) {
            log.info("  rollback existing transaction: " + feesData.getVendorRef());
            List<Transaction> transactions = transactionRepository.findBySessionId(feesData.getVendorRef());
            if (!transactions.isEmpty()) {
                transaction = transactions.getLast();
                rollbackTransaction(transaction, feesData);
            } else {
                log.error("transaction with sessionId={} does not exist, creating new", feesData.getVendorRef());
            }
        }

        //Process Transaction
        Transaction savedTransaction = transactionRepository.save(transaction
                .setTransactionCodeId(feesData.getTransactionCodeId())
                .setSessionId(feesData.getVendorRef())
                .setChannelId(CHANNEL_ID)
                .setCurrencyId(feesData.getCurrencyId())
                .setInitiatorId(feesData.getInitiatorId())
                .setInitiatorTypeId(feesData.getInitiatorTypeId())
                .setCreatedDate(feesData.getPaymentRequest() != null && feesData.getPaymentRequest().getDate() != null ? feesData.getPaymentRequest().getDate() : Tool.currentDateTime())
                .setMeta(feesData.getMetaData())
                .setStatementDate(Tool.currentDateTime()));

        //Loop through Transaction Code charges and process them
        feesData.setTransactionId(String.valueOf(savedTransaction.getId()));
        Map<TransactionLines, Account> transactionLines = processCharges(feesData, savedTransaction, false);
        updateAccountBalances(transactionLines);
        AccountBalance drAccountBalance = accountBalanceService.findOrCalculateAccountBalance(feesData.getOriginalDrAccountId());
        feesData.setBalance(drAccountBalance.getBalance());
    }

    private void rollbackTransaction(Transaction transaction, FeesData feesData) {
        List<TransactionLines> lines = transactionLinesRepository.findByTransactionIdIn(List.of(transaction.getId()));
        Map<TransactionLines, Account> transactionLines = new HashMap<>();
        Map<Integer, Account> accountsCache = feesData.getFeeEntries().stream().flatMap(fee -> Stream.of(
                new Account().setId(fee.getDrAccountId()).setAccountTypeId(fee.getDrAccountTypeId()),
                new Account().setId(fee.getCrAccountId()).setAccountTypeId(fee.getCrAccountTypeId())
        )).distinct().collect(toMap(Account::getId, a -> a));
        for (TransactionLines l : lines) {
            TransactionLines line = new TransactionLines(l.getTransactionId(), l.getTransactionCodeId(), l.getEntityAccountId(),
                    l.getDescription(), l.getAmount().negate());
            TransactionLines savedLine = transactionLinesRepository.save(line);
            Account account = accountsCache.get(savedLine.getEntityAccountId());
            transactionLines.put(savedLine, account != null ? account : new Account().setId(savedLine.getEntityAccountId()).setAccountTypeId(1));
        }
        updateAccountBalances(transactionLines);
    }

    private void updateAccountBalances(Map<TransactionLines, Account> transactionLines) {
        Map<Account, BigDecimal> accountBalanceChangeMap = transactionLines.entrySet().stream().collect(
                groupingBy(Map.Entry::getValue, mapping(entry -> entry.getKey().getAmount(),
                        reducing(BigDecimal.ZERO, BigDecimal::add))));
        accountBalanceService.updateAccountBalances(accountBalanceChangeMap);
    }

    @Transactional(timeout = 30)
    @Override
    public void processRefund(ErrorData errorData) {
        log.info(">>>>>> Ledger process refund for vendorRef: {}", errorData.getFeesData().getVendorRef());
        Transaction transaction = getTransactionBy(errorData.getFeesData().getVendorRef());
        if (transaction != null) {
            Map<TransactionLines, Account> transactionLines = processCharges(errorData.getFeesData(),
                    transaction, true);
            updateAccountBalances(transactionLines);
        } else {
            log.warn("Non-existent transaction with sessionId: " + errorData.getFeesData().getVendorRef());
        }
    }

    private Transaction getTransactionBy(String sessionId) {
        List<Transaction> transactions = transactionRepository.findBySessionId(sessionId);
        return transactions.isEmpty() ? null : transactions.getLast();
    }

    //Process all transactions
    private Map<TransactionLines, Account> processCharges(FeesData feesData, Transaction transaction, boolean reverse) {
        Map<TransactionLines, Account> transactionLines = new HashMap<>();
        for (FeeEntry feeEntry : feesData.getFeeEntries()) {
            Integer drAccountId = reverse ? feeEntry.getCrAccountId() : feeEntry.getDrAccountId();
            Integer crAccountId = reverse ? feeEntry.getDrAccountId() : feeEntry.getCrAccountId();
            Integer drAccountTypeId = reverse ? feeEntry.getCrAccountTypeId() : feeEntry.getDrAccountTypeId();
            Integer crAccountTypeId = reverse ? feeEntry.getDrAccountTypeId() : feeEntry.getCrAccountTypeId();
            String drEntityFirstName = reverse ? feeEntry.getCrEntityFirstName() : feeEntry.getDrEntityFirstName();
            String drEntityLastName = reverse ? feeEntry.getCrEntityLastName() : feeEntry.getDrEntityLastName();
            String crEntityFirstName = reverse ? feeEntry.getDrEntityFirstName() : feeEntry.getCrEntityFirstName();
            String crEntityLastName = reverse ? feeEntry.getDrEntityLastName() : feeEntry.getCrEntityLastName();

            //get charge transaction code
            String drDescription = Tool.concat(feeEntry.getTransactionCodeDescription(), " done by ",
                    drEntityFirstName, " ", drEntityLastName, " ref(", feesData.getVendorRef(), ")", reverse ? ", refund" : "");
            String crDescription = Tool.concat(feeEntry.getTransactionCodeDescription(), " received for ",
                    crEntityFirstName, " ", crEntityLastName, " ref(", feesData.getVendorRef(), ")", reverse ? ", refund" : "");

            //get amount
            BigDecimal chargeAmount = feeEntry.getAmount();

            //process transaction line
            TransactionLines drTransactionLine = new TransactionLines(transaction.getId(), feeEntry.getTransactionCodeId(),
                    drAccountId, drDescription, chargeAmount.negate());
            transactionLines.put(drTransactionLine, new Account().setId(drAccountId).setAccountTypeId(drAccountTypeId));
            transactionLinesRepository.save(drTransactionLine);

            TransactionLines crTransactionLine = new TransactionLines(transaction.getId(), feeEntry.getTransactionCodeId(),
                    crAccountId, crDescription, chargeAmount);
            transactionLines.put(crTransactionLine, new Account().setId(crAccountId).setAccountTypeId(crAccountTypeId));
            transactionLinesRepository.save(crTransactionLine);
        }
        return transactionLines;
    }
}

package cash.ice.ledger.performance;

import cash.ice.common.performance.PerfStopwatch;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.Transaction;
import cash.ice.sqldb.entity.TransactionLines;
import cash.ice.sqldb.repository.AccountRepository;
import cash.ice.sqldb.repository.TransactionLinesRepository;
import cash.ice.sqldb.repository.TransactionRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/test/ledger")
@Profile({"local-perf", "dev-k8s-perf", "dev-mysql-k8s-perf", "uat-k8s-perf"})
@AllArgsConstructor
@Slf4j
public class TestPerformanceController {
    private static final Random random = new Random();

    private TestPerformanceDbService dbService;
    private TransactionLinesRepository transactionLinesRepository;
    private TransactionRepository transactionRepository;
    private AccountRepository accountRepository;

    @PostMapping("/mysql/db")
    public ResponseEntity<Object> testMysqlLedgerInit(@Valid @RequestBody TestPerformanceInitRequest request) {
        log.debug("Received test performance mysql init db request: " + request);
        try {
            dbService.fillDatabase(request);
            return new ResponseEntity<>("Filled mysql db, records: " + transactionLinesRepository.count(), HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/mysql/db")
    public ResponseEntity<Object> testMysqlLedgerDelete() {
        log.debug("Received test performance mysql delete db request.");
        try {
            dbService.clearDatabase();
            return new ResponseEntity<>("Cleared mysql db, records: " + transactionLinesRepository.count(), HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/mysql")
    public ResponseEntity<Object> testMysqlLedger(@Valid @RequestBody TestPerformanceRequest request) {
        log.debug("Received test performance mysql request: " + request);
        try {
            int accountsQty = (int) Math.min(transactionLinesRepository.count() / request.getRecordsPerAccount(), 9);
            if (accountsQty < 1 || !Tool.isPower10(request.getRecordsPerAccount())) {
                throw new IllegalArgumentException("recordsPerAccount must be power of 10");
            }
            log.debug("Defined {} accounts", accountsQty);
            Map<Integer, Account> accountsMap = getAccountsMap(request.getRecordsPerAccount(), accountsQty);
            TestStrategy testStrategy = TestStrategy.of(request.getThreads());

            try (TestLogic logic = createTestLogic(request.getLogic(), accountsMap, request, accountsQty)) {
                log.debug("Using: {} strategy, invoke: {}", testStrategy, logic.getClass().getSimpleName());
                PerfStopwatch statistics = testStrategy.performInvokes(request, logic);
                if (request.getSaveResults() == Boolean.TRUE) {
                    dbService.saveStatistics(request, statistics);
                }
                return new ResponseEntity<>(statistics, HttpStatus.OK);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<Integer, Account> getAccountsMap(int recordsPerAccount, int accountsQty) {
        HashMap<Integer, Account> map = new HashMap<>();
        for (int i = 1; i <= accountsQty; i++) {
            int accountNumber = recordsPerAccount + i;
            List<Account> accounts = accountRepository.findByAccountNumber(String.valueOf(accountNumber));
            if (accounts.isEmpty()) {
                throw new IllegalStateException("Unexisting account: " + accountNumber);
            }
            map.put(accountNumber, accounts.get(0));
        }
        return map;
    }

    private TestLogic createTestLogic(String logicName, Map<Integer, Account> accountsMap, TestPerformanceRequest request, int accountsQty) {
        return switch (logicName) {
            case "GetBalance" -> new GetBalanceTestLogic(transactionLinesRepository, accountsMap, request, accountsQty);
            case "Ledger" -> new LedgerTestLogic(transactionRepository, transactionLinesRepository, dbService, accountsMap, request, accountsQty);
            default -> throw new IllegalArgumentException("Unknown logic: " + logicName);
        };
    }

    private static abstract class TestLogic implements Runnable, AutoCloseable {
        @Override
        public void close() {
        }
    }

    @AllArgsConstructor
    private static class GetBalanceTestLogic extends TestLogic {
        private TransactionLinesRepository transactionLinesRepository;
        private Map<Integer, Account> accountsMap;
        private TestPerformanceRequest request;
        private int accountsQty;

        @Override
        public void run() {
            int accountNum = request.getRecordsPerAccount() + random.nextInt(accountsQty) + 1;
            int accountId = accountsMap.get(accountNum).getId();
            transactionLinesRepository.getBalance(accountId);
        }
    }

    @RequiredArgsConstructor
    private static class LedgerTestLogic extends TestLogic {
        private final TransactionRepository transactionRepository;
        private final TransactionLinesRepository transactionLinesRepository;
        private final TestPerformanceDbService dbService;
        private final Map<Integer, Account> accountsMap;
        private final TestPerformanceRequest request;
        private final int accountsQty;
        private final Map<Integer, TransactionLines> createdLines = new ConcurrentHashMap<>();
        private final Map<Integer, Transaction> createdTransactions = new ConcurrentHashMap<>();

        @Override
        public void run() {
            int accountNum = request.getRecordsPerAccount() + random.nextInt(accountsQty) + 1;
            int accountId = accountsMap.get(accountNum).getId();
            dbService.performLedgerService(accountId, request.getTransactionLines(), createdTransactions, createdLines);
        }

        @Override
        public void close() {
            log.debug("Clearing test resources.");
            transactionLinesRepository.deleteAllById(createdLines.keySet());
            transactionRepository.deleteAllById(createdTransactions.keySet());
        }
    }
}

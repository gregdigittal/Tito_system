package cash.ice.ledger.service.impl;

import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.fee.FeeEntry;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.sqldb.entity.AccountBalance;
import cash.ice.sqldb.entity.Transaction;
import cash.ice.sqldb.entity.TransactionLines;
import cash.ice.sqldb.repository.TransactionLinesRepository;
import cash.ice.sqldb.repository.TransactionRepository;
import cash.ice.common.service.PaymentService;
import cash.ice.common.utils.Tool;
import cash.ice.ledger.service.AccountBalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerPaymentServiceTest {
    private static final int CURRENCY_CODE_ID = 1;
    private static final String CURRENCY_CODE = "ZWL";
    private static final int INITIATOR_DESC_ID = 4;
    private static final String INITIATOR_DESC = "card";
    private static final int TRANSACTION_ID = 100;
    private static final int CHANNEL_CODE_ID = 2;
    private static final int ACCOUNT_ID = 4;
    private static final String VENDOR_REF = "testVendorRef";
    private static final int TRANSACTION_CODE_ID = 11;
    private static final String TRANSACTION_CODE = "PAY";

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionLinesRepository transactionLinesRepository;
    @Mock
    private AccountBalanceService accountBalanceService;

    private PaymentService service;

    @BeforeEach
    void init() {
        service = new LedgerPaymentService(transactionRepository, transactionLinesRepository, accountBalanceService);
    }

    @Test
    void testSuccessProcess() {
        FeesData request = createFeesData();
        when(accountBalanceService.findOrCalculateAccountBalance(request.getOriginalDrAccountId()))
                .thenReturn(new AccountBalance().setBalance(new BigDecimal("10000.0")));
        when(transactionRepository.save(any())).thenReturn(new Transaction().setId(TRANSACTION_ID));
        service.processPayment(request, Tool.toKafkaHeaders(List.of()));

        ArgumentCaptor<Transaction> transactionArg = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionArg.capture());
        assertThat(transactionArg.getValue().getSessionId()).isEqualTo(VENDOR_REF);
        assertThat(transactionArg.getValue().getTransactionCodeId()).isEqualTo(TRANSACTION_CODE_ID);
        assertThat(transactionArg.getValue().getCurrencyId()).isEqualTo(CURRENCY_CODE_ID);
        assertThat(transactionArg.getValue().getInitiatorTypeId()).isEqualTo(INITIATOR_DESC_ID);
        assertThat(transactionArg.getValue().getChannelId()).isEqualTo(CHANNEL_CODE_ID);
        assertThat(transactionArg.getValue().getCreatedDate()).isNotNull();
        assertThat(transactionArg.getValue().getStatementDate()).isNotNull();

        ArgumentCaptor<TransactionLines> lineArg = ArgumentCaptor.forClass(TransactionLines.class);
        verify(transactionLinesRepository, times(4)).save(lineArg.capture());
        checkLines(lineArg.getAllValues());
    }

    private void checkLines(List<TransactionLines> lines) {
        assertThat(lines.size()).isEqualTo(4);

        assertThat(lines.get(0).getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(lines.get(0).getEntityAccountId()).isEqualTo(2);
        assertThat(lines.get(0).getTransactionCodeId()).isEqualTo(TRANSACTION_CODE_ID);
        assertThat(lines.get(0).getAmount()).isEqualTo(new BigDecimal("-100"));
        assertThat(lines.get(0).getDescription()).isEqualTo("descr1 done by firstName2 lastName2 ref(testVendorRef)");

        assertThat(lines.get(1).getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(lines.get(1).getEntityAccountId()).isEqualTo(3);
        assertThat(lines.get(1).getTransactionCodeId()).isEqualTo(TRANSACTION_CODE_ID);
        assertThat(lines.get(1).getAmount()).isEqualTo(new BigDecimal("100"));
        assertThat(lines.get(1).getDescription()).isEqualTo("descr1 received for firstName3 lastName3 ref(testVendorRef)");

        assertThat(lines.get(2).getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(lines.get(2).getEntityAccountId()).isEqualTo(4);
        assertThat(lines.get(2).getTransactionCodeId()).isEqualTo(TRANSACTION_CODE_ID);
        assertThat(lines.get(2).getAmount()).isEqualTo(new BigDecimal("-50"));
        assertThat(lines.get(2).getDescription()).isEqualTo("descr2 done by firstName4 lastName4 ref(testVendorRef)");

        assertThat(lines.get(3).getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(lines.get(3).getEntityAccountId()).isEqualTo(5);
        assertThat(lines.get(3).getTransactionCodeId()).isEqualTo(TRANSACTION_CODE_ID);
        assertThat(lines.get(3).getAmount()).isEqualTo(new BigDecimal("50"));
        assertThat(lines.get(3).getDescription()).isEqualTo("descr2 received for firstName5 lastName5 ref(testVendorRef)");
    }

    private FeesData createFeesData() {
        FeesData feesData = new FeesData();
        feesData.setVendorRef(VENDOR_REF);
        feesData.setPaymentRequest(new PaymentRequest().setVendorRef(VENDOR_REF));
        feesData.setOriginalDrAccountId(ACCOUNT_ID);
        feesData.setTransactionCodeId(TRANSACTION_CODE_ID);
        feesData.setTransactionCode(TRANSACTION_CODE);
        feesData.setCurrencyId(CURRENCY_CODE_ID);
        feesData.setCurrencyCode(CURRENCY_CODE);
        feesData.setInitiatorTypeId(INITIATOR_DESC_ID);
        feesData.setInitiatorTypeDescription(INITIATOR_DESC);
        feesData.setFeeEntries(List.of(
                new FeeEntry().setAmount(new BigDecimal("100")).setDrEntityFirstName("firstName2").setDrEntityLastName("lastName2")
                        .setCrEntityFirstName("firstName3").setCrEntityLastName("lastName3")
                        .setDrAccountId(2).setDrAccountTypeId(1).setCrAccountId(3).setCrAccountTypeId(1)
                        .setTransactionCodeId(TRANSACTION_CODE_ID).setTransactionCodeDescription("descr1"),
                new FeeEntry().setAmount(new BigDecimal("50")).setDrEntityFirstName("firstName4").setDrEntityLastName("lastName4")
                        .setCrEntityFirstName("firstName5").setCrEntityLastName("lastName5")
                        .setDrAccountId(4).setDrAccountTypeId(1).setCrAccountId(5).setCrAccountTypeId(1)
                        .setTransactionCodeId(TRANSACTION_CODE_ID).setTransactionCodeDescription("descr2")));
        return feesData;
    }
}
package cash.ice.api.service.impl;

import cash.ice.api.entity.zim.Payment;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.service.KafkaSender;
import cash.ice.sqldb.entity.PaymentLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentExecutionServiceImplTest {
    private static final int PAYMENT_ID = 1;
    private static final int PAYMENT_LINE_ID = 2;
    private static final int ACCOUNT_ID = 12;
    private static final BigDecimal AMOUNT = new BigDecimal("10.0");
    private static final String BIN = "123456";
    private static final String SWIFT = "1234";
    private static final String ACCOUNT_NO = "12345";
    private static final String BRANCH = "123";
    private static final String BENEFICIARY_NAME = "name";
    private static final String BENEFICIARY_REFERENCE = "ref";
    private static final String TRANSACTION_CODE = "TRN";
    private static final String CURRENCY = "ZWL";

    @Mock
    private KafkaSender kafkaSender;
    @Captor
    private ArgumentCaptor<PaymentRequest> paymentRequestCaptor;

    private PaymentExecutionServiceImpl service;

    @BeforeEach
    void init() {
        service = new PaymentExecutionServiceImpl(kafkaSender);
    }

    @Test
    void testExecute() {
        Payment payment = new Payment().setId(PAYMENT_ID).setAccountId(ACCOUNT_ID);
        List<PaymentLine> paymentLines = List.of(new PaymentLine().setId(PAYMENT_LINE_ID).setTransactionCode(TRANSACTION_CODE)
                .setCurrency(CURRENCY).setAmount(AMOUNT)
                .setMeta(Map.of("paymentMethod", "RTGS", "bankBin", BIN, "swiftCode", SWIFT,
                        "bankAccountNo", ACCOUNT_NO, "branchCode", BRANCH, "beneficiaryName", BENEFICIARY_NAME,
                        "beneficiaryReference", BENEFICIARY_REFERENCE)));

        service.execute(payment, paymentLines);
        verify(kafkaSender).sendPaymentRequest(anyString(), paymentRequestCaptor.capture());
        PaymentRequest actualRequest = paymentRequestCaptor.getValue();
        assertThat(actualRequest.getVendorRef()).isNotNull();
        assertThat(actualRequest.getTx()).isEqualTo(TRANSACTION_CODE);
        assertThat(actualRequest.getCurrency()).isEqualTo(CURRENCY);
        assertThat(actualRequest.getInitiatorType()).isEqualTo("icecash");
        assertThat(actualRequest.getInitiator()).isEqualTo(String.valueOf(ACCOUNT_ID));
        assertThat(actualRequest.getAmount()).isEqualTo(AMOUNT);
        Map<String, Object> actualMeta = actualRequest.getMeta();
        assertThat(actualMeta.get("referenceId")).isNotNull();
        assertThat(actualMeta.get("bankBin")).isEqualTo(BIN);
        assertThat(actualMeta.get("swiftCode")).isEqualTo(SWIFT);
        assertThat(actualMeta.get("bankAccountNo")).isEqualTo(ACCOUNT_NO);
        assertThat(actualMeta.get("branchCode")).isEqualTo(BRANCH);
        assertThat(actualMeta.get("beneficiaryName")).isEqualTo(BENEFICIARY_NAME);
        assertThat(actualMeta.get("beneficiaryReference")).isEqualTo(BENEFICIARY_REFERENCE);
        assertThat(actualMeta.get("paymentId")).isEqualTo(PAYMENT_ID);
        assertThat(actualMeta.get("paymentLineId")).isEqualTo(PAYMENT_LINE_ID);
    }
}
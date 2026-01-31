package cash.ice.fee.service.impl;

import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.fee.FeeEntry;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.fee.error.ICEcashInvalidRequestException;
import cash.ice.fee.error.ICEcashKycRequiredException;
import cash.ice.fee.service.FeeDeviceCalculator;
import cash.ice.fee.service.FeeService;
import cash.ice.fee.service.TransactionLimitCheckService;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.EntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static cash.ice.common.error.ErrorCodes.*;
import static cash.ice.sqldb.entity.AccountStatus.ACTIVE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeServiceImplTest {
    private static final String VENDOR_REF = "testVendorRef";
    private static final int CURRENCY_ID = 1;
    private static final int ENTITY_ID = 2;
    private static final int TRANSACTION_CODE_ID = 3;
    private static final int ACCOUNT_TYPE_ID = 4;

    @Mock
    private CacheableDataService dataService;
    @Mock
    private FeeDeviceCalculator feeDeviceCalculator;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private TransactionLimitCheckService transactionLimitCheckService;

    private FeeService service;
    private PaymentRequest paymentRequest;

    @BeforeEach
    void init() {
        service = new FeeServiceImpl(dataService, feeDeviceCalculator, entityRepository, transactionLimitCheckService);
        paymentRequest = createPaymentRequest();
    }

    @Test
    void testSuccessfulProcessing() {
        Account originalDrAccount = new Account().setId(1).setAccountStatus(ACTIVE).setEntityId(11);
        Fee fee1 = new Fee().setChargeType(ChargeType.ORIGINAL).setActive(true).setId(10).setProcessOrder(1).setDrEntityAccount(originalDrAccount).setTransactionCode(new TransactionCode().setId(TRANSACTION_CODE_ID));
        Fee fee2 = new Fee().setChargeType(ChargeType.FIXED).setActive(true).setId(1).setProcessOrder(2)
                .setAmount(new BigDecimal("1.00")).setDrEntityAccount(new Account().setAccountStatus(ACTIVE).setEntityId(12)).setTransactionCode(new TransactionCode().setId(TRANSACTION_CODE_ID));
        Fee fee3 = new Fee().setChargeType(ChargeType.PERCENT).setActive(true).setId(2).setProcessOrder(3)
                .setAmount(new BigDecimal("0.02")).setSrcAmountFeeId(1).setDrEntityAccount(new Account().setAccountStatus(ACTIVE).setEntityId(13)).setTransactionCode(new TransactionCode().setId(TRANSACTION_CODE_ID));
        Fee fee4 = new Fee().setChargeType(ChargeType.PERCENT).setActive(true).setId(3).setProcessOrder(4)
                .setAmount(new BigDecimal("0.15")).setSrcAmountFeeId(10).setDrEntityAccount(new Account().setAccountStatus(ACTIVE).setEntityId(14)).setTransactionCode(new TransactionCode().setId(TRANSACTION_CODE_ID));
        Currency currency = new Currency().setId(CURRENCY_ID);
        TransactionCode transactionCode = new TransactionCode().setId(TRANSACTION_CODE_ID).setActive(true);
        InitiatorType initiatorType = new InitiatorType().setEntityId(ENTITY_ID).setActive(true);

        when(dataService.getCurrency(paymentRequest.getCurrency())).thenReturn(currency);
        when(dataService.getTransactionCode(paymentRequest.getTx())).thenReturn(transactionCode);
        when(dataService.getInitiator(paymentRequest.getInitiatorType())).thenReturn(initiatorType);
        when(dataService.getAccountType(CURRENCY_ID, AccountType.PRIMARY_ACCOUNT)).thenReturn(new AccountType().setId(ACCOUNT_TYPE_ID));
        when(dataService.getAccount(ENTITY_ID, ACCOUNT_TYPE_ID)).thenReturn(new Account().setAccountStatus(ACTIVE).setEntityId(ENTITY_ID));
        when(dataService.getFees(TRANSACTION_CODE_ID, CURRENCY_ID)).thenReturn(List.of(fee1, fee2, fee3, fee4));
        when(entityRepository.findAllById(List.of(12, ENTITY_ID, 13, 14, 11))).thenReturn(List.of(
                new EntityClass().setId(ENTITY_ID).setStatus(EntityStatus.ACTIVE),
                new EntityClass().setId(11).setStatus(EntityStatus.ACTIVE),
                new EntityClass().setId(12).setStatus(EntityStatus.ACTIVE),
                new EntityClass().setId(13).setStatus(EntityStatus.ACTIVE),
                new EntityClass().setId(14).setStatus(EntityStatus.ACTIVE)));

        FeesData actualFeesData = service.process(paymentRequest);

        assertThat(actualFeesData).isNotNull();
        assertThat(actualFeesData.getVendorRef()).isEqualTo(VENDOR_REF);
        assertThat(actualFeesData.getOriginalDrAccountId()).isEqualTo(1);
        assertThat(actualFeesData.getPaymentRequest()).isEqualTo(paymentRequest);
        assertThat(actualFeesData.getCurrencyId()).isEqualTo(CURRENCY_ID);
        assertThat(actualFeesData.getTransactionCodeId()).isEqualTo(TRANSACTION_CODE_ID);
        assertThat(actualFeesData.getInitiatorTypeEntityId()).isEqualTo(ENTITY_ID);
        assertThat(actualFeesData.getFeeEntries()).asList().isNotNull().containsExactlyInAnyOrderElementsOf(Arrays.asList(
                new FeeEntry().setFeeId(10).setTransactionCodeId(TRANSACTION_CODE_ID).setSourceAmount(new BigDecimal("5")).setAmount(new BigDecimal("5")).setDrEntityId(11).setCrEntityId(2).setDrAccountId(originalDrAccount.getId()),
                new FeeEntry().setFeeId(1).setTransactionCodeId(TRANSACTION_CODE_ID).setSourceAmount(new BigDecimal("5")).setAmount(new BigDecimal("1.00")).setDrEntityId(12).setCrEntityId(2),
                new FeeEntry().setFeeId(2).setTransactionCodeId(TRANSACTION_CODE_ID).setSourceAmount(new BigDecimal("1.00")).setAmount(new BigDecimal("0.0200")).setDrEntityId(13).setCrEntityId(2),
                new FeeEntry().setFeeId(3).setTransactionCodeId(TRANSACTION_CODE_ID).setSourceAmount(new BigDecimal("5")).setAmount(new BigDecimal("0.75")).setDrEntityId(14).setCrEntityId(2)
        ));
    }

    @Test
    void testInvalidRequestAmount() {
        paymentRequest.setAmount(BigDecimal.valueOf(-10.0));
        ICEcashInvalidRequestException actualException = assertThrows(ICEcashInvalidRequestException.class,
                () -> service.process(paymentRequest));
        assertThat(actualException.getErrorCode()).isEqualTo(EC3001);
    }

    @Test
    void testInactiveTransactionCode() {
        TransactionCode transactionCode = new TransactionCode().setActive(false);
        when(dataService.getTransactionCode(paymentRequest.getTx())).thenReturn(transactionCode);
        ICEcashInvalidRequestException actualException = assertThrows(ICEcashInvalidRequestException.class,
                () -> service.process(paymentRequest));
        assertThat(actualException.getErrorCode()).isEqualTo(EC3011);
    }

    @Test
    void testInactiveInitiator() {
        when(dataService.getCurrency(paymentRequest.getCurrency())).thenReturn(new Currency().setId(CURRENCY_ID));
        when(dataService.getTransactionCode(paymentRequest.getTx())).thenReturn(new TransactionCode().setActive(true));
        when(dataService.getInitiator(paymentRequest.getInitiatorType())).thenReturn(new InitiatorType().setEntityId(ENTITY_ID).setActive(false));
        ICEcashInvalidRequestException actualException = assertThrows(ICEcashInvalidRequestException.class,
                () -> service.process(paymentRequest));
        assertThat(actualException.getErrorCode()).isEqualTo(EC3012);
    }

    @Test
    void testAbsentInitiatorEntityId() {
        when(dataService.getCurrency(paymentRequest.getCurrency())).thenReturn(new Currency().setId(CURRENCY_ID));
        when(dataService.getTransactionCode(paymentRequest.getTx())).thenReturn(
                new TransactionCode().setId(TRANSACTION_CODE_ID).setActive(true));
        when(dataService.getInitiator(paymentRequest.getInitiatorType())).thenReturn(new InitiatorType().setActive(true));
        when(dataService.getFees(TRANSACTION_CODE_ID, CURRENCY_ID)).thenReturn(
                List.of(new Fee().setChargeType(ChargeType.ORIGINAL).setActive(true).setDrEntityAccount(new Account())));
        ICEcashInvalidRequestException actualException = assertThrows(ICEcashInvalidRequestException.class,
                () -> service.process(paymentRequest));
        assertThat(actualException.getErrorCode()).isEqualTo(EC3003);
    }

    @Test
    void testInitiatorEntityKycFail() {
        when(dataService.getCurrency(paymentRequest.getCurrency())).thenReturn(new Currency().setId(CURRENCY_ID));
        when(dataService.getTransactionCode(paymentRequest.getTx())).thenReturn(
                new TransactionCode().setId(TRANSACTION_CODE_ID).setActive(true).setKycRequired(true));
        when(dataService.getInitiator(paymentRequest.getInitiatorType())).thenReturn(new InitiatorType().setEntityId(ENTITY_ID).setActive(true));
        when(dataService.getAccountType(CURRENCY_ID, AccountType.PRIMARY_ACCOUNT)).thenReturn(new AccountType().setId(ACCOUNT_TYPE_ID));
        when(dataService.getAccount(ENTITY_ID, ACCOUNT_TYPE_ID)).thenReturn(new Account().setAccountStatus(ACTIVE).setEntityId(ENTITY_ID));
        when(dataService.getFees(TRANSACTION_CODE_ID, CURRENCY_ID)).thenReturn(
                List.of(new Fee().setChargeType(ChargeType.ORIGINAL).setActive(true).setDrEntityAccount(new Account()
                        .setAccountStatus(ACTIVE).setEntityId(11))));
        when(entityRepository.findAllById(List.of(11, ENTITY_ID))).thenReturn(List.of(
                new EntityClass().setId(ENTITY_ID).setStatus(EntityStatus.ACTIVE).setKycStatusId(0),
                new EntityClass().setId(11).setStatus(EntityStatus.ACTIVE)));
        ICEcashKycRequiredException actualException = assertThrows(ICEcashKycRequiredException.class,
                () -> service.process(paymentRequest));
        assertThat(actualException.getErrorCode()).isEqualTo(EC3004);
    }

    @Test
    void testInactiveOriginalCharge() {
        when(dataService.getCurrency(paymentRequest.getCurrency())).thenReturn(new Currency().setId(CURRENCY_ID));
        when(dataService.getTransactionCode(paymentRequest.getTx())).thenReturn(
                new TransactionCode().setId(TRANSACTION_CODE_ID).setActive(true).setKycRequired(true));
        when(dataService.getInitiator(paymentRequest.getInitiatorType())).thenReturn(new InitiatorType().setEntityId(ENTITY_ID).setActive(true));
        when(dataService.getFees(TRANSACTION_CODE_ID, CURRENCY_ID)).thenReturn(List.of(
                new Fee().setActive(false).setChargeType(ChargeType.ORIGINAL).setTransactionCode(new TransactionCode())));
        ICEcashInvalidRequestException actualException = assertThrows(ICEcashInvalidRequestException.class,
                () -> service.process(paymentRequest));
        assertThat(actualException.getErrorCode()).isEqualTo(EC3013);
    }

    private PaymentRequest createPaymentRequest() {
        return new PaymentRequest().setVendorRef(VENDOR_REF).setApiVersion("1").setPartnerId("1")
                .setTx("PAY")
                .setInitiatorType("card")
                .setCurrency("ZWL")
                .setAmount(new BigDecimal("5"));
    }
}
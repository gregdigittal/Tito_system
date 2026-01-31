package cash.ice.fee.service.impl;

import cash.ice.common.constant.EntityMetaKey;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.fee.FeeEntry;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.sqldb.entity.AuthorisationType;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.LimitTier;
import cash.ice.sqldb.entity.TransactionLimit;
import cash.ice.sqldb.repository.TransactionLimitRepository;
import cash.ice.fee.config.property.LimitsProperties;
import cash.ice.fee.dto.TransactionLimitData;
import cash.ice.fee.repository.TransactionLimitDataRepository;
import cash.ice.fee.service.TransactionLimitOverrideService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionLimitCheckServiceImplTest {
    private static final int FEE_ID = 1;
    private static final int CURRENCY_ID = 3;
    private static final int TRANSACTION_CODE_ID = 4;
    private static final int INITIATOR_TYPE_ID = 5;
    private static final int KYC_STATUS_ID1 = 7;
    private static final int KYC_STATUS_ID2 = 8;
    private static final int ENTITY_TYPE_ID1 = 9;
    private static final int ENTITY_TYPE_ID2 = 10;
    private static final int ACCOUNT_TYPE_ID1 = 11;
    private static final int ACCOUNT_TYPE_ID2 = 12;
    private static final int LIMIT_ID1 = 13;
    private static final int LIMIT_ID2 = 14;
    private static final int LIMIT_ID3 = 15;
    private static final String VENDOR_REF = "vendorRef";
    private static final String LIMIT_DATA_ID1 = "dataId1";
    private static final String LIMIT_DATA_ID2 = "dataId2";

    @Mock
    private TransactionLimitOverrideService transactionLimitOverrideService;
    @Mock
    private TransactionLimitRepository transactionLimitRepository;
    @Mock
    private TransactionLimitDataRepository transactionLimitDataRepository;
    @Mock
    private LimitsProperties limitsProperties;
    @InjectMocks
    private TransactionLimitCheckServiceImpl service;

    @Test
    void testCheckLimits() {
        FeesData feesData = new FeesData().setPaymentRequest(new PaymentRequest().setVendorRef(VENDOR_REF)).setCurrencyId(CURRENCY_ID).setTransactionCodeId(TRANSACTION_CODE_ID)
                .setInitiatorTypeId(INITIATOR_TYPE_ID).setFeeEntries(List.of(
                        new FeeEntry().setAmount(BigDecimal.TEN).setFeeId(FEE_ID)
                                .setDrAccountTypeId(ACCOUNT_TYPE_ID1).setDrAuthorisationTypeString(AuthorisationType.SINGLE.toString()).setDrEntityId(11)
                                .setCrAccountTypeId(ACCOUNT_TYPE_ID2).setCrEntityId(12)
                ));
        Map<Integer, EntityClass> allEntities = Map.of(
                11, new EntityClass().setKycStatusId(KYC_STATUS_ID1).setEntityTypeId(ENTITY_TYPE_ID1),
                12, new EntityClass().setMeta(Map.of(EntityMetaKey.TransactionLimitTier, LimitTier.Tier1)).setKycStatusId(KYC_STATUS_ID2).setEntityTypeId(ENTITY_TYPE_ID2)
        );
        TransactionLimit limit1 = new TransactionLimit().setId(LIMIT_ID1).setDailyLimit(new BigDecimal("50.0"));
        TransactionLimit limit2 = new TransactionLimit().setId(LIMIT_ID2).setTransactionMaxLimit(new BigDecimal("50.0"));
        TransactionLimit limit3 = new TransactionLimit().setId(LIMIT_ID3);

        when(limitsProperties.isEnabled()).thenReturn(true);
        when(transactionLimitRepository.findTransactionLimits(CURRENCY_ID, "Debit", TRANSACTION_CODE_ID, KYC_STATUS_ID1,
                ENTITY_TYPE_ID1, ACCOUNT_TYPE_ID1, INITIATOR_TYPE_ID, null, "SINGLE")).thenReturn(List.of(limit1));
        when(transactionLimitRepository.findTransactionLimits(CURRENCY_ID, "Credit", TRANSACTION_CODE_ID, KYC_STATUS_ID2,
                ENTITY_TYPE_ID2, ACCOUNT_TYPE_ID2, INITIATOR_TYPE_ID, "Tier1", null)).thenReturn(List.of(limit2, limit3));
        when(limitsProperties.isOverridesEnabled()).thenReturn(true);
        when(transactionLimitOverrideService.overrideLimits(List.of(limit2, limit3))).thenReturn(List.of(limit2));
        when(transactionLimitDataRepository.findByTransactionLimitId(LIMIT_ID1)).thenReturn(Optional.of(new TransactionLimitData().setId(LIMIT_DATA_ID1).setTransactionLimitId(LIMIT_ID1)
                .setDay(3).setDayTransactions(1).setDayAmount(BigDecimal.TEN).setWeek(2).setWeekTransactions(1).setWeekAmount(BigDecimal.TEN).setMonth(1).setMonthTransactions(1).setMonthAmount(BigDecimal.TEN)));
        when(transactionLimitDataRepository.findByTransactionLimitId(LIMIT_ID2)).thenReturn(Optional.ofNullable(new TransactionLimitData().setId(LIMIT_DATA_ID2).setTransactionLimitId(LIMIT_ID2)));
        when(transactionLimitDataRepository.saveAll(any())).thenAnswer(invocation -> new ArrayList<>((Collection<?>) invocation.getArguments()[0]));

        service.checkLimits(feesData, allEntities);
        assertThat(feesData.getLimitData()).isEqualTo(Map.of(LIMIT_DATA_ID1, BigDecimal.TEN, LIMIT_DATA_ID2, BigDecimal.TEN));
    }
}
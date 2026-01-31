package cash.ice.fee.service.impl;

import cash.ice.sqldb.entity.AuthorisationType;
import cash.ice.sqldb.entity.LimitTier;
import cash.ice.sqldb.entity.TransactionLimit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static cash.ice.sqldb.entity.AuthorisationType.SINGLE;
import static cash.ice.sqldb.entity.LimitTier.Tier1;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TransactionLimitOverrideServiceImplTest {

    @InjectMocks
    private TransactionLimitOverrideServiceImpl service;

    @Test
    void testOverrideLimits() {
        List<TransactionLimit> actualLimits = service.overrideLimits(List.of(
                createTransactionLimit(1000, null, null, null, null, null, null).setId(1),
                createTransactionLimit(null, 1000, 1000, 1000, null, null, null).setId(2),
                createTransactionLimit(1000, null, 1000, 1000, null, Tier1, null).setId(3),
                createTransactionLimit(null, 1000, 1000, 1000, 1000, null, SINGLE).setId(4),
                createTransactionLimit(1000, null, null, 1000, null, Tier1, null).setId(5),
                createTransactionLimit(null, 1000, null, 1000, null, null, SINGLE).setId(6),
                createTransactionLimit(null, null, null, null, null, null, null).setId(7)
        ));
        assertThat(actualLimits.stream().map(TransactionLimit::getId).toList()).isEqualTo(List.of(3, 4));
    }

    private TransactionLimit createTransactionLimit(Integer p1, Integer p2, Integer p3, Integer p4, Integer p5, LimitTier p6, AuthorisationType p7) {
        return new TransactionLimit().setTransactionCodeId(p1).setKycStatusId(p2).setEntityTypeId(p3).setAccountTypeId(p4).setInitiatorTypeId(p5).setTier(p6).setAuthorisationType(p7);
    }
}
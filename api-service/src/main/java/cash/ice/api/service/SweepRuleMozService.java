package cash.ice.api.service;

import cash.ice.api.dto.moz.SweepRuleMoz;

import java.math.BigDecimal;
import java.util.List;

/** Phase 8-6: CRUD for sweep rules per account. */
public interface SweepRuleMozService {

    List<SweepRuleMoz> listByAccountId(Integer accountId);

    SweepRuleMoz create(Integer accountId, Integer authEntityId, String destinationType, String destinationRef, String triggerType,
                         String scheduleExpression, BigDecimal thresholdAmount, Boolean active);

    SweepRuleMoz update(Integer id, Integer authEntityId, String destinationType, String destinationRef,
                        String triggerType, String scheduleExpression, BigDecimal thresholdAmount, Boolean active);

    boolean delete(Integer id, Integer authEntityId);
}

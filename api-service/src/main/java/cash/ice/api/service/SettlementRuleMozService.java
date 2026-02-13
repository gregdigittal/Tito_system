package cash.ice.api.service;

import cash.ice.api.dto.moz.SettlementRuleMoz;

import java.util.List;

/** Phase 8-5: CRUD for settlement rules per entity. */
public interface SettlementRuleMozService {

    List<SettlementRuleMoz> listByEntityId(Integer entityId);

    SettlementRuleMoz create(Integer entityId, String ruleName, String shareJson, Boolean active);

    SettlementRuleMoz update(Integer id, Integer authEntityId, String ruleName, String shareJson, Boolean active);

    boolean delete(Integer id, Integer authEntityId);
}

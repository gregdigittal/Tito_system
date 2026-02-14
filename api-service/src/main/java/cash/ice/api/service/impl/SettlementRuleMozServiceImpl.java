package cash.ice.api.service.impl;

import cash.ice.api.dto.moz.SettlementRuleMoz;
import cash.ice.api.service.SettlementRuleMozService;
import cash.ice.api.util.JsonValidator;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.sqldb.entity.SettlementRule;
import cash.ice.sqldb.repository.SettlementRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementRuleMozServiceImpl implements SettlementRuleMozService {

    private final SettlementRuleRepository settlementRuleRepository;

    @Override
    public List<SettlementRuleMoz> listByEntityId(Integer entityId) {
        return settlementRuleRepository.findByEntityId(entityId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(timeout = 30)
    public SettlementRuleMoz create(Integer entityId, String ruleName, String shareJson, Boolean active) {
        JsonValidator.requireValidJson(shareJson, "shareJson");
        SettlementRule rule = new SettlementRule()
                .setEntityId(entityId)
                .setRuleName(ruleName != null ? ruleName : "")
                .setShareJson(shareJson != null ? shareJson : "{}")
                .setActive(active != null ? active : true)
                .setCreatedDate(LocalDateTime.now());
        rule = settlementRuleRepository.save(rule);
        return toDto(rule);
    }

    @Override
    @Transactional(timeout = 30)
    public SettlementRuleMoz update(Integer id, Integer authEntityId, String ruleName, String shareJson, Boolean active) {
        SettlementRule rule = settlementRuleRepository.findById(id)
                .orElseThrow(() -> new ICEcashException("Settlement rule not found", ErrorCodes.EC1022));
        if (!rule.getEntityId().equals(authEntityId)) {
            throw new ICEcashException("Settlement rule does not belong to this user", ErrorCodes.EC1077);
        }
        if (ruleName != null) rule.setRuleName(ruleName);
        if (shareJson != null) {
            JsonValidator.requireValidJson(shareJson, "shareJson");
            rule.setShareJson(shareJson);
        }
        if (active != null) rule.setActive(active);
        rule = settlementRuleRepository.save(rule);
        return toDto(rule);
    }

    @Override
    @Transactional(timeout = 30)
    public boolean delete(Integer id, Integer authEntityId) {
        return settlementRuleRepository.findById(id)
                .filter(r -> r.getEntityId().equals(authEntityId))
                .map(r -> {
                    settlementRuleRepository.delete(r);
                    return true;
                })
                .orElseThrow(() -> new ICEcashException("Settlement rule not found or access denied", ErrorCodes.EC1022));
    }

    private SettlementRuleMoz toDto(SettlementRule r) {
        return new SettlementRuleMoz()
                .setId(r.getId())
                .setEntityId(r.getEntityId())
                .setRuleName(r.getRuleName())
                .setShareJson(r.getShareJson())
                .setActive(r.getActive())
                .setCreatedAt(r.getCreatedDate());
    }
}

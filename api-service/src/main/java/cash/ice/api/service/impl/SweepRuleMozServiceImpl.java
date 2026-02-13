package cash.ice.api.service.impl;

import cash.ice.api.service.SweepRuleMozService;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.SweepRule;
import cash.ice.sqldb.repository.AccountRepository;
import cash.ice.sqldb.repository.SweepRuleRepository;
import cash.ice.api.dto.moz.SweepRuleMoz;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SweepRuleMozServiceImpl implements SweepRuleMozService {

    private final SweepRuleRepository sweepRuleRepository;
    private final AccountRepository accountRepository;

    @Override
    public List<SweepRuleMoz> listByAccountId(Integer accountId) {
        return sweepRuleRepository.findByAccountId(accountId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SweepRuleMoz create(Integer accountId, Integer authEntityId, String destinationType, String destinationRef, String triggerType,
                               String scheduleExpression, BigDecimal thresholdAmount, Boolean active) {
        ensureAccountOwnedByEntity(accountId, authEntityId);
        SweepRule rule = new SweepRule()
                .setAccountId(accountId)
                .setDestinationType(destinationType != null ? destinationType : "")
                .setDestinationRef(destinationRef)
                .setTriggerType(triggerType != null ? triggerType : "")
                .setScheduleExpression(scheduleExpression)
                .setThresholdAmount(thresholdAmount)
                .setActive(active != null ? active : true)
                .setCreatedDate(LocalDateTime.now());
        rule = sweepRuleRepository.save(rule);
        return toDto(rule);
    }

    @Override
    @Transactional
    public SweepRuleMoz update(Integer id, Integer authEntityId, String destinationType, String destinationRef,
                               String triggerType, String scheduleExpression, BigDecimal thresholdAmount, Boolean active) {
        SweepRule rule = sweepRuleRepository.findById(id)
                .orElseThrow(() -> new ICEcashException("Sweep rule not found", ErrorCodes.EC1022));
        ensureAccountOwnedByEntity(rule.getAccountId(), authEntityId);
        if (destinationType != null) rule.setDestinationType(destinationType);
        if (destinationRef != null) rule.setDestinationRef(destinationRef);
        if (triggerType != null) rule.setTriggerType(triggerType);
        if (scheduleExpression != null) rule.setScheduleExpression(scheduleExpression);
        if (thresholdAmount != null) rule.setThresholdAmount(thresholdAmount);
        if (active != null) rule.setActive(active);
        rule = sweepRuleRepository.save(rule);
        return toDto(rule);
    }

    @Override
    @Transactional
    public boolean delete(Integer id, Integer authEntityId) {
        return sweepRuleRepository.findById(id)
                .filter(r -> {
                    ensureAccountOwnedByEntity(r.getAccountId(), authEntityId);
                    return true;
                })
                .map(r -> {
                    sweepRuleRepository.delete(r);
                    return true;
                })
                .orElseThrow(() -> new ICEcashException("Sweep rule not found or access denied", ErrorCodes.EC1022));
    }

    private void ensureAccountOwnedByEntity(Integer accountId, Integer entityId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ICEcashException("Account not found", ErrorCodes.EC1022));
        if (!account.getEntityId().equals(entityId)) {
            throw new ICEcashException("Sweep rule account does not belong to this user", ErrorCodes.EC1077);
        }
    }

    private SweepRuleMoz toDto(SweepRule r) {
        return new SweepRuleMoz()
                .setId(r.getId())
                .setAccountId(r.getAccountId())
                .setDestinationType(r.getDestinationType())
                .setDestinationRef(r.getDestinationRef())
                .setTriggerType(r.getTriggerType())
                .setScheduleExpression(r.getScheduleExpression())
                .setThresholdAmount(r.getThresholdAmount())
                .setActive(r.getActive())
                .setCreatedAt(r.getCreatedDate());
    }
}

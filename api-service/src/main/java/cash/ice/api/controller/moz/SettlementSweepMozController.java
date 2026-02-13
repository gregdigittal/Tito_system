package cash.ice.api.controller.moz;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.moz.SettlementRuleMoz;
import cash.ice.api.dto.moz.SweepRuleMoz;
import cash.ice.api.service.AuthUserService;
import cash.ice.api.service.EntityMozService;
import cash.ice.api.service.SettlementRuleMozService;
import cash.ice.api.service.SweepRuleMozService;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Phase 8-5 / 8-6: Settlement rules and sweep rules GraphQL API.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class SettlementSweepMozController {

    private final EntityMozService entityMozService;
    private final AuthUserService authUserService;
    private final SettlementRuleMozService settlementRuleMozService;
    private final SweepRuleMozService sweepRuleMozService;
    private final AccountRepository accountRepository;

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<SettlementRuleMoz> listSettlementRules(@Argument Integer entityId) {
        EntityClass auth = entityMozService.getAuthEntity(getAuthUser(), null);
        Objects.requireNonNull(auth);
        Integer targetEntityId = entityId != null ? entityId : auth.getId();
        if (!targetEntityId.equals(auth.getId())) {
            targetEntityId = auth.getId();
        }
        return settlementRuleMozService.listByEntityId(targetEntityId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public SettlementRuleMoz createSettlementRule(@Argument CreateSettlementRuleInput input) {
        EntityClass auth = entityMozService.getAuthEntity(getAuthUser(), null);
        Objects.requireNonNull(auth);
        if (!input.getEntityId().equals(auth.getId())) {
            throw new IllegalArgumentException("entityId must be the current user's entity");
        }
        return settlementRuleMozService.create(
                input.getEntityId(),
                input.getRuleName(),
                input.getShareJson(),
                input.getActive() != null ? input.getActive() : true);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public SettlementRuleMoz updateSettlementRule(@Argument Integer id, @Argument UpdateSettlementRuleInput input) {
        EntityClass auth = entityMozService.getAuthEntity(getAuthUser(), null);
        Objects.requireNonNull(auth);
        return settlementRuleMozService.update(
                id,
                auth.getId(),
                input.getRuleName(),
                input.getShareJson(),
                input.getActive());
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Boolean deleteSettlementRule(@Argument Integer id) {
        EntityClass auth = entityMozService.getAuthEntity(getAuthUser(), null);
        Objects.requireNonNull(auth);
        return settlementRuleMozService.delete(id, auth.getId());
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<SweepRuleMoz> listSweepRules(@Argument Integer accountId) {
        EntityClass auth = entityMozService.getAuthEntity(getAuthUser(), null);
        Objects.requireNonNull(auth);
        if (accountId == null) {
            return List.of();
        }
        if (accountRepository.findById(accountId).filter(a -> auth.getId().equals(a.getEntityId())).isEmpty()) {
            return List.of();
        }
        return sweepRuleMozService.listByAccountId(accountId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public SweepRuleMoz createSweepRule(@Argument CreateSweepRuleInput input) {
        EntityClass auth = entityMozService.getAuthEntity(getAuthUser(), null);
        Objects.requireNonNull(auth);
        return sweepRuleMozService.create(
                input.getAccountId(),
                auth.getId(),
                input.getDestinationType(),
                input.getDestinationRef(),
                input.getTriggerType(),
                input.getScheduleExpression(),
                input.getThresholdAmount() != null ? BigDecimal.valueOf(input.getThresholdAmount()) : null,
                input.getActive() != null ? input.getActive() : true);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public SweepRuleMoz updateSweepRule(@Argument Integer id, @Argument UpdateSweepRuleInput input) {
        EntityClass auth = entityMozService.getAuthEntity(getAuthUser(), null);
        Objects.requireNonNull(auth);
        return sweepRuleMozService.update(
                id,
                auth.getId(),
                input.getDestinationType(),
                input.getDestinationRef(),
                input.getTriggerType(),
                input.getScheduleExpression(),
                input.getThresholdAmount() != null ? BigDecimal.valueOf(input.getThresholdAmount()) : null,
                input.getActive());
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Boolean deleteSweepRule(@Argument Integer id) {
        EntityClass auth = entityMozService.getAuthEntity(getAuthUser(), null);
        Objects.requireNonNull(auth);
        return sweepRuleMozService.delete(id, auth.getId());
    }

    private AuthUser getAuthUser() {
        return authUserService.getAuthUser();
    }

    public static class CreateSettlementRuleInput {
        private Integer entityId;
        private String ruleName;
        private String shareJson;
        private Boolean active;

        public Integer getEntityId() { return entityId; }
        public void setEntityId(Integer entityId) { this.entityId = entityId; }
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        public String getShareJson() { return shareJson; }
        public void setShareJson(String shareJson) { this.shareJson = shareJson; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    public static class UpdateSettlementRuleInput {
        private String ruleName;
        private String shareJson;
        private Boolean active;

        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        public String getShareJson() { return shareJson; }
        public void setShareJson(String shareJson) { this.shareJson = shareJson; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    public static class CreateSweepRuleInput {
        private Integer accountId;
        private String destinationType;
        private String destinationRef;
        private String triggerType;
        private String scheduleExpression;
        private Double thresholdAmount;
        private Boolean active;

        public Integer getAccountId() { return accountId; }
        public void setAccountId(Integer accountId) { this.accountId = accountId; }
        public String getDestinationType() { return destinationType; }
        public void setDestinationType(String destinationType) { this.destinationType = destinationType; }
        public String getDestinationRef() { return destinationRef; }
        public void setDestinationRef(String destinationRef) { this.destinationRef = destinationRef; }
        public String getTriggerType() { return triggerType; }
        public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
        public String getScheduleExpression() { return scheduleExpression; }
        public void setScheduleExpression(String scheduleExpression) { this.scheduleExpression = scheduleExpression; }
        public Double getThresholdAmount() { return thresholdAmount; }
        public void setThresholdAmount(Double thresholdAmount) { this.thresholdAmount = thresholdAmount; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    public static class UpdateSweepRuleInput {
        private String destinationType;
        private String destinationRef;
        private String triggerType;
        private String scheduleExpression;
        private Double thresholdAmount;
        private Boolean active;

        public String getDestinationType() { return destinationType; }
        public void setDestinationType(String destinationType) { this.destinationType = destinationType; }
        public String getDestinationRef() { return destinationRef; }
        public void setDestinationRef(String destinationRef) { this.destinationRef = destinationRef; }
        public String getTriggerType() { return triggerType; }
        public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
        public String getScheduleExpression() { return scheduleExpression; }
        public void setScheduleExpression(String scheduleExpression) { this.scheduleExpression = scheduleExpression; }
        public Double getThresholdAmount() { return thresholdAmount; }
        public void setThresholdAmount(Double thresholdAmount) { this.thresholdAmount = thresholdAmount; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }
}

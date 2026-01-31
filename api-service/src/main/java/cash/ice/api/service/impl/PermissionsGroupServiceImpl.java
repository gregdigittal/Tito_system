package cash.ice.api.service.impl;

import cash.ice.api.service.EntityMozService;
import cash.ice.api.service.PermissionsGroupService;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.Currency;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.AccountRelationshipRepository;
import cash.ice.sqldb.repository.SecurityGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionsGroupServiceImpl implements PermissionsGroupService {
    private static final String MOZ_SECURITY_GROUP_PREFIX = "MOZ";
    private static final String KEN_SECURITY_GROUP_PREFIX = "FNDS";

    private final SecurityGroupRepository securityGroupRepository;
    private final AccountRelationshipRepository accountRelationshipRepository;
    private final EntityMozService entityMozService;

    @Override
    public void grantMozPermissionsToAccounts(List<Integer> securityGroupIds, EntityClass entity, Account... accounts) {
        grantPermissionsToAccounts(securityGroupIds, entity, MOZ_SECURITY_GROUP_PREFIX, accounts);
    }

    @Override
    public void grantKenPermissionsToAccounts(List<Integer> securityGroupIds, EntityClass entity, Account... accounts) {
        grantPermissionsToAccounts(securityGroupIds, entity, KEN_SECURITY_GROUP_PREFIX, accounts);
    }

    private void grantPermissionsToAccounts(List<Integer> securityGroupIds, EntityClass entity, String securityGroupPrefix, Account... accounts) {
        List<SecurityGroup> securityGroups = securityGroupRepository.findAllById(securityGroupIds.stream().distinct().toList());
        List<Integer> securityGroupIdList = securityGroups.stream().sorted(Comparator.comparing(SecurityGroup::getId)).map(SecurityGroup::getId).toList();
        log.debug("  Grant '{}' security groups to '{}' accounts", securityGroups.stream().map(SecurityGroup::getName).collect(Collectors.joining("', '")),
                Arrays.stream(accounts).map(Account::getId).map(String::valueOf).collect(Collectors.joining(", ")));
        Arrays.stream(accounts).forEach(account ->
                accountRelationshipRepository.save(new AccountRelationship()
                        .setEntityId(entity.getId())
                        .setPartnerAccountId(account.getId())
                        .setSecurityGroupsMap(Map.of(securityGroupPrefix, securityGroupIdList))
                        .setCreatedDate(Tool.currentDateTime())));
    }

    @Override
    public boolean hasUserMozSecurityGroup(EntityClass entity, Integer securityGroupId) {
        return hasUserSecurityGroup(entity, securityGroupId, MOZ_SECURITY_GROUP_PREFIX, Currency.MZN);
    }

    @Override
    public boolean hasUserKenSecurityGroup(EntityClass entity, Integer securityGroupId) {
        return hasUserSecurityGroup(entity, securityGroupId, KEN_SECURITY_GROUP_PREFIX, Currency.KES);
    }

    private boolean hasUserSecurityGroup(EntityClass entity, Integer securityGroupId, String securityGroupPrefix, String currencyCode) {
        Account primaryAccount = entityMozService.getAccount(entity, AccountType.PRIMARY_ACCOUNT, currencyCode);
        AccountRelationship accountRelationship = accountRelationshipRepository.findByEntityIdAndPartnerAccountId(entity.getId(), primaryAccount.getId()).orElse(null);
        if (accountRelationship != null) {
            Object groupIds = accountRelationship.getSecurityGroups().get(securityGroupPrefix);
            if (groupIds instanceof List<?>) {
                return ((List<?>) groupIds).contains(securityGroupId);
            } else if (groupIds instanceof Integer) {
                return Objects.equals(groupIds, securityGroupId);
            }
        }
        return false;
    }

    @Override
    public void validateUserMozSecurityGroup(EntityClass entity, Integer securityGroupId) {
        if (!hasUserMozSecurityGroup(entity, securityGroupId)) {
            throw new ICEcashException(String.format("User does not have needed '%s' security group", securityGroupId), ErrorCodes.EC1078);
        }
    }

    @Override
    public void validateUserKenSecurityGroup(EntityClass entity, Integer securityGroupId) {
        if (!hasUserKenSecurityGroup(entity, securityGroupId)) {
            throw new ICEcashException(String.format("User does not have needed '%s' security group", securityGroupId), ErrorCodes.EC1078);
        }
    }
}

package cash.ice.api.service.impl;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.entity.backoffice.StaffMember;
import cash.ice.api.errors.ForbiddenException;
import cash.ice.api.errors.UnexistingUserException;
import cash.ice.api.repository.backoffice.StaffMemberRepository;
import cash.ice.api.service.PermissionsService;
import cash.ice.common.error.ICEcashException;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.AccountRelationshipRepository;
import cash.ice.sqldb.repository.AccountRepository;
import cash.ice.sqldb.repository.EntityRepository;
import cash.ice.sqldb.repository.SecurityGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static cash.ice.common.error.ErrorCodes.*;
import static cash.ice.sqldb.entity.AuthorisationType.DUAL_AND;

@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionsServiceImpl implements PermissionsService {
    private static final String RELEASE_A = "RELEASE_A";
    private static final String RELEASE_B = "RELEASE_B";
    private static final String BULKPAY = "BULKPAY";
    private static final String BULKPAY_LOAD = "BULKPAY_LOAD";

    private final EntityRepository entityRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final AccountRepository accountRepository;
    private final AccountRelationshipRepository accountRelationshipRepository;
    private final SecurityGroupRepository securityGroupRepository;

    @Override
    public boolean checkReadRights(AuthUser authUser, Integer accountId) {
        EntityClass authEntity = getAuthEntity(authUser);
        return checkRights(BULKPAY, authEntity, accountId, authUser.getRealm());
    }

    @Override
    public boolean checkWriteRights(AuthUser authUser, Integer accountId) {
        EntityClass authEntity = getAuthEntity(authUser);
        return checkRights(BULKPAY_LOAD, authEntity, accountId, authUser.getRealm());
    }

    @Override
    public boolean checkWriteRights(EntityClass entityClass, Integer accountId, String realm) {
        return checkRights(BULKPAY_LOAD, entityClass, accountId, realm);
    }

    boolean checkRights(String role, EntityClass authEntity, Integer accountId, String realm) {
        Account account = accountRepository.findById(accountId).orElseThrow(() ->
                new ICEcashException(String.format("Account with id=%s is absent", accountId), EC1022));
        List<String> rights = getRights(authEntity, account, realm);
        return rights.contains(role);
    }

    @Override
    public EntityClass getAuthEntity(AuthUser authUser) {
        if (authUser == null || authUser.getPrincipal() == null) {
            throw new UnexistingUserException();
        }
        return entityRepository.findByKeycloakId(authUser.getPrincipal()).orElseThrow(() ->
                new UnexistingUserException(authUser.getPrincipal()));
    }

    @Override
    public StaffMember getAuthStaffMember(AuthUser authUser) {
        if (authUser == null || authUser.getPrincipal() == null) {
            throw new UnexistingUserException();
        }
        return staffMemberRepository.findByKeycloakId(authUser.getPrincipal()).orElseThrow(() ->
                new UnexistingUserException(authUser.getPrincipal()));
    }

    @Override
    public List<String> getRights(EntityClass authEntity, Account account, String realm) {
        AccountRelationship relationship = accountRelationshipRepository.findByEntityIdAndPartnerAccountId(
                authEntity.getId(), account.getId()).orElse(null);
        Integer securityGroupId = relationship != null ? relationship.getSecurityGroupFor(realm) : null;
        if (securityGroupId == null) {
            throw new ForbiddenException("in realm: %s to work with account: %s".formatted(realm, account.getId()));
        }
        SecurityGroup securityGroup = securityGroupRepository.findById(securityGroupId)
                .orElseThrow(() -> new ICEcashException("Wrong security group: " + securityGroupId, EC1028));
        return securityGroup.getRights().stream().map(SecurityRight::getName).toList();
    }

    @Override
    public void checkApprovePermissions(Account account, List<String> rights, String realm, Integer prevApprover) {
        boolean releaseA = rights.contains(RELEASE_A);
        boolean releaseB = rights.contains(RELEASE_B);
        if (!releaseA && !releaseB) {
            throw new ForbiddenException("to approve payment");
        }
        AuthorisationType authorisationType = account.getAuthorisationType();
        if (prevApprover != null) {
            if (prevApprover.equals(account.getEntityId())) {
                throw new ForbiddenException(", the user has already approved the payment");
            }
            if (authorisationType == DUAL_AND) {
                EntityClass prevApproverEntity = entityRepository.findById(prevApprover).orElseThrow(() ->
                        new ICEcashException(String.format("Approver with id=%s is absent", prevApprover), EC1023));
                List<String> prevApproverRights = getRights(prevApproverEntity, account, realm);
                if (!prevApproverRights.contains(RELEASE_A) && !releaseA) {
                    throw new ForbiddenException("to approve payment, must be in group A");
                } else if (!prevApproverRights.contains(RELEASE_B) && !releaseB) {
                    throw new ForbiddenException("to approve payment, must be in group B");
                }
            }
        }
    }
}

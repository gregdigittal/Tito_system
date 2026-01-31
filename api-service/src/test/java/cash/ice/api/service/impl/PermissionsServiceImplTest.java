package cash.ice.api.service.impl;

import cash.ice.api.errors.ForbiddenException;
import cash.ice.api.repository.backoffice.StaffMemberRepository;
import cash.ice.common.error.ErrorCodes;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.AccountRelationshipRepository;
import cash.ice.sqldb.repository.AccountRepository;
import cash.ice.sqldb.repository.EntityRepository;
import cash.ice.sqldb.repository.SecurityGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionsServiceImplTest {
    private static final int ACCOUNT_ID = 1;
    private static final int AUTH_ENTITY_ID = 2;
    private static final int SECURITY_GROUP = 4;
    private static final int PREV_APPROVER = 5;
    private static final String REALM = "ONLINE";

    @Mock
    private EntityRepository entityRepository;
    @Mock
    private StaffMemberRepository staffMemberRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountRelationshipRepository accountRelationshipRepository;
    @Mock
    private SecurityGroupRepository securityGroupRepository;

    private PermissionsServiceImpl service;

    @BeforeEach
    void init() {
        service = new PermissionsServiceImpl(entityRepository, staffMemberRepository, accountRepository,
                accountRelationshipRepository, securityGroupRepository);
    }

    @Test
    void testCheckRights() {
        String role = "SOME_ROLE";
        EntityClass authEntity = new EntityClass().setId(AUTH_ENTITY_ID);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(new Account().setId(ACCOUNT_ID)));
        when(accountRelationshipRepository.findByEntityIdAndPartnerAccountId(AUTH_ENTITY_ID, ACCOUNT_ID)).thenReturn(
                Optional.of(new AccountRelationship().setSecurityGroupsMap(Map.of("ONLINE", SECURITY_GROUP))));
        when(securityGroupRepository.findById(SECURITY_GROUP)).thenReturn(Optional.of(new SecurityGroup()
                .setRights(Set.of(new SecurityRight().setName("RELEASE_A"), new SecurityRight().setName(role)))));

        boolean actualResponse = service.checkRights(role, authEntity, ACCOUNT_ID, REALM);
        assertThat(actualResponse).isTrue();
    }

    @Test
    void testCheckApprovePermissionsSingle() {
        List<String> rights = List.of("RELEASE_A");
        Account account = new Account().setId(ACCOUNT_ID).setAuthorisationType(AuthorisationType.SINGLE);
        service.checkApprovePermissions(account, rights, REALM, null);
    }

    @Test
    void testCheckApprovePermissionsSingleFail() {
        List<String> rights = List.of("BULKPAY", "BULKPAY_LOAD");
        Account account = new Account().setId(ACCOUNT_ID).setAuthorisationType(AuthorisationType.SINGLE);
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> service.checkApprovePermissions(
                account, rights, REALM, null));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.EC1021);
    }

    @Test
    void testCheckApprovePermissionsDualOr() {
        List<String> rights = List.of("RELEASE_A");
        Account account = new Account().setId(ACCOUNT_ID).setAuthorisationType(AuthorisationType.DUAL_OR).setEntityId(555);
        service.checkApprovePermissions(account, rights, REALM, PREV_APPROVER);
    }

    @Test
    void testCheckApprovePermissionsDualOrSameApprover() {
        List<String> rights = List.of("RELEASE_B");
        Account account = new Account().setId(ACCOUNT_ID).setAuthorisationType(AuthorisationType.DUAL_OR).setEntityId(PREV_APPROVER);
        assertThrows(ForbiddenException.class, () -> service.checkApprovePermissions(
                account, rights, REALM, PREV_APPROVER));
    }

    @Test
    void testCheckApprovePermissionsDualAnd() {
        List<String> rights = List.of("RELEASE_B");
        mockDualPermissions(List.of("RELEASE_A"));
        Account account = new Account().setId(ACCOUNT_ID).setAuthorisationType(AuthorisationType.DUAL_AND).setEntityId(555);
        service.checkApprovePermissions(account, rights, REALM, PREV_APPROVER);
    }

    @Test
    void testCheckApprovePermissionsDualAndFail() {
        List<String> rights = List.of("RELEASE_A");
        mockDualPermissions(List.of("RELEASE_A"));
        Account account = new Account().setId(ACCOUNT_ID).setAuthorisationType(AuthorisationType.DUAL_AND).setEntityId(555);
        assertThrows(ForbiddenException.class, () -> service.checkApprovePermissions(
                account, rights, REALM, PREV_APPROVER));
    }

    private void mockDualPermissions(List<String> prevApproverRights) {
        when(entityRepository.findById(PREV_APPROVER)).thenReturn(Optional.of(new EntityClass().setId(PREV_APPROVER)));
        when(accountRelationshipRepository.findByEntityIdAndPartnerAccountId(PREV_APPROVER, ACCOUNT_ID)).thenReturn(
                Optional.of(new AccountRelationship().setSecurityGroupsMap(Map.of("ONLINE", SECURITY_GROUP))));
        when(securityGroupRepository.findById(SECURITY_GROUP)).thenReturn(Optional.of(new SecurityGroup()
                .setRights(prevApproverRights.stream().map(role -> new SecurityRight().setName(role))
                        .collect(Collectors.toSet()))));
    }
}
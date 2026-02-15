package cash.ice.api.controller.backoffice;

import cash.ice.api.dto.*;
import cash.ice.api.entity.backoffice.StaffMember;
import cash.ice.api.service.*;
import cash.ice.api.util.MappingUtil;
import cash.ice.api.util.HttpUtils;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.LoginStatus;
import cash.ice.sqldb.entity.MfaType;
import cash.ice.sqldb.entity.SecurityGroup;
import cash.ice.sqldb.repository.SecurityGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

import static cash.ice.api.service.impl.KeycloakServiceImpl.logToken;

@Controller
@RequiredArgsConstructor
@Slf4j
public class StaffController {
    private final StaffMemberService staffMemberService;
    private final StaffMemberLoginService staffMemberLoginService;
    private final KeycloakService backofficeKeycloakService;
    private final AuthUserService authUserService;
    private final EntityService entityService;
    private final SecurityGroupRepository securityGroupRepository;

    @QueryMapping
    @PreAuthorize("@StaffProperties.securityDisabled || hasRole('ROLE_BACKOFFICE')")
    public StaffMember staffMember(@Argument Integer id, @Argument ConfigInput config) {
        log.info("> GET staff member: {}, config: {}", id != null ? id : "current", config);
        StaffMember authStaffMember = staffMemberService.getAuthStaffMember(getAuthUser(), config);
        return id == null ? authStaffMember : staffMemberService.getStaffMemberById(id);
    }

    @QueryMapping
    @PreAuthorize("@StaffProperties.securityDisabled || hasRole('ROLE_BACKOFFICE')")
    public Iterable<StaffMember> searchStaffMembers(@Argument String searchText, @Argument LoginStatus status,
                                                    @Argument int page, @Argument int size, @Argument SortInput sort, @Argument ConfigInput config) {
        log.info("> Search staff members: {}, status: {}, (pg={},sz={},sr={}), config: {}", searchText, status, page, size, sort, config);
        return staffMemberService.searchStaffMembers(searchText, status, page, size, sort);
    }

    @SchemaMapping(typeName = "StaffMember", field = "mfaQrCode")
    public String mfaQrCode(StaffMember staffMember) {
        return staffMemberLoginService.getMfaQrCode(staffMember);
    }

    @MutationMapping
    public StaffMember newStaffMember(@Argument StaffMember staffMember, @Argument String url, @Argument boolean sendEmail) {
        log.info("> Create new staff member: {}, url: {}, sendEmail: {}", staffMember, url, sendEmail);
        return staffMemberService.createNewStaffMember(staffMember, url, sendEmail);
    }

    @MutationMapping
    public StaffMember activateNewStaffMember(@Argument String key, @Argument String newPassword) {
        log.info("> Activate new staff member: key={}", key);
        return staffMemberLoginService.activateNewStaffMember(key, newPassword);
    }

    @MutationMapping
    public StaffMember registerStaffMember(@Argument StaffMember staffMember, @Argument String password) {
        log.info("> Register new staff member: {}", staffMember);
        return staffMemberLoginService.registerStaffMember(staffMember, password);
    }

    @MutationMapping
    @PreAuthorize("@StaffProperties.securityDisabled || hasRole('ROLE_BACKOFFICE')")
    public StaffMember updateStaffMember(@Argument Integer id, @Argument StaffMember staffMember, @Argument ConfigInput config, @Argument boolean sendEmail) {
        log.info("> Update staff member: {}, staffMember: {}, sendEmail: {}, config: {}", id != null ? id : "current", staffMember, sendEmail, config);
        StaffMember updater = staffMemberService.getAuthStaffMember(getAuthUser(), config);
        StaffMember updating = id != null ? staffMemberService.getStaffMemberById(id) : updater;
        return staffMemberService.updateStaffMember(updating, staffMember, updater, config, sendEmail);
    }

    @MutationMapping
    @PreAuthorize("@StaffProperties.securityDisabled || hasRole('ROLE_BACKOFFICE')")
    public StaffMember updateStaffMemberMsisdn(@Argument String msisdn, @Argument ConfigInput config) {
        log.info("> Update staff member msisdn: {}, config: {}", msisdn, config);
        StaffMember staffMember = staffMemberService.getAuthStaffMember(getAuthUser(), config);
        return staffMemberService.updateMsisdn(staffMember, msisdn);
    }

    @MutationMapping
    @PreAuthorize("@StaffProperties.securityDisabled || hasRole('ROLE_BACKOFFICE')")
    public StaffMember updateStaffMemberMfaType(@Argument MfaType mfaType, @Argument ConfigInput config) {
        log.info("> Update staff member mfaType: {}, config: {}", mfaType, config);
        StaffMember staffMember = staffMemberService.getAuthStaffMember(getAuthUser(), config);
        return staffMemberService.updateMfaType(staffMember, mfaType);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ROLE_BACKOFFICE')")
    public StaffMember updateStaffMemberPassword(@Argument String oldPassword, @Argument String newPassword) {
        AuthUser authUser = getAuthUser();
        log.info("> Update current staff member password, authUser: {}", authUser);
        return staffMemberLoginService.updateStaffMemberPassword(authUser, oldPassword, newPassword);
    }

    @MutationMapping
    @PreAuthorize("@StaffProperties.securityDisabled || hasRole('ROLE_BACKOFFICE')")
    public StaffMember updateStaffMemberLoginStatus(@Argument String email, @Argument LoginStatus loginStatus) {
        log.info("> Update staff member login status: {} for {}", loginStatus, email);
        return staffMemberLoginService.updateStaffMemberLoginStatus(email, loginStatus);
    }

    @MutationMapping
    @PreAuthorize("@StaffProperties.securityDisabled || hasRole('ROLE_BACKOFFICE')")
    public StaffMember generateNewBackupCodesForId(@Argument Integer id) {
        log.info("> Generate new backup codes for: {}", id);
        return staffMemberService.generateNewBackupCodes(id);
    }

    @MutationMapping
    @PreAuthorize("@StaffProperties.securityDisabled || hasRole('ROLE_BACKOFFICE')")
    public StaffMember generateNewBackupCodes() {
        AuthUser authUser = getAuthUser();
        log.info("> Generate new backup codes, authUser: {}", authUser);
        return staffMemberService.generateNewBackupCodes(authUser);
    }

    @MutationMapping
    @PreAuthorize("@StaffProperties.securityDisabled || hasRole('ROLE_BACKOFFICE')")
    public StaffMember deleteStaffMember(@Argument Integer id) {
        log.info("> Delete staff member: {}", id);
        return staffMemberService.deleteStaffMember(id);
    }

    @MutationMapping
    @PreAuthorize("@StaffProperties.securityDisabled || hasRole('ROLE_BACKOFFICE')")
    public StaffMember deleteCurrentStaffMember() {
        AuthUser authUser = getAuthUser();
        log.info("> Delete current staff member: {}", authUser);
        return staffMemberService.deleteStaffMember(authUser);
    }

    @MutationMapping
    @PreAuthorize("@StaffProperties.securityDisabled || hasRole('ROLE_BACKOFFICE')")
    public EntityClass updateEntity(@Argument Integer entityId, @Argument EntityClass details) {
        StaffMember staffMember = staffMemberService.getAuthStaffMember(getAuthUser(), null);
        log.info("> Update entity: (id={}), staffMember: (id={}), details: {}", entityId, (staffMember != null ? staffMember.getId() : null), details);
        return entityService.updateEntity(entityId, details, staffMember);
    }

    @MutationMapping
    public LoginResponse loginStaffMember(@Argument LoginEntityRequest request) {
        log.info("> Login staff member: {}", request);
        return staffMemberLoginService.loginStaffMember(request);
    }

    @MutationMapping
    public LoginResponse enterLoginMfaCode(@Argument LoginMfaRequest mfaRequest) {
        log.info("> Enter login mfa code request: {}", mfaRequest);
        return staffMemberLoginService.enterLoginMfaCode(mfaRequest);
    }

    @MutationMapping
    public LoginResponse enterLoginBackupCode(@Argument LoginMfaRequest mfaRequest) {
        log.info("> Enter login mfa backup code request: {}", mfaRequest);
        return staffMemberLoginService.enterLoginMfaBackupCode(mfaRequest);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ROLE_BACKOFFICE')")
    public boolean checkStaffMemberTotpCode(@Argument String totpCode) {
        AuthUser authUser = getAuthUser();
        log.info("> Check staff member TOTP code: {}, authUser: {}", totpCode, authUser);
        return staffMemberLoginService.checkTotpCode(authUser, totpCode);
    }

    @MutationMapping
    public LoginResponse refreshStaffMemberAccessToken(@Argument String refreshToken) {
        log.info("> Refresh staff access token: ({}){}...{}, user: {}", Tool.checksum(refreshToken), Tool.substrFirst(30, refreshToken), Tool.substrLast(30, refreshToken), getAuthUser());
        AccessTokenResponse token = backofficeKeycloakService.refreshToken(refreshToken, null, null);
        return LoginResponse.success(logToken(token));
    }

    @MutationMapping
    public LoginResponse invalidateStaffMemberRefreshToken(@Argument String refreshToken) {
        log.info("> Invalidate staff refresh token: ({}){}...{}, user: {}", Tool.checksum(refreshToken), Tool.substrFirst(30, refreshToken), Tool.substrLast(30, refreshToken), getAuthUser());
        backofficeKeycloakService.invalidateRefreshToken(refreshToken, null, null);
        return LoginResponse.success(null);
    }

    @MutationMapping
    public boolean resendOtpCode(@Argument String username) {
        log.info("> Resend OTP code request: {}", username);
        return staffMemberLoginService.resendOtpCode(username);
    }

    @MutationMapping
    public boolean forgotStaffMemberPassword(@Argument String email, @Argument String url, @Argument boolean sendEmail) {
        log.info("> Forgot staff member password request for: {}, url: {}, sendEmail: {}", email, url, sendEmail);
        return staffMemberLoginService.forgotPassword(email, url, sendEmail, HttpUtils.getRequestIP());
    }

    @MutationMapping
    public StaffMember resetStaffMemberPassword(@Argument String key, @Argument String newPassword) {
        log.info("> Reset staff member password, key: {}", key);
        return staffMemberLoginService.resetStaffMemberPassword(key, newPassword);
    }

    @BatchMapping(typeName = "StaffMember", field = "securityGroup")
    public Map<StaffMember, SecurityGroup> staffMemberSecurityGroup(List<StaffMember> staffMembers) {
        return MappingUtil.itemsToCategoriesMap(staffMembers, StaffMember::getSecurityGroupId,
                SecurityGroup::getId, securityGroupRepository);
    }

    protected AuthUser getAuthUser() {
        return authUserService.getAuthUser();
    }
}

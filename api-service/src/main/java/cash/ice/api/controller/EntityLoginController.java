package cash.ice.api.controller;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.LoginEntityRequest;
import cash.ice.api.dto.LoginMfaRequest;
import cash.ice.api.dto.LoginResponse;
import cash.ice.api.service.AuthUserService;
import cash.ice.api.service.EntityLoginService;
import cash.ice.api.service.KeycloakService;
import cash.ice.api.util.HttpUtils;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.LoginStatus;
import cash.ice.sqldb.entity.MfaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import static cash.ice.api.service.impl.KeycloakServiceImpl.logToken;

@Controller
@RequiredArgsConstructor
@Slf4j
public class EntityLoginController {
    private final EntityLoginService entityLoginService;
    private final AuthUserService authUserService;
    private final KeycloakService keycloakService;

    @MutationMapping
    @PreAuthorize("@EntitiesProperties.securityDisabled || isAuthenticated()")
    public EntityClass updateEntityLoginStatus(@Argument String username, @Argument LoginStatus loginStatus) {
        log.info("> Update entity login status: {} for {}", loginStatus, username);
        return entityLoginService.updateEntityLoginStatus(username, loginStatus);
    }

    @MutationMapping
    public LoginResponse loginEntity(@Argument LoginEntityRequest request) {
        log.info("> Entity Login request: " + request);
        return entityLoginService.makeLogin(request);
    }

    @MutationMapping
    public LoginResponse enterEntityLoginMfaCode(@Argument LoginMfaRequest mfaRequest) {
        log.info("> Enter login mfa code request: " + mfaRequest);
        return entityLoginService.enterLoginMfaCode(mfaRequest);
    }

    @MutationMapping
    public LoginResponse enterEntityLoginBackupCode(@Argument LoginMfaRequest mfaRequest) {
        log.info("> Enter login mfa backup code request: " + mfaRequest);
        return entityLoginService.enterLoginMfaBackupCode(mfaRequest);
    }

    @MutationMapping
    public boolean checkEntityTotpCode(@Argument String totpCode) {
        AuthUser authUser = authUserService.getAuthUser();
        log.info("> Check entity TOTP code: {}, authUser: {}", totpCode, authUser);
        return entityLoginService.checkTotpCode(authUser, totpCode);
    }

    @MutationMapping
    public boolean resendEntityOtpCode(@Argument String username) {
        log.info("> Resend OTP code request: " + username);
        return entityLoginService.resendOtpCode(username);
    }

    @MutationMapping
    public boolean forgotEntityPassword(@Argument String email, @Argument boolean sendEmail) {
        log.info("> Forgot password request for: {}, sendEmail: {}", email, sendEmail);
        return entityLoginService.forgotPassword(email, sendEmail, HttpUtils.getRequestIP());
    }

    @MutationMapping
    public EntityClass resetEntityPassword(@Argument String key, @Argument String newPassword) {
        log.info("> Reset entity password, key: {}", key);
        return entityLoginService.resetEntityPassword(key, newPassword);
    }

    @MutationMapping
    public EntityClass updateEntityMfa(@Argument Integer id, @Argument MfaType mfaType) {
        log.info("> update MFA type: {} for entity: {}", mfaType, id);
        return entityLoginService.updateEntityMfa(id, mfaType);
    }

    @MutationMapping
    public EntityClass generateNewEntityPassword(@Argument Integer id) {
        log.info("> Generate new password for entity: " + id);
        return entityLoginService.generateNewEntityPassword(id);
    }

    @MutationMapping
    public LoginResponse refreshAccessToken(@Argument String refreshToken) {
        log.info("> Refresh access token: ({}){}...{}, user: {}", Tool.checksum(refreshToken), Tool.substrFirst(30, refreshToken), Tool.substrLast(30, refreshToken), authUserService.getAuthUser());
        AccessTokenResponse token = keycloakService.refreshToken(refreshToken, null, null);
        return LoginResponse.success(logToken(token));
    }

    @MutationMapping
    public LoginResponse invalidateRefreshToken(@Argument String refreshToken) {
        log.info("> Invalidate refresh token: ({}){}...{}, user: {}", Tool.checksum(refreshToken), Tool.substrFirst(30, refreshToken), Tool.substrLast(30, refreshToken), authUserService.getAuthUser());
        keycloakService.invalidateRefreshToken(refreshToken, null, null);
        return LoginResponse.success(null);
    }

    @SchemaMapping(typeName = "Entity", field = "mfaQrCode")
    public String mfaQrCode(EntityClass entityClass) {
        return entityLoginService.getMfaQrCode(entityClass);
    }

    @MutationMapping
    @PreAuthorize("@EntitiesProperties.securityDisabled || isAuthenticated()")
    public EntityClass updateEntityPassword(@Argument String oldPassword, @Argument String newPassword) {
        AuthUser authUser = authUserService.getAuthUser();
        log.info("> Update current entity password, authUser: {}", authUser);
        return entityLoginService.updateEntityPassword(authUser, oldPassword, newPassword);
    }
}

package cash.ice.api.service.impl;

import cash.ice.api.config.property.MfaProperties;
import cash.ice.api.dto.LoginData;
import cash.ice.api.dto.LoginResponse;
import cash.ice.api.errors.LockLoginException;
import cash.ice.api.errors.UnexistingUserException;
import cash.ice.api.repository.LoginDataStore;
import cash.ice.api.service.KeycloakService;
import cash.ice.api.service.MfaService;
import cash.ice.api.service.NotificationService;
import cash.ice.api.service.SecurityPvvService;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.MfaType;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.TimeProvider;
import dev.samstevens.totp.util.Utils;
import jakarta.ws.rs.NotAuthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static cash.ice.api.service.impl.KeycloakServiceImpl.logToken;

@Service
@Slf4j
@RequiredArgsConstructor
public class MfaServiceImpl implements MfaService {
    private final SecurityPvvService securityPvvService;
    private final NotificationService notificationService;
    private final KeycloakService keycloakService;
    private final LoginDataStore loginDataStore;
    private final SecretGenerator secretGenerator;
    private final CodeVerifier verifier;
    private final TimeProvider timeProvider;
    private final CodeGenerator codeGenerator;
    private final QrDataFactory qrDataFactory;
    private final QrGenerator qrGenerator;
    private final RecoveryCodeGenerator recoveryCodeGenerator;

    @Override
    public LoginResponse handleLogin(String login, AccessTokenResponse accessToken, MfaType mfaType, String mfaSecretCode, String msisdn, MfaProperties mfaProperties) throws LockLoginException {
        LoginData loginData = findUserLoginData(login, true);
        if (accessToken == null) {
            return handleFailedLoginAttempt(loginData, "password", mfaProperties);
        } else if (mfaType == null) {
            return LoginResponse.success(logToken(accessToken));
        } else {
            loginData.setMfaType(mfaType)
                    .setMfaPinKey(Tool.generateDigits(EntityClass.INTERNAL_NUM_LENGTH, true))
                    .setMfaSecretCode(mfaSecretCode);
            Runnable afterSaveAction = () -> {
            };
            if (loginData.getMfaType() == MfaType.OTP) {
                String pin = Tool.generateDigits(mfaProperties.getOtpDigitsAmount(), false);
                loginData.setOtpPvv(securityPvvService.acquirePvv(loginData.getMfaPinKey(), pin));
                if (mfaProperties.isOtpSendSms()) {
                    afterSaveAction = () -> notificationService.sendSmsPinCode(pin, msisdn);
                }
            }
            invalidateAccessTokenIfNeed(loginData.getToken());
            loginDataStore.save(loginData
                    .setToken(accessToken)
                    .setTokenReceiveTime(Instant.now())
                    .setTokenExpireTime(Instant.now().plus(mfaProperties.getAccessTokenExpiration())));
            afterSaveAction.run();
            return LoginResponse.successNeedMfa(login, loginData.getMfaType(), msisdn);
        }
    }

    @Override
    public LoginResponse enterMfaCode(String login, String mfaCode, MfaProperties mfaProperties) throws LockLoginException {
        LoginData loginData = findUserLoginData(login, false);
        if (loginData.isTokenAbsentOrExpired()) {
            throw new ICEcashException("Access token is expired", ErrorCodes.EC1038);
        }
        if (checkMfaCode(loginData, mfaCode, mfaProperties)) {
            return handleSuccessfulLogin(loginData);
        } else {
            return handleFailedLoginAttempt(loginData, loginData.getMfaType() + " code", mfaProperties);
        }
    }

    @Override
    public LoginResponse enterBackupCode(String login, List<String> activeBackupCodes, String enteredCode, MfaProperties mfaProperties) throws LockLoginException {
        LoginData loginData = findUserLoginData(login, true);
        if (activeBackupCodes != null && activeBackupCodes.contains(enteredCode)) {
            invalidateAccessTokenIfNeed(loginData.getToken());
            return handleSuccessfulLogin(loginData);
        } else {
            return handleFailedLoginAttempt(loginData, "Backup code", mfaProperties);
        }
    }

    @Override
    public void resendOtpCode(String login, String msisdn, MfaProperties mfaProperties) {
        LoginData loginData = findUserLoginData(login, false);
        if (loginData.getOtpPvv() != null) {
            String pin = securityPvvService.restorePin(loginData.getMfaPinKey(), loginData.getOtpPvv(), mfaProperties.getOtpDigitsAmount());
            if (mfaProperties.isOtpSendSms()) {
                notificationService.sendSmsPinCode(pin, msisdn);
            }
        } else {
            throw new ICEcashException("No OTP code is assigned to user", ErrorCodes.EC1033);
        }
    }

    @Override
    public void cleanupLoginData(String login) {
        loginDataStore.save(findUserLoginData(login, true)
                .cleanup(true));
    }

    @Override
    public String getTotpCode(String mfaSecretCode) {
        long currentBucket = Math.floorDiv(timeProvider.getTime(), 30);
        try {
            return codeGenerator.generate(mfaSecretCode, currentBucket);
        } catch (CodeGenerationException e) {
            log.info(e.getMessage(), e);
            throw new ICEcashException(e.getMessage(), ErrorCodes.EC1004);
        }
    }

    @Override
    public boolean checkTotpCode(String mfaSecretCode, String totpCode) {
        return verifier.isValidCode(mfaSecretCode, totpCode);
    }

    @Override
    public String createForgotPasswordKey(String login) {
        LoginData loginData = findUserLoginData(login, true);
        loginDataStore.save(loginData.setForgotPasswordKey(secretGenerator.generate()));
        return loginData.getForgotPasswordKey();
    }

    @Override
    public String lookupLoginByForgotPasswordKey(String forgotPasswordKey) {
        LoginData loginData = loginDataStore.findByForgotPasswordKey(forgotPasswordKey).orElseThrow(() ->
                new ICEcashException("Unknown key provided", ErrorCodes.EC1034));
        loginDataStore.save(loginData.setForgotPasswordKey(null));
        return loginData.getLogin();
    }

    @Override
    public String getForgotPasswordKey(String login) {
        LoginData loginData = findUserLoginData(login, true);
        return loginData.getForgotPasswordKey();
    }

    @Override
    public String getQrCode(String appTitle, String email, String mfaSecretCode, MfaProperties mfaProperties) {
        QrData qrData = qrDataFactory.newBuilder()
                .issuer(appTitle)
                .label(email)
                .secret(mfaSecretCode)
                .digits(mfaProperties.getTotpQrCodeDigits())
                .period(mfaProperties.getTotpQrCodePeriod())
                .build();
        try {
            return Utils.getDataUriForImage(
                    qrGenerator.generate(qrData),
                    qrGenerator.getImageMimeType());
        } catch (QrGenerationException e) {
            throw new ICEcashException("error generating TOTP QR code", ErrorCodes.EC1041, e);
        }
    }

    @Override
    public List<String> generateBackupCodes(MfaProperties mfaProperties) {
        return List.of(recoveryCodeGenerator.generateCodes(mfaProperties.getBackupCodesAmount()));
    }

    @Override
    public String generateSecretCode() {
        return secretGenerator.generate();
    }

    @Override
    public void cleanupExpiredTokensTask() {
        Instant now = Instant.now();
        List<LoginData> expiredTokenUsers = loginDataStore.findAllByTokenExpireTimeIsBefore(now);
        log.info("Cleanup {} staff members expired tokens older than: {}", expiredTokenUsers.size(), now);
        if (!expiredTokenUsers.isEmpty()) {
            expiredTokenUsers.forEach(loginData -> loginData.cleanup(false));
            loginDataStore.saveAll(expiredTokenUsers);
        }
    }

    private LoginData findUserLoginData(String login, boolean createIfAbsent) {
        return loginDataStore.findByLogin(login).orElseGet(() -> {
            if (createIfAbsent) {
                return new LoginData().setLogin(login);
            } else {
                throw new UnexistingUserException(login);
            }
        });
    }

    private LoginResponse handleSuccessfulLogin(LoginData loginData) {
        AccessTokenResponse token = loginData.getToken();
        MfaType mfaType = loginData.getMfaType();
        loginDataStore.save(loginData.cleanup(true));
        return LoginResponse.success(logToken(token)).setMfaType(mfaType);
    }

    private LoginResponse handleFailedLoginAttempt(LoginData loginData, String type, MfaProperties mfaProperties) throws LockLoginException {
        loginData.addFailedLoginAttempt(mfaProperties.getMaxWrongLoginAttempts());
        log.debug("Wrong {}, account: {}, failAttempts: {}", type, loginData.getLogin(), loginData.getFailedLoginAttempts());
        loginDataStore.save(loginData);
        if (isMaxLoginAttemptsViolated(loginData, mfaProperties)) {
            throw new LockLoginException(new NotAuthorizedException("Wrong " + type));
        }
        throw new NotAuthorizedException("Wrong " + type);
    }

    private void invalidateAccessTokenIfNeed(AccessTokenResponse accessToken) {
        if (accessToken != null) {
            try {
                log.debug("Another token is available, invalidating");
                keycloakService.invalidateRefreshToken(accessToken.getRefreshToken(), null, null);
            } catch (Exception e) {
                // token already invalidated, do nothing
            }
        }
    }

    private boolean isMaxLoginAttemptsViolated(LoginData loginData, MfaProperties mfaProperties) {
        if (loginData.getFailedLoginAttempts().size() >= mfaProperties.getMaxWrongLoginAttempts()) {
            if (mfaProperties.getMaxWrongLoginPeriod() != null) {
                Duration diff = loginData.getDurationBetweenFailedLoginAttempts();
                if (diff.compareTo(mfaProperties.getMaxWrongLoginPeriod()) < 0) {
                    log.debug("Max ({}) login attempts amount within {} reached, period {}, staffMember: {}",
                            mfaProperties.getMaxWrongLoginAttempts(), mfaProperties.getMaxWrongLoginPeriod(), diff, loginData.getLogin());
                    return true;
                }
            } else {
                log.debug("Max ({}) login attempts amount reached, staffMember: {}", mfaProperties.getMaxWrongLoginAttempts(), loginData.getLogin());
                return true;
            }
        }
        return false;
    }

    private boolean checkMfaCode(LoginData loginData, String enteredCode, MfaProperties mfaProperties) {
        switch (loginData.getMfaType()) {
            case OTP -> {
                if (loginData.isOtpCodeExpired(mfaProperties.getOtpCodeExpiration())) {
                    throw new ICEcashException("expired OTP code", ErrorCodes.EC1036);
                }
                String otpPvv = securityPvvService.acquirePvv(loginData.getMfaPinKey(), enteredCode);
                return Objects.equals(otpPvv, loginData.getOtpPvv());
            }
            case TOTP -> {
                return verifier.isValidCode(loginData.getMfaSecretCode(), enteredCode);
            }
            default -> throw new ICEcashException("unknown or unset MFA type", ErrorCodes.EC1037);
        }
    }
}

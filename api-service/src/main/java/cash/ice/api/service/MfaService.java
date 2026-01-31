package cash.ice.api.service;

import cash.ice.api.config.property.MfaProperties;
import cash.ice.api.dto.LoginResponse;
import cash.ice.api.errors.LockLoginException;
import cash.ice.sqldb.entity.MfaType;
import org.keycloak.representations.AccessTokenResponse;

import java.util.List;

public interface MfaService {

    LoginResponse handleLogin(String login, AccessTokenResponse accessToken, MfaType mfaType, String mfaSecretCode, String msisdn, MfaProperties mfaProperties) throws LockLoginException;

    LoginResponse enterMfaCode(String login, String mfaCode, MfaProperties mfaProperties) throws LockLoginException;

    LoginResponse enterBackupCode(String login, List<String> activeBackupCodes, String enteredCode, MfaProperties mfaProperties) throws LockLoginException;

    void resendOtpCode(String login, String msisdn, MfaProperties mfaProperties);

    void cleanupLoginData(String login);

    String getTotpCode(String mfaSecretCode);

    boolean checkTotpCode(String mfaSecretCode, String totpCode);

    String createForgotPasswordKey(String login);

    String lookupLoginByForgotPasswordKey(String forgotPasswordKey);

    String getForgotPasswordKey(String login);

    String getQrCode(String appTitle, String email, String mfaSecretCode, MfaProperties mfaProperties);

    List<String> generateBackupCodes(MfaProperties mfaProperties);

    String generateSecretCode();

    void cleanupExpiredTokensTask();
}

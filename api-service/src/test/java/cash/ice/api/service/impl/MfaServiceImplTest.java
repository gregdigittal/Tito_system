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
import cash.ice.sqldb.entity.MfaType;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.TimeProvider;
import jakarta.ws.rs.NotAuthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.AccessTokenResponse;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MfaServiceImplTest {
    private static final String LOGIN = "test@ice.cash";
    private static final String MFA_SECRET_CODE = "ffab";
    private static final String MSISDN = "2630000000";
    private static final String PVV = "SOME_PVV";
    private static final String MFA_PIN_KEY = "123456";
    private static final String PIN = "1234";
    private static final String FORGOT_PASSWORD_KEY = "ForgotPasswordKey";
    private static final AccessTokenResponse ACCESS_TOKEN = new AccessTokenResponse();

    @Mock
    private SecurityPvvService securityPvvService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private KeycloakService keycloakService;
    @Mock
    private LoginDataStore loginDataStore;
    @Mock
    private SecretGenerator secretGenerator;
    @Mock
    private CodeVerifier verifier;
    @Mock
    private TimeProvider timeProvider;
    @Mock
    private CodeGenerator codeGenerator;
    @Mock
    private QrDataFactory qrDataFactory;
    @Mock
    private QrGenerator qrGenerator;
    @Mock
    private RecoveryCodeGenerator recoveryCodeGenerator;
    @Captor
    private ArgumentCaptor<LoginData> loginDataCaptor;

    private MfaService service;

    @BeforeEach
    void init() {
        service = new MfaServiceImpl(securityPvvService, notificationService, keycloakService, loginDataStore, secretGenerator,
                verifier, timeProvider, codeGenerator, qrDataFactory, qrGenerator, recoveryCodeGenerator);
    }

    @Test
    void testHandleSimpleLogin() throws LockLoginException {
        MfaProperties mfaProperties = new MfaProperties();
        LoginResponse actualResponse = service.handleLogin(LOGIN, ACCESS_TOKEN, null, MFA_SECRET_CODE, MSISDN, mfaProperties);
        assertThat(actualResponse.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(actualResponse.getStatus()).isEqualTo(LoginResponse.Status.SUCCESS);
    }

    @Test
    void testHandleOtpLogin() throws LockLoginException {
        MfaProperties mfaProperties = new MfaProperties().setOtpDigitsAmount(4).setOtpSendSms(true).setAccessTokenExpiration(Duration.parse("PT5m"));
        when(securityPvvService.acquirePvv(anyString(), anyString())).thenReturn(PVV);

        LoginResponse actualResponse = service.handleLogin(LOGIN, ACCESS_TOKEN, MfaType.OTP, MFA_SECRET_CODE, MSISDN, mfaProperties);
        assertThat(actualResponse.getAccessToken()).isNull();
        assertThat(actualResponse.getLogin()).isEqualTo(LOGIN);
        assertThat(actualResponse.getStatus()).isEqualTo(LoginResponse.Status.MFA_REQUIRED);
        assertThat(actualResponse.getMfaType()).isEqualTo(MfaType.OTP);

        verify(notificationService).sendSmsPinCode(anyString(), eq(MSISDN));
        verify(loginDataStore).save(loginDataCaptor.capture());
        LoginData actualLoginData = loginDataCaptor.getValue();
        assertThat(actualLoginData.getLogin()).isEqualTo(LOGIN);
        assertThat(actualLoginData.getMfaType()).isEqualTo(MfaType.OTP);
        assertThat(actualLoginData.getMfaSecretCode()).isEqualTo(MFA_SECRET_CODE);
        assertThat(actualLoginData.getMfaPinKey()).isNotNull();
        assertThat(actualLoginData.getOtpPvv()).isEqualTo(PVV);
        assertThat(actualLoginData.getToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(actualLoginData.getTokenReceiveTime()).isNotNull();
        assertThat(actualLoginData.getTokenExpireTime()).isNotNull();
    }

    @Test
    void testHandleTotpLogin() throws LockLoginException {
        MfaProperties mfaProperties = new MfaProperties().setOtpDigitsAmount(4).setAccessTokenExpiration(Duration.parse("PT5m"));

        LoginResponse actualResponse = service.handleLogin(LOGIN, ACCESS_TOKEN, MfaType.TOTP, MFA_SECRET_CODE, MSISDN, mfaProperties);
        assertThat(actualResponse.getAccessToken()).isNull();
        assertThat(actualResponse.getLogin()).isEqualTo(LOGIN);
        assertThat(actualResponse.getStatus()).isEqualTo(LoginResponse.Status.MFA_REQUIRED);
        assertThat(actualResponse.getMfaType()).isEqualTo(MfaType.TOTP);

        verify(loginDataStore).save(loginDataCaptor.capture());
        LoginData actualLoginData = loginDataCaptor.getValue();
        assertThat(actualLoginData.getLogin()).isEqualTo(LOGIN);
        assertThat(actualLoginData.getMfaType()).isEqualTo(MfaType.TOTP);
        assertThat(actualLoginData.getMfaSecretCode()).isEqualTo(MFA_SECRET_CODE);
        assertThat(actualLoginData.getMfaPinKey()).isNotNull();
        assertThat(actualLoginData.getToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(actualLoginData.getTokenReceiveTime()).isNotNull();
        assertThat(actualLoginData.getTokenExpireTime()).isNotNull();
    }

    @Test
    void testHandleFailedLogin() {
        MfaProperties mfaProperties = new MfaProperties().setMaxWrongLoginAttempts(3);

        assertThrows(NotAuthorizedException.class,
                () -> service.handleLogin(LOGIN, null, null, MFA_SECRET_CODE, MSISDN, mfaProperties));

        verify(loginDataStore).save(loginDataCaptor.capture());
        LoginData actualLoginData = loginDataCaptor.getValue();
        assertThat(actualLoginData.getLogin()).isEqualTo(LOGIN);
        assertThat(actualLoginData.getFailedLoginAttempts()).isNotEmpty();
    }

    @Test
    void testHandleFailedLoginLastAttempt() {
        MfaProperties mfaProperties = new MfaProperties().setMaxWrongLoginAttempts(3);
        when(loginDataStore.findByLogin(LOGIN)).thenReturn(Optional.of(new LoginData().setLogin(LOGIN)
                .setFailedLoginAttempts(new ArrayList<>(List.of(Tool.currentDateTime(), Tool.currentDateTime())))));

        LockLoginException exception = assertThrows(LockLoginException.class,
                () -> service.handleLogin(LOGIN, null, null, MFA_SECRET_CODE, MSISDN, mfaProperties));
        assertThat(exception.getInitialException()).isNotNull();

        verify(loginDataStore).save(loginDataCaptor.capture());
        LoginData actualLoginData = loginDataCaptor.getValue();
        assertThat(actualLoginData.getLogin()).isEqualTo(LOGIN);
        assertThat(actualLoginData.getFailedLoginAttempts().size()).isEqualTo(3);
    }

    @Test
    void testEnterTotpMfaCode() throws LockLoginException {
        MfaProperties mfaProperties = new MfaProperties().setOtpCodeExpiration(Duration.ofMinutes(5));
        when(loginDataStore.findByLogin(LOGIN)).thenReturn(Optional.of(new LoginData().setLogin(LOGIN)
                .setToken(ACCESS_TOKEN).setTokenReceiveTime(Instant.now().minus(Duration.ofMinutes(4))).setTokenExpireTime(Instant.now().plus(Duration.ofMinutes(1)))
                .setMfaType(MfaType.TOTP).setMfaSecretCode(MFA_SECRET_CODE).setFailedLoginAttempts(List.of(Tool.currentDateTime()))));
        when(verifier.isValidCode(MFA_SECRET_CODE, PIN)).thenReturn(true);

        LoginResponse actualResponse = service.enterMfaCode(LOGIN, PIN, mfaProperties);
        assertThat(actualResponse.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(actualResponse.getStatus()).isEqualTo(LoginResponse.Status.SUCCESS);

        verify(loginDataStore).save(loginDataCaptor.capture());
        checkLoginDataCleanedUp(loginDataCaptor.getValue());
    }

    @Test
    void testEnterOtpMfaCode() throws LockLoginException {
        MfaProperties mfaProperties = new MfaProperties().setOtpCodeExpiration(Duration.ofMinutes(5));
        when(loginDataStore.findByLogin(LOGIN)).thenReturn(Optional.of(new LoginData().setLogin(LOGIN)
                .setToken(ACCESS_TOKEN).setTokenReceiveTime(Instant.now().minus(Duration.ofMinutes(4))).setTokenExpireTime(Instant.now().plus(Duration.ofMinutes(1)))
                .setMfaType(MfaType.OTP).setMfaPinKey(MFA_PIN_KEY).setOtpPvv(PVV).setFailedLoginAttempts(List.of(Tool.currentDateTime()))));
        when(securityPvvService.acquirePvv(MFA_PIN_KEY, PIN)).thenReturn(PVV);

        LoginResponse actualResponse = service.enterMfaCode(LOGIN, PIN, mfaProperties);
        assertThat(actualResponse.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(actualResponse.getStatus()).isEqualTo(LoginResponse.Status.SUCCESS);

        verify(loginDataStore).save(loginDataCaptor.capture());
        checkLoginDataCleanedUp(loginDataCaptor.getValue());
    }

    @Test
    void testEnterWrongOtpMfaCode() {
        MfaProperties mfaProperties = new MfaProperties().setMaxWrongLoginAttempts(3).setOtpCodeExpiration(Duration.ofMinutes(5));
        when(loginDataStore.findByLogin(LOGIN)).thenReturn(Optional.of(new LoginData().setLogin(LOGIN)
                .setToken(ACCESS_TOKEN).setTokenReceiveTime(Instant.now().minus(Duration.ofMinutes(4))).setTokenExpireTime(Instant.now().plus(Duration.ofMinutes(1)))
                .setMfaType(MfaType.OTP).setMfaPinKey(MFA_PIN_KEY).setOtpPvv(PVV)));
        when(securityPvvService.acquirePvv(MFA_PIN_KEY, PIN)).thenReturn("Wrong PVV");

        assertThrows(NotAuthorizedException.class,
                () -> service.enterMfaCode(LOGIN, PIN, mfaProperties));

        verify(loginDataStore).save(loginDataCaptor.capture());
        LoginData actualLoginData = loginDataCaptor.getValue();
        assertThat(actualLoginData.getLogin()).isEqualTo(LOGIN);
        assertThat(actualLoginData.getFailedLoginAttempts()).isNotEmpty();
    }

    @Test
    void testEnterWrongOtpMfaCodeLastAttempt() {
        MfaProperties mfaProperties = new MfaProperties().setMaxWrongLoginAttempts(3).setOtpCodeExpiration(Duration.ofMinutes(5));
        when(loginDataStore.findByLogin(LOGIN)).thenReturn(Optional.of(new LoginData().setLogin(LOGIN)
                .setToken(ACCESS_TOKEN).setTokenReceiveTime(Instant.now().minus(Duration.ofMinutes(4))).setTokenExpireTime(Instant.now().plus(Duration.ofMinutes(1)))
                .setMfaType(MfaType.OTP).setMfaPinKey(MFA_PIN_KEY).setOtpPvv(PVV).setFailedLoginAttempts(new ArrayList<>(List.of(Tool.currentDateTime(), Tool.currentDateTime())))));
        when(securityPvvService.acquirePvv(MFA_PIN_KEY, PIN)).thenReturn("Wrong PVV");

        LockLoginException exception = assertThrows(LockLoginException.class,
                () -> service.enterMfaCode(LOGIN, PIN, mfaProperties));
        assertThat(exception.getInitialException()).isNotNull();

        verify(loginDataStore).save(loginDataCaptor.capture());
        LoginData actualLoginData = loginDataCaptor.getValue();
        assertThat(actualLoginData.getLogin()).isEqualTo(LOGIN);
        assertThat(actualLoginData.getFailedLoginAttempts().size()).isEqualTo(3);
    }

    @Test
    void testEnterOtpMfaCodeTokenExpired() {
        MfaProperties mfaProperties = new MfaProperties().setMaxWrongLoginAttempts(3).setOtpCodeExpiration(Duration.ofMinutes(5));
        when(loginDataStore.findByLogin(LOGIN)).thenReturn(Optional.of(new LoginData().setLogin(LOGIN)
                .setToken(ACCESS_TOKEN).setTokenReceiveTime(Instant.now().minus(Duration.ofMinutes(6))).setTokenExpireTime(Instant.now().minus(Duration.ofMinutes(1)))
                .setMfaType(MfaType.OTP).setMfaPinKey(MFA_PIN_KEY).setOtpPvv(PVV)));

        ICEcashException exception = assertThrows(ICEcashException.class,
                () -> service.enterMfaCode(LOGIN, PIN, mfaProperties));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.EC1038);
    }

    @Test
    void testEnterMfaCodeWrongLogin() {
        UnexistingUserException exception = assertThrows(UnexistingUserException.class,
                () -> service.enterMfaCode(LOGIN, PIN, new MfaProperties()));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.EC1010);
    }

    @Test
    void testEnterBackupCode() throws LockLoginException {
        List<String> activeBackupCodes = List.of("1111", PIN, "2222");
        when(loginDataStore.findByLogin(LOGIN)).thenReturn(Optional.of(new LoginData().setLogin(LOGIN)
                .setToken(ACCESS_TOKEN).setTokenReceiveTime(Instant.now().minus(Duration.ofMinutes(4))).setTokenExpireTime(Instant.now().plus(Duration.ofMinutes(1)))
                .setMfaType(MfaType.TOTP).setMfaSecretCode(MFA_SECRET_CODE).setFailedLoginAttempts(List.of(Tool.currentDateTime()))));

        LoginResponse actualResponse = service.enterBackupCode(LOGIN, activeBackupCodes, PIN, new MfaProperties());
        assertThat(actualResponse.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(actualResponse.getStatus()).isEqualTo(LoginResponse.Status.SUCCESS);

        verify(loginDataStore).save(loginDataCaptor.capture());
        checkLoginDataCleanedUp(loginDataCaptor.getValue());
    }

    @Test
    void testEnterWrongBackupCode() {
        List<String> activeBackupCodes = List.of("1111", "2222");
        MfaProperties mfaProperties = new MfaProperties().setMaxWrongLoginAttempts(3);
        when(loginDataStore.findByLogin(LOGIN)).thenReturn(Optional.of(new LoginData().setLogin(LOGIN)
                .setToken(ACCESS_TOKEN).setTokenReceiveTime(Instant.now().minus(Duration.ofMinutes(4))).setTokenExpireTime(Instant.now().plus(Duration.ofMinutes(1)))
                .setMfaType(MfaType.TOTP).setMfaSecretCode(MFA_SECRET_CODE)));

        assertThrows(NotAuthorizedException.class,
                () -> service.enterBackupCode(LOGIN, activeBackupCodes, PIN, mfaProperties));

        verify(loginDataStore).save(loginDataCaptor.capture());
        LoginData actualLoginData = loginDataCaptor.getValue();
        assertThat(actualLoginData.getLogin()).isEqualTo(LOGIN);
        assertThat(actualLoginData.getFailedLoginAttempts()).isNotEmpty();
    }

    @Test
    void testEnterWrongBackupCodeLastAttempt() {
        List<String> activeBackupCodes = List.of("1111", "2222");
        MfaProperties mfaProperties = new MfaProperties().setMaxWrongLoginAttempts(3);
        when(loginDataStore.findByLogin(LOGIN)).thenReturn(Optional.of(new LoginData().setLogin(LOGIN)
                .setToken(ACCESS_TOKEN).setTokenReceiveTime(Instant.now().minus(Duration.ofMinutes(4))).setTokenExpireTime(Instant.now().plus(Duration.ofMinutes(1)))
                .setMfaType(MfaType.TOTP).setMfaSecretCode(MFA_SECRET_CODE).setFailedLoginAttempts(new ArrayList<>(List.of(Tool.currentDateTime(), Tool.currentDateTime())))));

        LockLoginException exception = assertThrows(LockLoginException.class,
                () -> service.enterBackupCode(LOGIN, activeBackupCodes, PIN, mfaProperties));
        assertThat(exception.getInitialException()).isNotNull();

        verify(loginDataStore).save(loginDataCaptor.capture());
        LoginData actualLoginData = loginDataCaptor.getValue();
        assertThat(actualLoginData.getLogin()).isEqualTo(LOGIN);
        assertThat(actualLoginData.getFailedLoginAttempts().size()).isEqualTo(3);
    }

    @Test
    void testResendOtpCode() {
        MfaProperties mfaProperties = new MfaProperties().setOtpSendSms(true).setOtpDigitsAmount(PIN.length());
        when(loginDataStore.findByLogin(LOGIN)).thenReturn(Optional.of(new LoginData().setLogin(LOGIN)
                .setMfaType(MfaType.OTP).setMfaPinKey(MFA_PIN_KEY).setOtpPvv(PVV)));
        when(securityPvvService.restorePin(MFA_PIN_KEY, PVV, PIN.length())).thenReturn(PIN);

        service.resendOtpCode(LOGIN, MSISDN, mfaProperties);
        verify(notificationService).sendSmsPinCode(PIN, MSISDN);
    }

    @Test
    void testResendOtpCodeNoPvvSaved() {
        when(loginDataStore.findByLogin(LOGIN)).thenReturn(Optional.of(new LoginData().setLogin(LOGIN)));
        ICEcashException exception = assertThrows(ICEcashException.class,
                () -> service.resendOtpCode(LOGIN, MSISDN, new MfaProperties()));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.EC1033);
    }

    @Test
    void testResendOtpCodeWrongLogin() {
        UnexistingUserException exception = assertThrows(UnexistingUserException.class,
                () -> service.resendOtpCode(LOGIN, MSISDN, new MfaProperties()));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.EC1010);
    }

    @Test
    void testCleanupLoginData() {
        when(loginDataStore.findByLogin(LOGIN)).thenReturn(Optional.of(new LoginData().setLogin(LOGIN)
                .setToken(ACCESS_TOKEN).setTokenReceiveTime(Instant.now().minus(Duration.ofMinutes(4))).setTokenExpireTime(Instant.now().plus(Duration.ofMinutes(1)))
                .setMfaType(MfaType.TOTP).setMfaSecretCode(MFA_SECRET_CODE).setFailedLoginAttempts(List.of(Tool.currentDateTime()))));

        service.cleanupLoginData(LOGIN);
        verify(loginDataStore).save(loginDataCaptor.capture());
        checkLoginDataCleanedUp(loginDataCaptor.getValue());
    }

    @Test
    void testCreateForgotPasswordKey() {
        when(secretGenerator.generate()).thenReturn(FORGOT_PASSWORD_KEY);
        String actualKey = service.createForgotPasswordKey(LOGIN);
        assertThat(actualKey).isEqualTo(FORGOT_PASSWORD_KEY);

        verify(loginDataStore).save(loginDataCaptor.capture());
        LoginData actualLoginData = loginDataCaptor.getValue();
        assertThat(actualLoginData.getLogin()).isEqualTo(LOGIN);
        assertThat(actualLoginData.getForgotPasswordKey()).isEqualTo(FORGOT_PASSWORD_KEY);
    }

    @Test
    void testLookupLoginByForgotPasswordKey() {
        when(loginDataStore.findByForgotPasswordKey(FORGOT_PASSWORD_KEY))
                .thenReturn(Optional.of(new LoginData().setLogin(LOGIN)));
        String actualLogin = service.lookupLoginByForgotPasswordKey(FORGOT_PASSWORD_KEY);
        assertThat(actualLogin).isEqualTo(LOGIN);

        verify(loginDataStore).save(loginDataCaptor.capture());
        LoginData actualLoginData = loginDataCaptor.getValue();
        assertThat(actualLoginData.getLogin()).isEqualTo(LOGIN);
        assertThat(actualLoginData.getForgotPasswordKey()).isNull();
    }

    @Test
    void testLookupLoginByWrongForgotPasswordKey() {
        ICEcashException exception = assertThrows(ICEcashException.class,
                () -> service.lookupLoginByForgotPasswordKey(FORGOT_PASSWORD_KEY));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.EC1034);
    }

    private void checkLoginDataCleanedUp(LoginData actualLoginData) {
        assertThat(actualLoginData.getLogin()).isEqualTo(LOGIN);
        assertThat(actualLoginData.getToken()).isNull();
        assertThat(actualLoginData.getTokenReceiveTime()).isNull();
        assertThat(actualLoginData.getTokenExpireTime()).isNull();
        assertThat(actualLoginData.getMfaType()).isNull();
        assertThat(actualLoginData.getMfaPinKey()).isNull();
        assertThat(actualLoginData.getOtpPvv()).isNull();
        assertThat(actualLoginData.getFailedLoginAttempts()).isNull();
    }
}
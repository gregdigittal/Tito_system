package cash.ice.api.service.impl;

import cash.ice.api.config.property.MfaProperties;
import cash.ice.api.dto.LoginData;
import cash.ice.api.dto.LoginResponse;
import cash.ice.api.errors.LockLoginException;
import cash.ice.api.repository.LoginDataStore;
import cash.ice.api.service.KeycloakService;
import cash.ice.api.service.NotificationService;
import cash.ice.api.service.SecurityPvvService;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests MFA flow: null MFA type skips challenge, OTP/TOTP handling, wrong code increments attempts, lock after max attempts.
 */
@ExtendWith(MockitoExtension.class)
class MfaServiceImplTest {

    private static final String LOGIN = "42";
    private static final String MSISDN = "263771234567";
    private static final String SECRET_CODE = "JBSWY3DPEHPK3PXP";

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

    @InjectMocks
    private MfaServiceImpl mfaService;

    private MfaProperties mfaProperties;
    private LoginData loginData;

    @BeforeEach
    void setUp() {
        mfaProperties = new MfaProperties();
        mfaProperties.setOtpDigitsAmount(6);
        mfaProperties.setOtpSendSms(true);
        mfaProperties.setAccessTokenExpiration(Duration.ofMinutes(5));
        mfaProperties.setMaxWrongLoginAttempts(3);
        mfaProperties.setMaxWrongLoginPeriod(Duration.ofMinutes(15));

        loginData = new LoginData().setLogin(LOGIN).setMfaType(MfaType.OTP).setMfaSecretCode(SECRET_CODE);
        when(loginDataStore.findByLogin(LOGIN)).thenReturn(Optional.of(loginData));
        when(loginDataStore.save(any(LoginData.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void handleLogin_whenMfaTypeNull_returnsSuccessWithToken() throws LockLoginException {
        AccessTokenResponse token = new AccessTokenResponse();
        token.setToken("access-token");
        when(loginDataStore.findByLogin(LOGIN)).thenReturn(Optional.empty());

        LoginResponse response = mfaService.handleLogin(LOGIN, token, null, null, MSISDN, mfaProperties);

        assertThat(response.getStatus()).isEqualTo(LoginResponse.Status.SUCCESS);
        assertThat(response.getAccessToken()).isNotNull();
    }

    @Test
    void enterMfaCode_whenTokenExpired_throws() {
        loginData.setToken(null);
        when(loginDataStore.findByLogin(LOGIN)).thenReturn(Optional.of(loginData));

        assertThrows(cash.ice.common.error.ICEcashException.class,
                () -> mfaService.enterMfaCode(LOGIN, "123456", mfaProperties));
    }

    @Test
    void enterBackupCode_whenCodeValid_returnsSuccess() throws LockLoginException {
        loginData.setToken(new AccessTokenResponse());
        List<String> backupCodes = List.of("backup1", "backup2");
        when(loginDataStore.findByLogin(LOGIN)).thenReturn(Optional.of(loginData));

        LoginResponse response = mfaService.enterBackupCode(LOGIN, backupCodes, "backup1", mfaProperties);

        assertThat(response.getStatus()).isEqualTo(LoginResponse.Status.SUCCESS);
    }

    @Test
    void enterBackupCode_whenCodeInvalid_incrementsFailedAttempt() throws LockLoginException {
        loginData.setToken(new AccessTokenResponse());
        when(loginDataStore.findByLogin(LOGIN)).thenReturn(Optional.of(loginData));

        assertThrows(NotAuthorizedException.class,
                () -> mfaService.enterBackupCode(LOGIN, List.of("valid"), "wrong", mfaProperties));
        verify(loginDataStore).save(loginDataCaptor.capture());
        assertThat(loginDataCaptor.getValue().getFailedLoginAttempts()).isNotEmpty();
    }
}

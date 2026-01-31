package cash.ice.api.service.impl;

import cash.ice.api.config.property.MfaProperties;
import cash.ice.api.config.property.StaffProperties;
import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.LoginEntityRequest;
import cash.ice.api.dto.LoginMfaRequest;
import cash.ice.api.dto.LoginResponse;
import cash.ice.api.entity.backoffice.StaffMember;
import cash.ice.api.errors.LockLoginException;
import cash.ice.api.service.*;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.ChannelRepository;
import cash.ice.sqldb.repository.DictionaryRepository;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.AccessTokenResponse;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffMemberLoginServiceImplTest {
    private static final String EMAIL = "user@ice.cash";
    private static final String PIN = "1234";
    private static final String PIN_KEY = "pin_key";
    private static final String PVV = "pvv";
    private static final String SECRET_CODE = "secretCode";
    private static final String MSISDN = "123456789012";
    private static final String FORGOT_PASSWORD_KEY = "fpKey";
    private static final String KEYCLOAK_ID = "keycloakId";
    private static final String FIRST_NAME = "someFirstName";
    private static final String LAST_NAME = "someLastName";

    @Mock
    private StaffMemberService staffMemberService;
    @Mock
    private SecurityPvvService securityPvvService;
    @Mock
    private KeycloakService keycloakService;
    @Mock
    private MfaService mfaService;
    @Mock
    private PermissionsService permissionsService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private DictionaryRepository dictionaryRepository;
    @Mock
    private StaffProperties staffProperties;
    @Captor
    private ArgumentCaptor<StaffMember> staffMemberArgumentCaptor;
    @InjectMocks
    private StaffMemberLoginServiceImpl service;

    @Test
    void testRegisterStaffMember() {
        StaffMember newStaffMember = new StaffMember().setEmail(EMAIL).setFirstName(FIRST_NAME).setLastName(LAST_NAME).setMsisdn(MSISDN);
        MfaProperties mfaProperties = new MfaProperties();
        List<String> mfaBackupCodes = List.of("code1", "code2");

        when(staffMemberService.isStaffMemberExist(EMAIL)).thenReturn(false);
        when(staffProperties.getPasswordDigitsAmount()).thenReturn(4);
        when(staffMemberService.generateStaffMemberPinKey()).thenReturn(PIN_KEY);
        when(securityPvvService.acquirePvv(eq(PIN_KEY), any())).thenReturn(PVV);
        when(keycloakService.createStaffMember(EMAIL, PVV, FIRST_NAME, LAST_NAME, EMAIL)).thenReturn(KEYCLOAK_ID);
        when(mfaService.generateSecretCode()).thenReturn(SECRET_CODE);
        when(staffProperties.getMfa()).thenReturn(mfaProperties);
        when(mfaService.generateBackupCodes(mfaProperties)).thenReturn(mfaBackupCodes);
        when(staffMemberService.save(any())).thenAnswer(invocation -> invocation.getArguments()[0]);
        when(staffProperties.isPasswordSendSms()).thenReturn(true);

        StaffMember actualStaffMember = service.registerStaffMember(newStaffMember, null);
        assertThat(actualStaffMember.getLoginStatus()).isEqualTo(LoginStatus.ACTIVE);
        assertThat(actualStaffMember.getPinKey()).isEqualTo(PIN_KEY);
        assertThat(actualStaffMember.getPvv()).isEqualTo(PVV);
        assertThat(actualStaffMember.getMfaType()).isEqualTo(MfaType.OTP);
        assertThat(actualStaffMember.getMfaSecretCode()).isEqualTo(SECRET_CODE);
        assertThat(actualStaffMember.getKeycloakId()).isEqualTo(KEYCLOAK_ID);
        assertThat(actualStaffMember.getMfaBackupCodes()).isEqualTo(mfaBackupCodes);
        assertThat(actualStaffMember.getLocale()).isEqualTo(Locale.ENGLISH);
        assertThat(actualStaffMember.getCreatedDate()).isNotNull();
        verify(notificationService).sendSmsPinCode(any(), eq(MSISDN));
    }

    @Test
    void testActivateNewStaffMember() {
        String newUserKey = "newUserKey";

        when(mfaService.lookupLoginByForgotPasswordKey(newUserKey)).thenReturn(EMAIL);
        when(staffMemberService.findStaffMember(EMAIL)).thenReturn(createStaffMember());
        when(securityPvvService.acquirePvv(PIN_KEY, PIN)).thenReturn(PVV);
        when(keycloakService.createStaffMember(EMAIL, PVV, FIRST_NAME, LAST_NAME, EMAIL)).thenReturn(KEYCLOAK_ID);
        when(staffMemberService.save(any())).thenAnswer(invocation -> invocation.getArguments()[0]);

        service.activateNewStaffMember(newUserKey, PIN);
        verify(staffMemberService).save(staffMemberArgumentCaptor.capture());
        assertThat(staffMemberArgumentCaptor.getValue().getPvv()).isEqualTo(PVV);
        assertThat(staffMemberArgumentCaptor.getValue().getKeycloakId()).isEqualTo(KEYCLOAK_ID);
    }


    @Test
    void testLoginStaffMember() throws LockLoginException {
        AccessTokenResponse accessToken = new AccessTokenResponse();
        MfaProperties mfaProperties = new MfaProperties();
        LoginResponse loginResponse = LoginResponse.success(accessToken);

        when(staffMemberService.findActiveStaffMember(EMAIL)).thenReturn(createStaffMember());
        when(securityPvvService.acquirePvv(PIN_KEY, PIN)).thenReturn(PVV);
        when(keycloakService.loginStaffMember(EMAIL, PVV)).thenReturn(accessToken);
        when(staffProperties.getMfa()).thenReturn(mfaProperties);
        when(mfaService.handleLogin(EMAIL, accessToken, MfaType.OTP, SECRET_CODE, MSISDN, mfaProperties)).thenReturn(loginResponse);

        LoginResponse actualResponse = service.loginStaffMember(
                new LoginEntityRequest().setUsername(EMAIL).setPassword(PIN));
        assertSame(loginResponse, actualResponse);
    }

    @Test
    void testWrongLoginStaffMemberAndLock() throws LockLoginException {
        AccessTokenResponse accessToken = new AccessTokenResponse();
        MfaProperties mfaProperties = new MfaProperties();

        when(staffMemberService.findActiveStaffMember(EMAIL)).thenReturn(createStaffMember());
        when(securityPvvService.acquirePvv(PIN_KEY, PIN)).thenReturn(PVV);
        when(keycloakService.loginStaffMember(EMAIL, PVV)).thenReturn(accessToken);
        when(staffProperties.getMfa()).thenReturn(mfaProperties);
        when(mfaService.handleLogin(EMAIL, accessToken, MfaType.OTP, SECRET_CODE, MSISDN, mfaProperties))
                .thenThrow(new LockLoginException(new NotAuthorizedException(Response.status(401))));

        assertThrows(NotAuthorizedException.class,
                () -> service.loginStaffMember(new LoginEntityRequest().setUsername(EMAIL).setPassword(PIN)));
        verify(staffMemberService).save(staffMemberArgumentCaptor.capture());
        assertThat(staffMemberArgumentCaptor.getValue().getLoginStatus()).isEqualTo(LoginStatus.LOCKED);
    }

    @Test
    void testEnterLoginMfaCode() throws LockLoginException {
        AccessTokenResponse accessToken = new AccessTokenResponse();
        MfaProperties mfaProperties = new MfaProperties();
        LoginResponse loginResponse = LoginResponse.success(accessToken);

        when(staffMemberService.findActiveStaffMember(EMAIL)).thenReturn(createStaffMember());
        when(staffProperties.getMfa()).thenReturn(mfaProperties);
        when(mfaService.enterMfaCode(EMAIL, PIN, mfaProperties)).thenReturn(loginResponse);

        LoginResponse actualResponse = service.enterLoginMfaCode(
                new LoginMfaRequest().setUsername(EMAIL).setCode(PIN));
        assertSame(loginResponse, actualResponse);
    }

    @Test
    void testEnterLoginMfaBackupCode() throws LockLoginException {
        String backupCode = "backupCode1";
        StaffMember staffMember = createStaffMember();
        AccessTokenResponse accessToken = new AccessTokenResponse();
        MfaProperties mfaProperties = new MfaProperties();
        LoginResponse loginResponse = LoginResponse.success(null);

        when(staffMemberService.findActiveStaffMember(EMAIL)).thenReturn(staffMember);
        when(staffProperties.getMfa()).thenReturn(mfaProperties);
        when(mfaService.enterBackupCode(EMAIL, staffMember.getMfaBackupCodes(), backupCode, mfaProperties)).thenReturn(loginResponse);
        when(keycloakService.loginStaffMember(EMAIL, PVV)).thenReturn(accessToken);

        LoginResponse actualResponse = service.enterLoginMfaBackupCode(
                new LoginMfaRequest().setUsername(EMAIL).setCode(backupCode));
        assertSame(loginResponse, actualResponse);
        assertThat(actualResponse.getAccessToken()).isEqualTo(accessToken);
        verify(staffMemberService).save(staffMemberArgumentCaptor.capture());
        assertThat(staffMemberArgumentCaptor.getValue().getMfaBackupCodes()).isEqualTo(List.of("backupCode2"));
    }

    @Test
    void testResendOtpCode() {
        MfaProperties mfaProperties = new MfaProperties();

        when(staffMemberService.findActiveStaffMember(EMAIL)).thenReturn(createStaffMember());
        when(staffProperties.getMfa()).thenReturn(mfaProperties);

        service.resendOtpCode(EMAIL);
        verify(mfaService).resendOtpCode(EMAIL, MSISDN, mfaProperties);
    }

    @Test
    void testForgotPassword() {
        StaffMember staffMember = createStaffMember();
        String emailTemplate = "template";
        int languageId = 3;
        String emailFrom = "support@ice.cash";

        when(staffMemberService.findStaffMemberOrElse(eq(EMAIL), any())).thenReturn(staffMember);
        when(staffProperties.getForgotPasswordEmailTemplate()).thenReturn(emailTemplate);
        when(staffProperties.getForgotPasswordEmailFrom()).thenReturn(emailFrom);
        when(mfaService.createForgotPasswordKey(EMAIL)).thenReturn(FORGOT_PASSWORD_KEY);
        when(staffMemberService.getStaffMemberLanguage(staffMember)).thenReturn(new Language().setId(languageId));
        when(notificationService.sendEmailByTemplate(eq(true), eq(emailTemplate), eq(languageId), eq(emailFrom), eq(List.of(EMAIL)), any())).thenReturn(true);

        boolean actualResponse = service.forgotPassword(EMAIL, "url", true, "someIp");
        assertThat(actualResponse).isTrue();
    }

    @Test
    void testResetStaffMemberPassword() {
        String newPvv = "newPvv";

        when(mfaService.lookupLoginByForgotPasswordKey(FORGOT_PASSWORD_KEY)).thenReturn(EMAIL);
        when(staffMemberService.findStaffMember(EMAIL)).thenReturn(createStaffMember());
        when(securityPvvService.acquirePvv(PIN_KEY, PIN)).thenReturn(newPvv);
        when(staffMemberService.save(any())).thenAnswer(invocation -> invocation.getArguments()[0]);

        service.resetStaffMemberPassword(FORGOT_PASSWORD_KEY, PIN);
        verify(keycloakService).updateUser(KEYCLOAK_ID, newPvv, FIRST_NAME, LAST_NAME, EMAIL);
        verify(staffMemberService).save(staffMemberArgumentCaptor.capture());
        assertThat(staffMemberArgumentCaptor.getValue().getPvv()).isEqualTo(newPvv);
    }

    @Test
    void testGetMfaQrCode() {
        StaffMember staffMember = createStaffMember();
        MfaProperties mfaProperties = new MfaProperties();
        int languageId = 4;
        String languageKey = "languageKey";
        String dictionaryValue = "dict";
        String qrCode = "qrCode";

        when(channelRepository.findByCode("ADM")).thenReturn(Optional.of(new Channel().setLanguageKey(languageKey)));
        when(staffMemberService.getStaffMemberLanguage(staffMember)).thenReturn(new Language().setId(languageId));
        when(dictionaryRepository.findByLanguageIdAndLookupKey(languageId, languageKey)).thenReturn(Optional.of(new Dictionary().setValue(dictionaryValue)));
        when(staffProperties.getMfa()).thenReturn(mfaProperties);
        when(mfaService.getQrCode(dictionaryValue, EMAIL, SECRET_CODE, mfaProperties)).thenReturn(qrCode);

        String actualQrCode = service.getMfaQrCode(staffMember);
        assertThat(actualQrCode).isEqualTo(qrCode);
    }

    @Test
    void testGetMfaQrCodeNoLanguageKey() {
        when(channelRepository.findByCode("ADM")).thenReturn(Optional.of(new Channel().setLanguageKey(null)));
        ICEcashException exception = assertThrows(ICEcashException.class,
                () -> service.getMfaQrCode(createStaffMember()));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.EC1043);
    }

    @Test
    void testUpdateEntityPassword() {
        AuthUser authUser = new AuthUser().setPrincipal(EMAIL);
        String oldPassword = "1234";
        String newPassword = "2345";
        String newPvv = "newPvv";

        when(permissionsService.getAuthStaffMember(authUser)).thenReturn(createStaffMember());
        when(securityPvvService.acquirePvv(PIN_KEY, oldPassword)).thenReturn(PVV);
        when(securityPvvService.acquirePvv(PIN_KEY, newPassword)).thenReturn(newPvv);
        when(staffMemberService.save(any())).thenAnswer(invocation -> invocation.getArguments()[0]);

        service.updateStaffMemberPassword(authUser, oldPassword, newPassword);
        verify(keycloakService).updateUser(KEYCLOAK_ID, newPvv, FIRST_NAME, LAST_NAME, EMAIL);
        verify(staffMemberService).save(staffMemberArgumentCaptor.capture());
        assertThat(staffMemberArgumentCaptor.getValue().getPvv()).isEqualTo(newPvv);
    }

    @Test
    void testUpdateStaffMemberLoginStatus() {
        when(staffMemberService.findStaffMember(EMAIL)).thenReturn(
                createStaffMember().setLoginStatus(LoginStatus.LOCKED));
        service.updateStaffMemberLoginStatus(EMAIL, LoginStatus.ACTIVE);
        verify(staffMemberService).save(staffMemberArgumentCaptor.capture());
        assertThat(staffMemberArgumentCaptor.getValue().getLoginStatus()).isEqualTo(LoginStatus.ACTIVE);
    }

    private StaffMember createStaffMember() {
        return new StaffMember().setEmail(EMAIL).setPinKey(PIN_KEY).setPvv(PVV)
                .setMsisdn(MSISDN).setFirstName(FIRST_NAME).setLastName(LAST_NAME).setKeycloakId(KEYCLOAK_ID)
                .setLoginStatus(LoginStatus.ACTIVE).setMfaType(MfaType.OTP).setMfaSecretCode(SECRET_CODE)
                .setMfaBackupCodes(List.of("backupCode1", "backupCode2"));
    }
}
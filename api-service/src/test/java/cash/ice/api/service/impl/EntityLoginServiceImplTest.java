package cash.ice.api.service.impl;

import cash.ice.api.config.property.EntitiesProperties;
import cash.ice.api.config.property.MfaProperties;
import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.LoginEntityRequest;
import cash.ice.api.dto.LoginMfaRequest;
import cash.ice.api.dto.LoginResponse;
import cash.ice.api.errors.LockLoginException;
import cash.ice.api.errors.UnexistingUserException;
import cash.ice.api.service.*;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityLoginServiceImplTest {
    private static final String PIN = "1234";
    private static final String CLIENT_ID = "testClientId";
    private static final String CLIENT_SECRET = "testClientSecret";
    private static final int ENTITY_ID = 12;
    private static final String ENTITY_KEYCLOAK_USERNAME = String.valueOf(30000000 + ENTITY_ID);
    private static final int ACCOUNT_ID = 15;
    private static final String INTERNAL_ID = "23";
    private static final String PVV = "34";
    private static final String GRANT_TYPE = "testGrantType";
    private static final String USERNAME = "31234567890";
    private static final String SECRET_CODE = "secretCode";
    private static final String MSISDN = "123456789012";
    private static final String KEYCLOAK_ID = "keycloakId";
    private static final String EMAIL = "user@ice.cash";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";

    @Mock
    private SecurityPvvService securityPvvService;
    @Mock
    private KeycloakService keycloakService;
    @Mock
    private MfaService mfaService;
    @Mock
    private PermissionsService permissionsService;
    @Mock
    private EntityService entityService;
    @Mock
    private EntityMsisdnRepository entityMsisdnRepository;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private InitiatorRepository initiatorRepository;
    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private DictionaryRepository dictionaryRepository;
    @Mock
    private EntitiesProperties entitiesProperties;
    @Captor
    private ArgumentCaptor<EntityClass> entityArgumentCaptor;
    @InjectMocks
    private EntityLoginServiceImpl service;

    @Test
    void testLoginByAccountNumber() {
        String enterId = "31234567890";
        LoginEntityRequest request = new LoginEntityRequest().setUsername(enterId).setPassword(PIN)
                .setClientId(CLIENT_ID).setClientSecret(CLIENT_SECRET).setGrantType(GRANT_TYPE);
        AccessTokenResponse expectedResponse = new AccessTokenResponse();

        when(accountRepository.findByAccountNumber(enterId)).thenReturn(List.of(new Account().setEntityId(ENTITY_ID).setAccountStatus(AccountStatus.ACTIVE)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(createEntity()));
        when(keycloakService.loginUser(GRANT_TYPE, ENTITY_KEYCLOAK_USERNAME, PIN, CLIENT_ID, CLIENT_SECRET)).thenReturn(expectedResponse);

        AccessTokenResponse actualResponse = service.simpleLogin(request);
        assertSame(expectedResponse, actualResponse);
    }

    @Test
    void testLoginByAccountNumberMultipleAccountsSameEntity() {
        String enterId = "31234567890";
        LoginEntityRequest request = new LoginEntityRequest().setUsername(enterId).setPassword(PIN)
                .setClientId(CLIENT_ID).setClientSecret(CLIENT_SECRET).setGrantType(GRANT_TYPE);
        AccessTokenResponse expectedResponse = new AccessTokenResponse();

        when(accountRepository.findByAccountNumber(enterId)).thenReturn(List.of(new Account().setEntityId(ENTITY_ID), new Account().setEntityId(ENTITY_ID).setAccountStatus(AccountStatus.ACTIVE)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(createEntity()));
        when(keycloakService.loginUser(GRANT_TYPE, ENTITY_KEYCLOAK_USERNAME, PIN, CLIENT_ID, CLIENT_SECRET)).thenReturn(expectedResponse);

        AccessTokenResponse actualResponse = service.simpleLogin(request);
        assertSame(expectedResponse, actualResponse);
    }

    @Test
    void testLoginByAccountNumberMultipleEntities() {
        String enterId = "31234567890";
        LoginEntityRequest request = new LoginEntityRequest().setUsername(enterId).setPassword(PIN)
                .setClientId(CLIENT_ID).setClientSecret(CLIENT_SECRET).setGrantType(GRANT_TYPE);

        when(accountRepository.findByAccountNumber(enterId)).thenReturn(
                List.of(new Account().setEntityId(ENTITY_ID), new Account().setEntityId(111)));
        assertThrows(ICEcashException.class, () -> service.simpleLogin(request));
    }

    @Test
    void testLoginByCardNumber() {
        String enterId = "1234567890123456";
        LoginEntityRequest request = new LoginEntityRequest().setUsername(enterId).setPassword(PIN)
                .setClientId(CLIENT_ID).setClientSecret(CLIENT_SECRET).setGrantType(GRANT_TYPE);
        AccessTokenResponse expectedResponse = new AccessTokenResponse();

        when(initiatorRepository.findByIdentifier(enterId)).thenReturn(Optional.of(new Initiator().setAccountId(ACCOUNT_ID)));
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(new Account().setEntityId(ENTITY_ID)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(createEntity()));
        when(keycloakService.loginUser(GRANT_TYPE, ENTITY_KEYCLOAK_USERNAME, PIN, CLIENT_ID, CLIENT_SECRET)).thenReturn(expectedResponse);

        AccessTokenResponse actualResponse = service.simpleLogin(request);
        assertSame(expectedResponse, actualResponse);
    }

    @Test
    void testLoginByIdNumber() {
        String enterId = "123456";
        LoginEntityRequest request = new LoginEntityRequest().setUsername(enterId).setPassword(PIN)
                .setClientId(CLIENT_ID).setClientSecret(CLIENT_SECRET).setGrantType(GRANT_TYPE);
        AccessTokenResponse expectedResponse = new AccessTokenResponse();

        when(entityRepository.findByIdNumber(enterId)).thenReturn(List.of(createEntity()));
        when(keycloakService.loginUser(GRANT_TYPE, ENTITY_KEYCLOAK_USERNAME, PIN, CLIENT_ID, CLIENT_SECRET)).thenReturn(expectedResponse);

        AccessTokenResponse actualResponse = service.simpleLogin(request);
        assertSame(expectedResponse, actualResponse);
    }

    @Test
    void testLoginUnexistingUser() {
        String enterId = "123456";
        LoginEntityRequest request = new LoginEntityRequest().setUsername(enterId).setPassword(PIN)
                .setClientId(CLIENT_ID).setClientSecret(CLIENT_SECRET).setGrantType(GRANT_TYPE);
        when(entityRepository.findByIdNumber(enterId)).thenReturn(List.of());

        assertThrows(UnexistingUserException.class, () -> service.simpleLogin(request));
    }

    @Test
    void testMakeLogin() throws LockLoginException {
        AccessTokenResponse accessToken = new AccessTokenResponse();
        MfaProperties mfaProperties = new MfaProperties();
        LoginResponse loginResponse = LoginResponse.success(accessToken);

        when(accountRepository.findByAccountNumber(USERNAME)).thenReturn(List.of(new Account().setEntityId(ENTITY_ID).setAccountStatus(AccountStatus.ACTIVE)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(createEntity()));
        when(keycloakService.loginUser(null, ENTITY_KEYCLOAK_USERNAME, PIN, null, null)).thenReturn(accessToken);
        when(entityMsisdnRepository.findByEntityIdAndPrimaryMsisdn(ENTITY_ID)).thenReturn(Optional.of(new EntityMsisdn().setMsisdn(MSISDN)));
        when(entitiesProperties.getMfa()).thenReturn(mfaProperties);
        when(mfaService.handleLogin(String.valueOf(ENTITY_ID), accessToken, MfaType.OTP, SECRET_CODE, MSISDN, mfaProperties)).thenReturn(loginResponse);

        LoginResponse actualResponse = service.makeLogin(
                new LoginEntityRequest().setUsername(USERNAME).setPassword(PIN));
        assertSame(loginResponse, actualResponse);
    }

    @Test
    void testWrongLoginAndLock() throws LockLoginException {
        AccessTokenResponse accessToken = new AccessTokenResponse();
        MfaProperties mfaProperties = new MfaProperties();

        when(accountRepository.findByAccountNumber(USERNAME)).thenReturn(List.of(new Account().setEntityId(ENTITY_ID).setAccountStatus(AccountStatus.ACTIVE)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(createEntity()));
        when(keycloakService.loginUser(null, ENTITY_KEYCLOAK_USERNAME, PIN, null, null)).thenReturn(accessToken);
        when(entityMsisdnRepository.findByEntityIdAndPrimaryMsisdn(ENTITY_ID)).thenReturn(Optional.of(new EntityMsisdn().setMsisdn(MSISDN)));
        when(entitiesProperties.getMfa()).thenReturn(mfaProperties);
        when(mfaService.handleLogin(String.valueOf(ENTITY_ID), accessToken, MfaType.OTP, SECRET_CODE, MSISDN, mfaProperties))
                .thenThrow(new LockLoginException(new NotAuthorizedException(Response.status(401))));

        assertThrows(NotAuthorizedException.class,
                () -> service.makeLogin(new LoginEntityRequest().setUsername(USERNAME).setPassword(PIN)));
        verify(entityRepository).save(entityArgumentCaptor.capture());
        assertThat(entityArgumentCaptor.getValue().getLoginStatus()).isEqualTo(LoginStatus.LOCKED);
    }

    @Test
    void testWrongPasswordFromKeycloakIsHandledByMfaTracking() throws LockLoginException {
        MfaProperties mfaProperties = new MfaProperties();

        when(accountRepository.findByAccountNumber(USERNAME)).thenReturn(List.of(new Account().setEntityId(ENTITY_ID).setAccountStatus(AccountStatus.ACTIVE)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(createEntity()));
        when(keycloakService.loginUser(null, ENTITY_KEYCLOAK_USERNAME, PIN, null, null))
                .thenThrow(new NotAuthorizedException(Response.status(401)));
        when(entityMsisdnRepository.findByEntityIdAndPrimaryMsisdn(ENTITY_ID)).thenReturn(Optional.of(new EntityMsisdn().setMsisdn(MSISDN)));
        when(entitiesProperties.getMfa()).thenReturn(mfaProperties);
        when(mfaService.handleLogin(String.valueOf(ENTITY_ID), null, MfaType.OTP, SECRET_CODE, MSISDN, mfaProperties))
                .thenThrow(new LockLoginException(new NotAuthorizedException(Response.status(401))));

        assertThrows(NotAuthorizedException.class,
                () -> service.makeLogin(new LoginEntityRequest().setUsername(USERNAME).setPassword(PIN)));
        verify(entityRepository).save(entityArgumentCaptor.capture());
        assertThat(entityArgumentCaptor.getValue().getLoginStatus()).isEqualTo(LoginStatus.LOCKED);
    }

    @Test
    void testEnterLoginMfaCode() throws LockLoginException {
        AccessTokenResponse accessToken = new AccessTokenResponse();
        MfaProperties mfaProperties = new MfaProperties();
        LoginResponse loginResponse = LoginResponse.success(accessToken);

        when(accountRepository.findByAccountNumber(USERNAME)).thenReturn(List.of(new Account().setEntityId(ENTITY_ID).setAccountStatus(AccountStatus.ACTIVE)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(createEntity()));
        when(entitiesProperties.getMfa()).thenReturn(mfaProperties);
        when(mfaService.enterMfaCode(String.valueOf(ENTITY_ID), PIN, mfaProperties)).thenReturn(loginResponse);

        LoginResponse actualResponse = service.enterLoginMfaCode(
                new LoginMfaRequest().setUsername(USERNAME).setCode(PIN));
        assertSame(loginResponse, actualResponse);
    }

    @Test
    void testEnterLoginMfaBackupCode() throws LockLoginException {
        String backupCode = "backupCode1";
        EntityClass entity = createEntity();
        AccessTokenResponse accessToken = new AccessTokenResponse();
        MfaProperties mfaProperties = new MfaProperties();
        LoginResponse loginResponse = LoginResponse.success(accessToken);

        when(accountRepository.findByAccountNumber(USERNAME)).thenReturn(List.of(new Account().setEntityId(ENTITY_ID).setAccountStatus(AccountStatus.ACTIVE)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(entity));
        when(entitiesProperties.getMfa()).thenReturn(mfaProperties);
        when(mfaService.enterBackupCode(String.valueOf(ENTITY_ID), entity.getMfaBackupCodes(), backupCode, mfaProperties)).thenReturn(loginResponse);

        LoginResponse actualResponse = service.enterLoginMfaBackupCode(
                new LoginMfaRequest().setUsername(USERNAME).setCode(backupCode));
        assertSame(loginResponse, actualResponse);
        assertThat(actualResponse.getAccessToken()).isEqualTo(accessToken);
        verify(entityRepository).save(entityArgumentCaptor.capture());
        assertThat(entityArgumentCaptor.getValue().getMfaBackupCodes()).isEqualTo(List.of("backupCode2"));
    }

    @Test
    void testEnterLoginMfaBackupCodeWithoutTokenThrows() throws LockLoginException {
        String backupCode = "backupCode1";
        EntityClass entity = createEntity();
        MfaProperties mfaProperties = new MfaProperties();
        LoginResponse loginResponse = LoginResponse.success(null);

        when(accountRepository.findByAccountNumber(USERNAME)).thenReturn(List.of(new Account().setEntityId(ENTITY_ID).setAccountStatus(AccountStatus.ACTIVE)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(entity));
        when(entitiesProperties.getMfa()).thenReturn(mfaProperties);
        when(mfaService.enterBackupCode(String.valueOf(ENTITY_ID), entity.getMfaBackupCodes(), backupCode, mfaProperties)).thenReturn(loginResponse);

        ICEcashException exception = assertThrows(ICEcashException.class,
                () -> service.enterLoginMfaBackupCode(new LoginMfaRequest().setUsername(USERNAME).setCode(backupCode)));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.EC1038);
    }

    @Test
    void testResendOtpCode() {
        MfaProperties mfaProperties = new MfaProperties();

        when(accountRepository.findByAccountNumber(USERNAME)).thenReturn(List.of(new Account().setEntityId(ENTITY_ID).setAccountStatus(AccountStatus.ACTIVE)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(createEntity()));
        when(entitiesProperties.getMfa()).thenReturn(mfaProperties);
        when(entityMsisdnRepository.findByEntityIdAndPrimaryMsisdn(ENTITY_ID)).thenReturn(Optional.of(new EntityMsisdn().setMsisdn(MSISDN)));

        service.resendOtpCode(USERNAME);
        verify(mfaService).resendOtpCode(String.valueOf(ENTITY_ID), MSISDN, mfaProperties);
    }

    @Test
    void testUpdateEntityPassword() {
        AuthUser authUser = new AuthUser().setPrincipal(USERNAME);
        String oldPassword = "1234";
        String newPassword = "2345";
        String newPvv = "newPvv";

        when(permissionsService.getAuthEntity(authUser)).thenReturn(createEntity());
        when(securityPvvService.acquirePvv(INTERNAL_ID, oldPassword)).thenReturn(PVV);
        when(securityPvvService.acquirePvv(INTERNAL_ID, newPassword)).thenReturn(newPvv);
        when(entityRepository.save(any())).thenAnswer(invocation -> invocation.getArguments()[0]);

        service.updateEntityPassword(authUser, oldPassword, newPassword);
        verify(keycloakService).updateUser(KEYCLOAK_ID, newPassword, FIRST_NAME, LAST_NAME, EMAIL);
        verify(entityRepository).save(entityArgumentCaptor.capture());
        assertThat(entityArgumentCaptor.getValue().getPvv()).isEqualTo(newPvv);
    }

    @Test
    void testUpdateEntityLoginStatus() {
        when(accountRepository.findByAccountNumber(USERNAME)).thenReturn(List.of(new Account().setEntityId(ENTITY_ID).setAccountStatus(AccountStatus.ACTIVE)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(createEntity().setLoginStatus(LoginStatus.LOCKED)));
        service.updateEntityLoginStatus(USERNAME, LoginStatus.ACTIVE);
        verify(entityRepository).save(entityArgumentCaptor.capture());
        assertThat(entityArgumentCaptor.getValue().getLoginStatus()).isEqualTo(LoginStatus.ACTIVE);
    }

    @Test
    void testGetMfaQrCode() {
        MfaProperties mfaProperties = new MfaProperties();
        EntityClass entity = createEntity();
        int languageId = 4;
        String languageKey = "languageKey";
        String dictionaryValue = "dict";
        String qrCode = "qrCode";

        when(channelRepository.findByCode("ONL")).thenReturn(Optional.of(new Channel().setLanguageKey(languageKey)));
        when(entityService.getEntityLanguage(entity)).thenReturn(new Language().setId(languageId));
        when(dictionaryRepository.findByLanguageIdAndLookupKey(languageId, languageKey)).thenReturn(Optional.of(new Dictionary().setValue(dictionaryValue)));
        when(entitiesProperties.getMfa()).thenReturn(mfaProperties);
        when(mfaService.getQrCode(dictionaryValue, entity.getEmail(), SECRET_CODE, mfaProperties)).thenReturn(qrCode);

        String actualQrCode = service.getMfaQrCode(entity);
        assertThat(actualQrCode).isEqualTo(qrCode);
    }

    @Test
    void testGetMfaQrCodeNoLanguageKey() {
        when(channelRepository.findByCode("ONL")).thenReturn(Optional.of(new Channel().setLanguageKey(null)));
        ICEcashException exception = assertThrows(ICEcashException.class,
                () -> service.getMfaQrCode(createEntity()));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.EC1043);
    }

    private EntityClass createEntity() {
        return new EntityClass().setId(ENTITY_ID).setInternalId(INTERNAL_ID).setPvv(PVV).setKeycloakId(KEYCLOAK_ID)
                .setEmail(EMAIL).setFirstName(FIRST_NAME).setLastName(LAST_NAME).setLoginStatus(LoginStatus.ACTIVE)
                .setMfaType(MfaType.OTP).setMfaSecretCode(SECRET_CODE).setMfaBackupCodes(List.of("backupCode1", "backupCode2"));
    }
}

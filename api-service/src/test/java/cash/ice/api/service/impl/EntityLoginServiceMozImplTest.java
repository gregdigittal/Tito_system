package cash.ice.api.service.impl;

import cash.ice.api.config.property.EntitiesProperties;
import cash.ice.api.config.property.MfaProperties;
import cash.ice.api.dto.LoginEntityRequest;
import cash.ice.api.dto.LoginResponse;
import cash.ice.api.errors.LockLoginException;
import cash.ice.api.service.KeycloakService;
import cash.ice.api.service.MfaService;
import cash.ice.api.service.SecurityPvvService;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.entity.moz.Device;
import cash.ice.sqldb.entity.moz.Vehicle;
import cash.ice.sqldb.repository.AccountRepository;
import cash.ice.sqldb.repository.EntityMsisdnRepository;
import cash.ice.sqldb.repository.EntityRepository;
import cash.ice.sqldb.repository.moz.DeviceRepository;
import cash.ice.sqldb.repository.moz.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.AccessTokenResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityLoginServiceMozImplTest {
    private static final int ENTITY_ID = 1234;
    private static final String DEVICE_SERIAL = "serial";
    private static final int VEHICLE_ID = 123;
    private static final String PIN = "1234";
    private static final String USERNAME = "31234567890";
    private static final String INTERNAL_ID = "23";
    private static final String PVV = "34";
    private static final String ENTITY_KEYCLOAK_USERNAME = String.valueOf(30000000 + ENTITY_ID);
    private static final String SECRET_CODE = "secretCode";
    private static final String MSISDN = "123456789012";
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private SecurityPvvService securityPvvService;
    @Mock
    private KeycloakService keycloakService;
    @Mock
    private EntityMsisdnRepository entityMsisdnRepository;
    @Mock
    private MfaService mfaService;
    @Mock
    private EntitiesProperties entitiesProperties;
    @InjectMocks
    private EntityLoginServiceMozImpl service;

    @Test
    void testFindEntityById() {
        String enterId = "1234";
        EntityClass entity = new EntityClass().setId(ENTITY_ID);

        when(entityRepository.findById(1234)).thenReturn(Optional.of(entity));
        EntityClass actualEntity = service.findEntity(enterId);
        assertThat(actualEntity).isEqualTo(entity);
    }

    @Test
    void testFindEntityByAccountNum() {
        String enterId = "1234";
        EntityClass entity = new EntityClass().setId(ENTITY_ID);

        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(entity));

        EntityClass actualEntity = service.findEntity(enterId);
        assertThat(actualEntity).isEqualTo(entity);
    }

    @Test
    void testMakePosDeviceLogin() throws LockLoginException {
        AccessTokenResponse accessToken = new AccessTokenResponse();
        MfaProperties mfaProperties = new MfaProperties();
        LoginResponse loginResponse = LoginResponse.success(accessToken);
        EntityClass entity = new EntityClass().setId(ENTITY_ID).setInternalId(INTERNAL_ID).setPvv(PVV).setLoginStatus(LoginStatus.ACTIVE)
                .setMfaType(MfaType.OTP).setMfaSecretCode(SECRET_CODE).setMfaBackupCodes(List.of("backupCode1", "backupCode2"));

        when(accountRepository.findByAccountNumber(USERNAME)).thenReturn(List.of(new Account().setEntityId(ENTITY_ID).setAccountStatus(AccountStatus.ACTIVE)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(entity));
        when(securityPvvService.acquirePvv(INTERNAL_ID, PIN)).thenReturn(PVV);
        when(keycloakService.loginUser(null, ENTITY_KEYCLOAK_USERNAME, PIN, null, null)).thenReturn(accessToken);
        when(entityMsisdnRepository.findByEntityIdAndPrimaryMsisdn(ENTITY_ID)).thenReturn(Optional.of(new EntityMsisdn().setMsisdn(MSISDN)));
        when(entitiesProperties.getMfa()).thenReturn(mfaProperties);
        when(mfaService.handleLogin(String.valueOf(ENTITY_ID), accessToken, MfaType.OTP, SECRET_CODE, MSISDN, mfaProperties)).thenReturn(loginResponse);
        when(deviceRepository.findBySerial(DEVICE_SERIAL)).thenReturn(Optional.of(new Device().setVehicleId(VEHICLE_ID)));
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(new Vehicle().setCollectorEntityId(ENTITY_ID)));

        LoginResponse actualResponse = service.makePosDeviceLogin(DEVICE_SERIAL, new LoginEntityRequest().setUsername(USERNAME).setPassword(PIN));
        assertSame(loginResponse, actualResponse);
    }
}

package cash.ice.api.service.impl;

import cash.ice.api.config.property.KenProperties;
import cash.ice.api.dto.OtpType;
import cash.ice.api.dto.RegisterEntityRequest;
import cash.ice.api.dto.moz.AccountTypeKen;
import cash.ice.api.dto.moz.IdTypeKen;
import cash.ice.api.dto.moz.RegisterEntityKenRequest;
import cash.ice.api.dto.moz.RegisterKenResponse;
import cash.ice.api.service.*;
import cash.ice.common.dto.ResponseStatus;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.CurrencyRepository;
import cash.ice.sqldb.repository.EntityMsisdnRepository;
import cash.ice.sqldb.repository.EntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static cash.ice.sqldb.entity.AccountType.FNDS_ACCOUNT;
import static cash.ice.sqldb.entity.AccountType.PRIMARY_ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityRegistrationKenServiceImplTest {
    private static final String PIN = "1234";
    private static final String INTERNAL_ID = "4321";
    private static final String KEYCLOAK_ID = "32";
    private static final int CURRENCY_ID = 1;
    private static final String PVV = "Pvv";
    private static final String FNDS_ACC_NUMBER = "4567";
    private static final int ENTITY_ID = 12;
    private static final int PRIMARY_ACCOUNT_ID = 14;
    private static final String FIRST_NAME = "first";
    private static final String LAST_NAME = "last";
    private static final IdTypeKen ID_TYPE_ID = IdTypeKen.NationalID;
    private static final String ID_NUMBER = "1423";
    private static final String EMAIL = null;
    private static final String MOBILE = "123-45-67";
    private static final Locale LOCALE = Locale.ENGLISH;
    private static final int ID_UPLOAD_DOCUMENT_ID = 12;
    private static final int BIO_UPLOAD_DOCUMENT_ID = 13;
    private static final String OTP = "1234";

    @Mock
    private EntityRegistrationService entityRegistrationService;
    @Mock
    private OtpService otpService;
    @Mock
    private SecurityPvvService securityPvvService;
    @Mock
    private DocumentsService documentsService;
    @Mock
    private PermissionsService permissionsService;
    @Mock
    private PermissionsGroupService permissionsGroupService;
    @Mock
    private KeycloakService keycloakService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private EntityMsisdnRepository entityMsisdnRepository;
    @Mock
    private KenProperties kenProperties;
    @InjectMocks
    private EntityRegistrationKenServiceImpl service;

    @Test
    void testRegisterFndsUser() {
        RegisterEntityKenRequest request = new RegisterEntityKenRequest().setAccountType(AccountTypeKen.Farmer).setEmail(EMAIL)
                .setFirstName(FIRST_NAME).setLastName(LAST_NAME).setIdType(ID_TYPE_ID).setIdNumber(ID_NUMBER).setMobile(MOBILE).setPin(PIN)
                .setIdUploadDocumentId(ID_UPLOAD_DOCUMENT_ID).setBiometricUploadDocumentId(BIO_UPLOAD_DOCUMENT_ID);
        EntityClass entity = new EntityClass().setId(ENTITY_ID).setPvv(PVV).setFirstName(FIRST_NAME).setLastName(LAST_NAME).setEmail(EMAIL).setLocale(LOCALE);
        Currency currency = new Currency().setId(CURRENCY_ID).setIsoCode(Currency.KES);
        Account primaryAccount = new Account().setId(PRIMARY_ACCOUNT_ID);
        Account fndsAccount = new Account().setAccountNumber(FNDS_ACC_NUMBER);

        when(kenProperties.isUserRegCheckOtp()).thenReturn(true);
        when(kenProperties.isValidatePhoneUniqueness()).thenReturn(true);
        when(entityMsisdnRepository.existsByMsisdn(MOBILE)).thenReturn(false);
        when(kenProperties.isValidateIdUniqueness()).thenReturn(true);
        when(entityRepository.existsAccountByIdNumberAndIdType(ID_NUMBER, ID_TYPE_ID.getDbId())).thenReturn(false);
        when(entityRegistrationService.generateInternalId()).thenReturn(INTERNAL_ID);
        when(securityPvvService.acquirePvv(INTERNAL_ID, PIN)).thenReturn(PVV);
        when(entityRegistrationService.saveEntity(new RegisterEntityRequest().setEntityType(EntityType.FNDS_FARMER).setFirstName(FIRST_NAME).setLastName(LAST_NAME)
                        .setIdTypeId(String.valueOf(ID_TYPE_ID.getDbId())).setIdNumber(ID_NUMBER).setLocale(LOCALE),
                INTERNAL_ID, PVV, Map.of())).thenReturn(entity);
        when(entityRegistrationService.saveMsisdn(entity, MsisdnType.PRIMARY, MOBILE, FIRST_NAME + " " + LAST_NAME)).thenReturn(new EntityMsisdn().setMsisdn(MOBILE));
        when(currencyRepository.findByIsoCode(Currency.KES)).thenReturn(Optional.of(currency));
        when(entityRegistrationService.saveAccount(entity, currency, PRIMARY_ACCOUNT, null)).thenReturn(primaryAccount);
        when(entityRegistrationService.saveAccount(entity, currency, FNDS_ACCOUNT, null)).thenReturn(fndsAccount);
        when(keycloakService.createUser(entity.keycloakUsername(), PIN, FIRST_NAME, LAST_NAME, EMAIL)).thenReturn(KEYCLOAK_ID);
        when(kenProperties.isRegisterNotificationSmsEnable()).thenReturn(true);
        when(kenProperties.getRegisterNotificationSmsMessageEn()).thenReturn("Your new Voucher Account Number is %s.\nYour PIN is %s.");

        RegisterKenResponse actualResponse = service.registerUser(request, null, OTP, false);
        assertThat(actualResponse.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(actualResponse.getMessage()).isEqualTo("Registration processed successfully");
        assertThat(actualResponse.getAccountNumber()).isEqualTo(FNDS_ACC_NUMBER);
        assertThat(actualResponse.getEntity()).isEqualTo(entity);
        assertThat(actualResponse.getDate()).isNotNull();
        verify(otpService).validateOtp(OtpType.FNDS_REG_USER, MOBILE, OTP);
        verify(permissionsGroupService).grantKenPermissionsToAccounts(
                List.of(AccountTypeKen.Farmer.getSecurityGroupId()), entity, primaryAccount, fndsAccount);
        verify(documentsService).assignDocumentToEntity(entity, ID_UPLOAD_DOCUMENT_ID);
        verify(documentsService).assignDocumentToEntity(entity, BIO_UPLOAD_DOCUMENT_ID);
        verify(notificationService).sendSmsMessage(String.format("Your new Voucher Account Number is %s.\nYour PIN is %s.", FNDS_ACC_NUMBER, PIN), MOBILE);
    }
}

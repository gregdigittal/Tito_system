package cash.ice.api.service.impl;

import cash.ice.api.config.property.MozProperties;
import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.OtpType;
import cash.ice.api.dto.RegisterEntityRequest;
import cash.ice.api.dto.moz.*;
import cash.ice.api.errors.MozRegistrationException;
import cash.ice.api.service.*;
import cash.ice.common.constant.EntityMetaKey;
import cash.ice.common.dto.ResponseStatus;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static cash.ice.api.dto.moz.AccountTypeMoz.CommuterRegular;
import static cash.ice.api.dto.moz.AccountTypeMoz.TransportOwnerPrivate;
import static cash.ice.common.error.ErrorCodes.EC1071;
import static cash.ice.sqldb.entity.AccountType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityRegistrationMozServiceImplTest {
    private static final String PIN = "1234";
    private static final String INTERNAL_ID = "4321";
    private static final String NUIT = "12345";
    private static final String KEYCLOAK_ID = "32";
    private static final int CURRENCY_ID = 1;
    private static final String PVV = "Pvv";
    private static final String SUBSIDY_ACC_NUMBER = "3456";
    private static final String PREPAID_ACC_NUMBER = "4567";
    private static final int ENTITY_ID = 12;
    private static final int PRIMARY_ACCOUNT_ID = 14;
    private static final String FIRST_NAME = "first";
    private static final String LAST_NAME = "last";
    private static final IdTypeMoz ID_TYPE_ID = IdTypeMoz.ID;
    private static final String ID_NUMBER = "1423";
    private static final String EMAIL = "test@test.com";
    private static final String MOBILE = "123-45-67";
    private static final Locale LOCALE = Locale.ENGLISH;
    private static final String COMPANY_NAME = "CompanyName";
    private static final String NUEL = "SomeNuel";
    private static final String COMPANY_EMAIL = "companyEmail";
    private static final String COMPANY_MOBILE = "SomeCompanyMobile";
    private static final int ID_UPLOAD_DOCUMENT_ID = 12;
    private static final int NUIT_UPLOAD_DOCUMENT_ID = 13;
    private static final int NUEL_UPLOAD_DOCUMENT_ID = 14;
    private static final int NUIT_UPLOAD_DOCUMENT_ID2 = 15;
    private static final int LANGUAGE_ID = 43;
    private static final String AGREEMENT_PREFIX = "moz.user.register.agreement.";
    private static final String AGREEMENT_CONTENT = "AgreementContent";
    private static final String OTP = "1234";

    @Mock
    private EntityRegistrationService entityRegistrationService;
    @Mock
    private OtpService otpService;
    @Mock
    private SecurityPvvService securityPvvService;
    @Mock
    private KeycloakService keycloakService;
    @Mock
    private PermissionsGroupService permissionsGroupService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PermissionsService permissionsService;
    @Mock
    private DocumentsService documentsService;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private EntityMsisdnRepository entityMsisdnRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private MozProperties mozProperties;
    @Mock
    private DictionaryRepository dictionaryRepository;
    @Mock
    private LanguageRepository languageRepository;
    @InjectMocks
    private EntityRegistrationMozServiceImpl service;

    @Test
    void testRegisterEntity() {
        RegisterEntityMozRequest request = new RegisterEntityMozRequest().setAccountType(TransportOwnerPrivate).setPin(PIN).setFirstName(FIRST_NAME).setLastName(LAST_NAME)
                .setMobile(MOBILE).setEmail(EMAIL).setPin(PIN).setIdType(ID_TYPE_ID).setIdNumber(ID_NUMBER).setNuit(NUIT)
                .setIdUploadDocumentId(ID_UPLOAD_DOCUMENT_ID).setNuitUploadDocumentId(NUIT_UPLOAD_DOCUMENT_ID).setLocale(LOCALE);
        EntityClass entity = new EntityClass().setId(ENTITY_ID).setPvv(PVV).setFirstName(FIRST_NAME).setLastName(LAST_NAME).setEmail(EMAIL).setLocale(LOCALE);
        Currency currency = new Currency().setId(CURRENCY_ID).setIsoCode(Currency.MZN);
        Account primaryAccount = new Account().setId(PRIMARY_ACCOUNT_ID);
        Account subsidyAccount = new Account().setAccountNumber(SUBSIDY_ACC_NUMBER);
        Account prepaidAccount = new Account().setAccountNumber(PREPAID_ACC_NUMBER);

        when(mozProperties.isUserRegCheckOtp()).thenReturn(true);
        when(mozProperties.isValidateEmailUniqueness()).thenReturn(true);
        when(entityRepository.existsAccountByEmail(EMAIL)).thenReturn(false);
        when(mozProperties.isValidatePhoneUniqueness()).thenReturn(true);
        when(entityMsisdnRepository.existsByMsisdn(MOBILE)).thenReturn(false);
        when(mozProperties.isValidateIdUniqueness()).thenReturn(true);
        when(entityRepository.existsAccountByIdNumberAndIdType(ID_NUMBER, ID_TYPE_ID.getDbId())).thenReturn(false);
        when(entityRegistrationService.generateInternalId()).thenReturn(INTERNAL_ID);
        when(securityPvvService.acquirePvv(INTERNAL_ID, PIN)).thenReturn(PVV);
        when(entityRegistrationService.saveEntity(new RegisterEntityRequest().setEntityType(EntityType.PRIVATE).setFirstName(FIRST_NAME).setLastName(LAST_NAME)
                        .setEmail(EMAIL).setIdTypeId(String.valueOf(ID_TYPE_ID.getDbId())).setIdNumber(ID_NUMBER).setLocale(LOCALE),
                INTERNAL_ID, PVV, Map.of(EntityMetaKey.AccountTypeMoz, TransportOwnerPrivate, EntityMetaKey.Nuit, NUIT))).thenReturn(entity);
        when(entityRegistrationService.saveMsisdn(entity, MsisdnType.PRIMARY, MOBILE, FIRST_NAME + " " + LAST_NAME)).thenReturn(new EntityMsisdn().setMsisdn(MOBILE));
        when(currencyRepository.findByIsoCode(Currency.MZN)).thenReturn(Optional.of(currency));
        when(entityRegistrationService.saveAccount(entity, currency, PRIMARY_ACCOUNT, null)).thenReturn(primaryAccount);
        when(entityRegistrationService.saveAccount(entity, currency, SUBSIDY_ACCOUNT, null)).thenReturn(subsidyAccount);
        when(entityRegistrationService.saveAccount(entity, currency, PREPAID_TRANSPORT, null)).thenReturn(prepaidAccount);
        when(keycloakService.createUser(entity.keycloakUsername(), PVV, FIRST_NAME, LAST_NAME, EMAIL)).thenReturn(KEYCLOAK_ID);
        when(mozProperties.isRegisterNotificationSmsEnable()).thenReturn(true);
        when(mozProperties.getRegisterNotificationSmsMessageEn()).thenReturn("notification account number: %s");

        RegisterMozResponse actualResponse = service.registerUser(request, null, null, OTP, false);
        assertThat(actualResponse.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(actualResponse.getMessage()).isEqualTo("Registration processed successfully");
        assertThat(actualResponse.getAccountNumber()).isEqualTo(PREPAID_ACC_NUMBER);
        assertThat(actualResponse.getEntity()).isEqualTo(entity);
        assertThat(actualResponse.getDate()).isNotNull();
        verify(otpService).validateOtp(OtpType.MOZ_REG_USER, MOBILE, OTP);
        verify(permissionsGroupService).grantMozPermissionsToAccounts(List.of(TransportOwnerPrivate.getSecurityGroupId(), CommuterRegular.getSecurityGroupId()),
                entity, primaryAccount, subsidyAccount, prepaidAccount);
        verify(documentsService).assignDocumentToEntity(entity, ID_UPLOAD_DOCUMENT_ID);
        verify(documentsService).assignDocumentToEntity(entity, NUIT_UPLOAD_DOCUMENT_ID);
        verify(notificationService).sendSmsMessage("notification account number: " + PREPAID_ACC_NUMBER, MOBILE);
    }

    @Test
    void testRegisterByAgentFail() {
        AuthUser authUser = new AuthUser().setPrincipal("regular agent");
        RegisterEntityMozRequest request = new RegisterEntityMozRequest().setAccountType(CommuterRegular).setPin(PIN).setFirstName(FIRST_NAME).setLastName(LAST_NAME)
                .setMobile(MOBILE).setEmail(EMAIL).setPin(PIN).setIdType(ID_TYPE_ID).setIdNumber(ID_NUMBER).setNuit(NUIT)
                .setIdUploadDocumentId(ID_UPLOAD_DOCUMENT_ID).setNuitUploadDocumentId(NUIT_UPLOAD_DOCUMENT_ID).setLocale(LOCALE);

        when(mozProperties.isUserRegCheckOtp()).thenReturn(true);
        when(mozProperties.isValidateEmailUniqueness()).thenReturn(true);
        when(entityRepository.existsAccountByEmail(EMAIL)).thenReturn(false);
        when(mozProperties.isValidatePhoneUniqueness()).thenReturn(true);
        when(entityMsisdnRepository.existsByMsisdn(MOBILE)).thenReturn(false);
        when(mozProperties.isValidateIdUniqueness()).thenReturn(true);
        when(entityRepository.existsAccountByIdNumberAndIdType(ID_NUMBER, ID_TYPE_ID.getDbId())).thenReturn(false);
        when(mozProperties.getAgentRegularRegisterPermissions()).thenReturn(List.of());
        when(permissionsService.getAuthEntity(authUser)).thenReturn(new EntityClass().setMeta(Map.of(EntityMetaKey.AccountTypeMoz, AccountTypeMoz.AgentRegular)));

        MozRegistrationException exception = assertThrows(MozRegistrationException.class,
                () -> service.registerUser(request, null, authUser, OTP, false));
        AssertionsForClassTypes.assertThat(exception.getErrorCode()).isEqualTo(EC1071);
    }

    @Test
    void testRegisterCorporateEntity() {
        RegisterCompanyMozRequest company = new RegisterCompanyMozRequest().setName(COMPANY_NAME).setNuel(NUEL).setEmail(COMPANY_EMAIL)
                .setMobile(COMPANY_MOBILE).setNuelUploadDocumentId(NUEL_UPLOAD_DOCUMENT_ID).setNuitUploadDocumentId(NUIT_UPLOAD_DOCUMENT_ID2);
        RegisterEntityMozRequest representative = new RegisterEntityMozRequest().setAccountType(TransportOwnerPrivate).setPin(PIN).setFirstName(FIRST_NAME).setLastName(LAST_NAME)
                .setMobile(MOBILE).setEmail(EMAIL).setPin(PIN).setIdType(ID_TYPE_ID).setIdNumber(ID_NUMBER).setNuit(NUIT)
                .setIdUploadDocumentId(ID_UPLOAD_DOCUMENT_ID).setNuitUploadDocumentId(NUIT_UPLOAD_DOCUMENT_ID).setLocale(LOCALE);
        EntityClass entity = new EntityClass().setId(ENTITY_ID).setPvv(PVV).setFirstName(FIRST_NAME).setLastName(LAST_NAME).setEmail(EMAIL).setLocale(LOCALE);
        Currency currency = new Currency().setId(CURRENCY_ID).setIsoCode(Currency.MZN);
        Account primaryAccount = new Account().setId(PRIMARY_ACCOUNT_ID);
        Account subsidyAccount = new Account().setAccountNumber(SUBSIDY_ACC_NUMBER);
        Account prepaidAccount = new Account().setAccountNumber(PREPAID_ACC_NUMBER);

        when(mozProperties.isUserRegCheckOtp()).thenReturn(true);
        when(mozProperties.isValidateCompanyUniqueness()).thenReturn(true);
        when(entityRepository.existsAccountByFirstName(COMPANY_NAME)).thenReturn(false);
        when(mozProperties.isValidateCompanyNuelUniqueness()).thenReturn(true);
        when(entityRepository.existsAccountByIdNumberAndIdType(NUEL, IdTypeMoz.NUEL.getDbId())).thenReturn(false);
        when(mozProperties.isValidateEmailUniqueness()).thenReturn(true);
        when(entityRepository.existsAccountByEmail(COMPANY_EMAIL)).thenReturn(false);
        when(mozProperties.isValidatePhoneUniqueness()).thenReturn(true);
        when(entityMsisdnRepository.existsByMsisdn(COMPANY_MOBILE)).thenReturn(false);
        when(mozProperties.isValidateEmailUniqueness()).thenReturn(true);
        when(entityRepository.existsAccountByEmail(EMAIL)).thenReturn(false);
        when(mozProperties.isValidatePhoneUniqueness()).thenReturn(true);
        when(entityMsisdnRepository.existsByMsisdn(MOBILE)).thenReturn(false);
        when(mozProperties.isValidateIdUniqueness()).thenReturn(true);
        when(entityRepository.existsAccountByIdNumberAndIdType(ID_NUMBER, ID_TYPE_ID.getDbId())).thenReturn(false);
        when(entityRegistrationService.generateInternalId()).thenReturn(INTERNAL_ID);
        when(securityPvvService.acquirePvv(INTERNAL_ID, PIN)).thenReturn(PVV);
        when(entityRegistrationService.saveEntity(new RegisterEntityRequest().setEntityType(EntityType.BUSINESS).setFirstName(COMPANY_NAME)
                        .setEmail(COMPANY_EMAIL).setIdTypeId(String.valueOf(IdTypeMoz.NUEL.getDbId())).setIdNumber(NUEL).setLocale(LOCALE),
                INTERNAL_ID, PVV, Map.of(EntityMetaKey.AccountTypeMoz, TransportOwnerPrivate, EntityMetaKey.Representative, Map.of(
                        "firstName", FIRST_NAME, "lastName", LAST_NAME, "idType", ID_TYPE_ID, "idNumber",
                        ID_NUMBER, "nuit", NUIT, "mobile", MOBILE, "email", EMAIL)))).thenReturn(entity);
        when(entityRegistrationService.saveMsisdn(entity, MsisdnType.PRIMARY, COMPANY_MOBILE, null)).thenReturn(new EntityMsisdn().setMsisdn(MOBILE));

        when(currencyRepository.findByIsoCode(Currency.MZN)).thenReturn(Optional.of(currency));
        when(entityRegistrationService.saveAccount(entity, currency, PRIMARY_ACCOUNT, null)).thenReturn(primaryAccount);
        when(entityRegistrationService.saveAccount(entity, currency, SUBSIDY_ACCOUNT, null)).thenReturn(subsidyAccount);
        when(entityRegistrationService.saveAccount(entity, currency, PREPAID_TRANSPORT, null)).thenReturn(prepaidAccount);
        when(keycloakService.createUser(entity.keycloakUsername(), PVV, FIRST_NAME, LAST_NAME, EMAIL)).thenReturn(KEYCLOAK_ID);
        when(mozProperties.isRegisterNotificationSmsEnable()).thenReturn(true);
        when(mozProperties.getRegisterNotificationSmsMessageEn()).thenReturn("notification account number: %s");

        RegisterMozResponse actualResponse = service.registerCorporateUser(company, representative, null, null, OTP, false);
        assertThat(actualResponse.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(actualResponse.getMessage()).isEqualTo("Registration processed successfully");
        assertThat(actualResponse.getAccountNumber()).isEqualTo(PREPAID_ACC_NUMBER);
        assertThat(actualResponse.getEntity()).isEqualTo(entity);
        assertThat(actualResponse.getDate()).isNotNull();
        verify(otpService).validateOtp(OtpType.MOZ_REG_USER, COMPANY_MOBILE, OTP);
        verify(permissionsGroupService).grantMozPermissionsToAccounts(List.of(TransportOwnerPrivate.getSecurityGroupId(), CommuterRegular.getSecurityGroupId()),
                entity, primaryAccount, subsidyAccount, prepaidAccount);
        verify(documentsService).assignDocumentToEntity(entity, NUEL_UPLOAD_DOCUMENT_ID);
        verify(documentsService).assignDocumentToEntity(entity, NUIT_UPLOAD_DOCUMENT_ID2);
        verify(documentsService).assignDocumentToEntity(entity, ID_UPLOAD_DOCUMENT_ID);
        verify(documentsService).assignDocumentToEntity(entity, NUIT_UPLOAD_DOCUMENT_ID);
        verify(notificationService).sendSmsMessage("notification account number: " + PREPAID_ACC_NUMBER, MOBILE);
    }

    @Test
    void testRegistrationAgreement() {
        AccountTypeMoz accountType = CommuterRegular;
        when(languageRepository.findByLanguageKey(Locale.ENGLISH.getLanguage()))
                .thenReturn(Optional.of(new Language().setId(LANGUAGE_ID)));
        when(mozProperties.getRegAgreementPrefix()).thenReturn(AGREEMENT_PREFIX);
        when(dictionaryRepository.findByLanguageIdAndLookupKey(LANGUAGE_ID, AGREEMENT_PREFIX + accountType.getTypeString()))
                .thenReturn(Optional.of(new Dictionary().setValue(AGREEMENT_CONTENT)));
        Dictionary actualDictionary = service.getRegistrationAgreement(Locale.ENGLISH, accountType);
        assertThat(actualDictionary.getValue()).isEqualTo(AGREEMENT_CONTENT);
    }
}
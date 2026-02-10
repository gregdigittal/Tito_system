package cash.ice.api.service.impl;

import cash.ice.api.config.property.EntitiesProperties;
import cash.ice.api.config.property.MfaProperties;
import cash.ice.api.dto.RegisterEntityRequest;
import cash.ice.api.dto.RegisterResponse;
import cash.ice.api.errors.RegistrationException;
import cash.ice.api.service.KeycloakService;
import cash.ice.api.service.MfaService;
import cash.ice.api.service.SecurityPvvService;
import cash.ice.common.constant.EntityMetaKey;
import cash.ice.common.dto.ResponseStatus;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static cash.ice.common.error.ErrorCodes.EC1011;
import static cash.ice.common.error.ErrorCodes.EC1060;
import static cash.ice.sqldb.entity.AccountType.PRIMARY_ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntityRegistrationServiceImplTest {
    private static final String ZWL = "ZWL";
    private static final String PIN = "1234";
    private static final int ENTITY_ID = 11;
    private static final int ACCOUNT_ID = 12;
    private static final String MOBILE = "123-45-67";
    private static final String PVV = "testPvv";
    private static final String FIRST_NAME = "first";
    private static final String LAST_NAME = "last";
    private static final String ID_TYPE_ID = "1";
    private static final String ID_NUMBER = "1234";
    private static final String EMAIL = "test@test.com";
    private static final String ENTITY_TYPE = "Personal";
    private static final String COMPANY = "ICEcash";
    private static final String CARD = "1234567890123456";
    private static final int INITIATOR_ID = 14;
    private static final int CURRENCY_ID = 1;
    private static final int ACCOUNT_TYPE_ID = 1;
    private static final String CONTACT_NAME = "ContactName";
    private static final String ALT_MOBILE = "2345";
    private static final String ALT_CONTACT_NAME = "AltContactName";
    private static final int CITIZENSHIP_COUNTRY_ID = 35;
    private static final int COUNTRY_ID = 14;
    private static final String CITY = "City";
    private static final String POSTAL_CODE = "postalCode";
    private static final String ADDRESS_1 = "address1";
    private static final String ADDRESS_2 = "address2";
    private static final String ADDRESS_NOTES = "addressNotes";

    @Mock
    private SecurityPvvService securityPvvService;
    @Mock
    private KeycloakService keycloakService;
    @Mock
    private MfaService mfaService;
    @Mock
    private EntityTypeRepository entityTypeRepository;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private EntityMsisdnRepository entityMsisdnRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private AccountTypeRepository accountTypeRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private MetaDataRepository metaDataRepository;
    @Mock
    private InitiatorRepository initiatorRepository;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private EntitiesProperties entitiesProperties;
    @Captor
    private ArgumentCaptor<String> internalIdArgumentCaptor;
    @Captor
    private ArgumentCaptor<EntityClass> entityArgumentCaptor;
    @Captor
    private ArgumentCaptor<EntityMsisdn> msisdnArgumentCaptor;
    @Captor
    private ArgumentCaptor<Account> accountArgumentCaptor;
    @Captor
    private ArgumentCaptor<Address> accressArgumentCaptor;
    @InjectMocks
    private EntityRegistrationServiceImpl service;

    @Test
    void testRegisterUser() {
        RegisterEntityRequest request = new RegisterEntityRequest().setFirstName(FIRST_NAME).setLastName(LAST_NAME)
                .setIdTypeId(ID_TYPE_ID).setIdNumber(ID_NUMBER).setEntityType(ENTITY_TYPE).setMobile(MOBILE)
                .setKycStatus(KYC.PARTIAL).setTransactionLimitTier(LimitTier.Tier1).setCorporateFee(true)
                .setGender(Gender.MALE).setAuthorisationType(AuthorisationType.SINGLE).setContactName(CONTACT_NAME)
                .setAltMobile(ALT_MOBILE).setAltContactName(ALT_CONTACT_NAME).setCitizenshipCountryId(CITIZENSHIP_COUNTRY_ID)
                .setAddress(new RegisterEntityRequest.Address().setCountryId(COUNTRY_ID).setCity(CITY).setPostalCode(POSTAL_CODE)
                        .setAddress1(ADDRESS_1).setAddress2(ADDRESS_2).setNotes(ADDRESS_NOTES))
                .setEmail(EMAIL).setCompany(COMPANY).setCard(CARD);

        when(currencyRepository.findByIsoCode(ZWL)).thenReturn(Optional.of(new Currency().setId(CURRENCY_ID).setIsoCode(ZWL)));
        when(securityPvvService.acquirePvv(anyString(), anyString())).thenReturn(PVV);
        when(entityTypeRepository.findByDescription(ENTITY_TYPE)).thenReturn(Optional.of(new EntityType().setId(ENTITY_ID)));
        when(entityRepository.save(any(EntityClass.class))).thenAnswer(invocation -> {
            ((EntityClass) invocation.getArgument(0)).setId(ENTITY_ID);
            return invocation.getArguments()[0];
        });
        when(accountTypeRepository.findByNameAndCurrencyId(PRIMARY_ACCOUNT, CURRENCY_ID)).thenReturn(Optional.of(new AccountType().setId(ACCOUNT_TYPE_ID)));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            ((Account) invocation.getArgument(0)).setId(ACCOUNT_ID);
            return invocation.getArguments()[0];
        });
        when(metaDataRepository.existsByName(EntityMetaKey.Company)).thenReturn(false);
        when(initiatorRepository.findByIdentifier(CARD)).thenReturn(Optional.of(new Initiator().setId(INITIATOR_ID)));
        when(countryRepository.existsById(CITIZENSHIP_COUNTRY_ID)).thenReturn(true);
        when(countryRepository.existsById(COUNTRY_ID)).thenReturn(true);
        when(entitiesProperties.getMfa()).thenReturn(new MfaProperties());
        when(mfaService.generateSecretCode()).thenReturn("secretCode");
        when(mfaService.generateBackupCodes(any())).thenReturn(List.of("backupCode"));

        RegisterResponse response = service.registerEntity(request);

        verifyRepositories();
        verify(keycloakService).createUser("30000011", PIN, FIRST_NAME, LAST_NAME, EMAIL);

        assertThat(response.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(response.getMessage()).isEqualTo("Registration processed successfully");
        assertThat(response.getDate()).isNotNull();
    }

    private void verifyRepositories() {
        verify(entityRepository, times(2)).save(entityArgumentCaptor.capture());
        EntityClass entity = entityArgumentCaptor.getValue();
        assertThat(entity.getStatus()).isEqualTo(EntityStatus.ACTIVE);
        assertThat(entity.getLoginStatus()).isEqualTo(LoginStatus.ACTIVE);
        assertThat(entity.getEntityTypeId()).isEqualTo(ENTITY_ID);
        assertThat(entity.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(entity.getLastName()).isEqualTo(LAST_NAME);
        assertThat(entity.getIdType()).isEqualTo(Integer.valueOf(ID_TYPE_ID));
        assertThat(entity.getIdNumber()).isEqualTo(ID_NUMBER);
        assertThat(entity.getKycStatusId()).isEqualTo(KYC.PARTIAL.ordinal());
        assertThat(entity.getGender()).isEqualTo(Gender.MALE);
        assertThat(entity.getCitizenshipCountryId()).isEqualTo(CITIZENSHIP_COUNTRY_ID);
        assertThat(entity.getInternalId()).isNotNull();
        assertThat(entity.getPvv()).isEqualTo(PVV);
        assertThat(entity.getEmail()).isEqualTo(EMAIL);
        assertThat(entity.getCreatedDate()).isNotNull();
        assertThat(entity.getMeta()).isEqualTo(Map.of(
                EntityMetaKey.Company, COMPANY,
                EntityMetaKey.TransactionLimitTier, LimitTier.Tier1,
                EntityMetaKey.CorporateFee, true));

        verify(entityMsisdnRepository, times(2)).save(msisdnArgumentCaptor.capture());
        List<EntityMsisdn> mobiles = msisdnArgumentCaptor.getAllValues();
        assertThat(mobiles.size()).isEqualTo(2);
        EntityMsisdn msisdn = mobiles.get(0);
        assertThat(msisdn.getEntityId()).isEqualTo(ENTITY_ID);
        assertThat(msisdn.getMsisdnType()).isEqualTo(MsisdnType.PRIMARY);
        assertThat(msisdn.getMsisdn()).isEqualTo(MOBILE);
        assertThat(msisdn.getDescription()).isEqualTo(CONTACT_NAME);
        assertThat(msisdn.getCreatedDate()).isNotNull();
        EntityMsisdn msisdn2 = mobiles.get(1);
        assertThat(msisdn2.getEntityId()).isEqualTo(ENTITY_ID);
        assertThat(msisdn2.getMsisdnType()).isEqualTo(MsisdnType.SECONDARY);
        assertThat(msisdn2.getMsisdn()).isEqualTo(ALT_MOBILE);
        assertThat(msisdn2.getDescription()).isEqualTo(ALT_CONTACT_NAME);
        assertThat(msisdn2.getCreatedDate()).isNotNull();

        verify(addressRepository).save(accressArgumentCaptor.capture());
        Address address = accressArgumentCaptor.getValue();
        assertThat(address.getEntityId()).isEqualTo(ENTITY_ID);
        assertThat(address.getCountryId()).isEqualTo(COUNTRY_ID);
        assertThat(address.getCity()).isEqualTo(CITY);
        assertThat(address.getAddressType()).isEqualTo(AddressType.PRIMARY);
        assertThat(address.getPostalCode()).isEqualTo(POSTAL_CODE);
        assertThat(address.getAddress1()).isEqualTo(ADDRESS_1);
        assertThat(address.getAddress2()).isEqualTo(ADDRESS_2);
        assertThat(address.getNotes()).isEqualTo(ADDRESS_NOTES);

        verify(accountRepository).save(accountArgumentCaptor.capture());
        Account account = accountArgumentCaptor.getValue();
        assertThat(account.getEntityId()).isEqualTo(ENTITY_ID);
        assertThat(account.getAccountTypeId()).isEqualTo(1);
        assertThat(account.getAccountNumber()).isNotNull();
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getAuthorisationType()).isEqualTo(AuthorisationType.SINGLE);
        assertThat(account.getCreatedDate()).isNotNull();

        verify(metaDataRepository).save(new MetaData().setTable("entity").setName(EntityMetaKey.Company));
        verify(metaDataRepository).save(new MetaData().setTable("entity").setName(EntityMetaKey.TransactionLimitTier));
        verify(metaDataRepository).save(new MetaData().setTable("entity").setName(EntityMetaKey.CorporateFee));
    }

    @Test
    void testSimpleRegisterUser() {
        RegisterEntityRequest request = new RegisterEntityRequest().setFirstName(FIRST_NAME).setLastName(LAST_NAME)
                .setIdTypeId(ID_TYPE_ID).setIdNumber(ID_NUMBER).setEntityType(ENTITY_TYPE).setMobile(MOBILE)
                .setEmail(EMAIL).setCompany(COMPANY).setCard(CARD);

        when(currencyRepository.findByIsoCode(ZWL)).thenReturn(Optional.of(new Currency().setId(CURRENCY_ID).setIsoCode(ZWL)));
        when(securityPvvService.acquirePvv(anyString(), anyString())).thenReturn(PVV);
        when(entityTypeRepository.findByDescription(ENTITY_TYPE)).thenReturn(Optional.of(new EntityType().setId(ENTITY_ID)));
        when(entityRepository.save(any(EntityClass.class))).thenAnswer(invocation -> {
            ((EntityClass) invocation.getArgument(0)).setId(ENTITY_ID);
            return invocation.getArguments()[0];
        });
        when(accountTypeRepository.findByNameAndCurrencyId(PRIMARY_ACCOUNT, CURRENCY_ID)).thenReturn(Optional.of(new AccountType().setId(ACCOUNT_TYPE_ID)));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            ((Account) invocation.getArgument(0)).setId(ACCOUNT_ID);
            return invocation.getArguments()[0];
        });
        when(metaDataRepository.existsByName(EntityMetaKey.Company)).thenReturn(false);
        when(initiatorRepository.findByIdentifier(CARD)).thenReturn(Optional.of(new Initiator().setId(INITIATOR_ID)));
        when(entitiesProperties.getMfa()).thenReturn(new MfaProperties());
        when(mfaService.generateSecretCode()).thenReturn("secretCode");
        when(mfaService.generateBackupCodes(any())).thenReturn(List.of("backupCode"));

        RegisterResponse response = service.registerEntity(request);

        verifyRepositoriesForSimpleReg();
        verify(keycloakService).createUser("30000011", PIN, FIRST_NAME, LAST_NAME, EMAIL);

        assertThat(response.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(response.getMessage()).isEqualTo("Registration processed successfully");
        assertThat(response.getDate()).isNotNull();
    }

    private void verifyRepositoriesForSimpleReg() {
        verify(entityRepository, times(2)).save(entityArgumentCaptor.capture());
        EntityClass entity = entityArgumentCaptor.getValue();
        assertThat(entity.getStatus()).isEqualTo(EntityStatus.ACTIVE);
        assertThat(entity.getLoginStatus()).isEqualTo(LoginStatus.ACTIVE);
        assertThat(entity.getEntityTypeId()).isEqualTo(ENTITY_ID);
        assertThat(entity.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(entity.getLastName()).isEqualTo(LAST_NAME);
        assertThat(entity.getIdType()).isEqualTo(Integer.valueOf(ID_TYPE_ID));
        assertThat(entity.getIdNumber()).isEqualTo(ID_NUMBER);
        assertThat(entity.getInternalId()).isNotNull();
        assertThat(entity.getPvv()).isEqualTo(PVV);
        assertThat(entity.getEmail()).isEqualTo(EMAIL);
        assertThat(entity.getCreatedDate()).isNotNull();
        assertThat(entity.getMeta()).isEqualTo(Map.of(EntityMetaKey.Company, COMPANY));

        verify(entityMsisdnRepository).save(msisdnArgumentCaptor.capture());
        EntityMsisdn msisdn = msisdnArgumentCaptor.getValue();
        assertThat(msisdn.getEntityId()).isEqualTo(ENTITY_ID);
        assertThat(msisdn.getMsisdnType()).isEqualTo(MsisdnType.PRIMARY);
        assertThat(msisdn.getMsisdn()).isEqualTo(MOBILE);
        assertThat(msisdn.getCreatedDate()).isNotNull();

        verify(accountRepository).save(accountArgumentCaptor.capture());
        Account account = accountArgumentCaptor.getValue();
        assertThat(account.getEntityId()).isEqualTo(ENTITY_ID);
        assertThat(account.getAccountTypeId()).isEqualTo(1);
        assertThat(account.getAccountNumber()).isNotNull();
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getCreatedDate()).isNotNull();

        verify(metaDataRepository).save(new MetaData().setTable("entity").setName(EntityMetaKey.Company));
    }

    @Test
    void testRegisterUserWithUnknownType() {
        when(currencyRepository.findByIsoCode(ZWL)).thenReturn(Optional.of(new Currency().setId(CURRENCY_ID).setIsoCode(ZWL)));
        RegisterEntityRequest request = new RegisterEntityRequest();
        RegistrationException exception = assertThrows(RegistrationException.class,
                () -> service.registerEntity(request));
        assertThat(exception.getErrorCode()).isEqualTo(EC1011);
    }

    @Test
    void testRegisterUserWithUnknownCard() {
        RegisterEntityRequest request = new RegisterEntityRequest().setFirstName(FIRST_NAME).setLastName(LAST_NAME)
                .setIdTypeId(ID_TYPE_ID).setIdNumber(ID_NUMBER).setEntityType(ENTITY_TYPE).setMobile(MOBILE)
                .setEmail(EMAIL).setCompany(COMPANY).setCard(CARD);

        when(currencyRepository.findByIsoCode(ZWL)).thenReturn(Optional.of(new Currency().setId(CURRENCY_ID).setIsoCode(ZWL)));
        when(securityPvvService.acquirePvv(internalIdArgumentCaptor.capture(), anyString())).thenReturn(PVV);
        when(entityTypeRepository.findByDescription(ENTITY_TYPE)).thenReturn(Optional.of(new EntityType().setId(ENTITY_ID)));
        when(entityRepository.save(any(EntityClass.class))).thenAnswer(invocation -> {
            ((EntityClass) invocation.getArgument(0)).setId(ENTITY_ID);
            return invocation.getArguments()[0];
        });

        RegistrationException exception = assertThrows(RegistrationException.class, () -> service.registerEntity(request));
        assertThat(exception.getErrorCode()).isEqualTo(EC1060);
    }
}

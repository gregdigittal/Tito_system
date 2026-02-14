package cash.ice.api.service.impl;

import cash.ice.api.config.property.EntitiesProperties;
import cash.ice.api.dto.RegisterEntityRequest;
import cash.ice.api.dto.RegisterResponse;
import cash.ice.api.errors.MozRegistrationException;
import cash.ice.api.errors.RegistrationException;
import cash.ice.api.service.*;
import cash.ice.common.constant.EntityMetaKey;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static cash.ice.common.error.ErrorCodes.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntityRegistrationServiceImpl implements EntityRegistrationService {
    private final SecurityPvvService securityPvvService;
    private final KeycloakService keycloakService;
    private final NotificationService notificationService;
    private final MfaService mfaService;
    private final EntityTypeRepository entityTypeRepository;
    private final EntityRepository entityRepository;
    private final EntityMsisdnRepository entityMsisdnRepository;
    private final AddressRepository addressRepository;
    private final CurrencyRepository currencyRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final AccountRepository accountRepository;
    private final MetaDataRepository metaDataRepository;
    private final InitiatorRepository initiatorRepository;
    private final CountryRepository countryRepository;
    private final EntitiesProperties entitiesProperties;

    @Value("${ice.cash.registration.sms-pin}")
    private boolean smsPin;

    @Override
    @Transactional(timeout = 30)
    public RegisterResponse registerEntity(RegisterEntityRequest request) {
        validateRequest(request);
        String pin = Tool.generateDigits(4, false);
        String internalId = generateInternalId();
        String accountNumber = generateAccountNumber();
        String pvv = securityPvvService.acquirePvv(internalId, pin);
        return registerEntity(request, pin, internalId, pvv, accountNumber, "ZWL", smsPin);
    }

    private void validateRequest(RegisterEntityRequest request) {
        if (entitiesProperties.isValidateEmailUniqueness() && entityRepository.existsAccountByEmail(request.getEmail())) {
            throw new MozRegistrationException(EC1001, "Email already exists", true);
        } else if (entitiesProperties.isValidatePhoneUniqueness() && entityMsisdnRepository.existsByMsisdn(request.getMobile())) {
            throw new MozRegistrationException(EC1001, "Mobile number already exists", true);
        } else if (entitiesProperties.isValidateIdUniqueness() && entityRepository.existsAccountByIdNumberAndIdType(request.getIdNumber(), Integer.valueOf(request.getIdTypeId()))) {
            throw new MozRegistrationException(EC1001, "ID already exists", true);
        }
    }

    @Override
    @Transactional(timeout = 30)
    public RegisterResponse registerEntity(RegisterEntityRequest request, String pin, String internalId, String pvv, String accountNumber, String currencyCode, boolean sendPinBySms) {
        Currency currency = currencyRepository.findByIsoCode(currencyCode).orElseThrow(() ->
                new RegistrationException(EC1062, String.format("Currency '%s' does not exist", currencyCode)));
        return registerEntity(request, pin, internalId, pvv, accountNumber, currency, sendPinBySms);
    }

    @Override
    @Transactional(timeout = 30)
    public RegisterResponse registerEntity(RegisterEntityRequest request, String pin, String internalId, String pvv, String accountNumber, Currency currency, boolean sendPinBySms) {
        EntityClass entity = saveEntity(request, internalId, pvv, Map.of());
        if (request.getMobile() != null) {
            saveMsisdn(entity, MsisdnType.PRIMARY, request.getMobile(), request.getContactName());
        }
        if (request.getAltMobile() != null) {
            saveMsisdn(entity, MsisdnType.SECONDARY, request.getAltMobile(), request.getAltContactName());
        }
        if (request.getAddress() != null) {
            saveAddress(entity, AddressType.PRIMARY, request.getAddress());
        }
        Account primaryAccount = saveAccount(entity, AccountType.PRIMARY_ACCOUNT, currency, accountNumber, request.getAuthorisationType());
        saveInitiator(request.getCard(), primaryAccount);
        String keycloakId = keycloakService.createUser(entity.keycloakUsername(), pin,
                request.getFirstName(), request.getLastName(), request.getEmail());
        try {
            entityRepository.save(entity.setKeycloakId(keycloakId));
            if (sendPinBySms) {
                notificationService.sendSmsPinCode(pin, request.getMobile());
            }
        } catch (Exception e) {
            keycloakService.removeUser(keycloakId);
            throw e;
        }
        return RegisterResponse.success(accountNumber, entity, primaryAccount);
    }

    @Override
    public boolean isExistsId(Integer idTypeId, String idNumber, boolean forceCheck) {
        return (forceCheck || entitiesProperties.isValidateIdUniqueness()) && entityRepository.existsAccountByIdNumberAndIdType(idNumber, idTypeId);
    }

    @Override
    public boolean isExistsEmail(String email, boolean forceCheck) {
        return (forceCheck || entitiesProperties.isValidateEmailUniqueness()) && entityRepository.existsAccountByEmail(email);
    }

    @Override
    public boolean isExistsMsisdn(String msisdn, boolean checkOnlyPrimary, boolean forceCheck) {
        if (forceCheck || entitiesProperties.isValidatePhoneUniqueness()) {
            if (checkOnlyPrimary) {
                return entityMsisdnRepository.existsByMsisdnAndMsisdnType(msisdn, MsisdnType.PRIMARY);
            } else {
                return entityMsisdnRepository.existsByMsisdn(msisdn);
            }
        }
        return false;
    }

    @Override
    public EntityClass saveEntity(RegisterEntityRequest request, String internalId, String pvv, Map<String, Object> addToMetadata) {
        EntityType entityType = entityTypeRepository.findByDescription(request.getEntityType()).orElseThrow(() ->
                new RegistrationException(EC1011, "Unknown entityType: " + request.getEntityType()));
        EntityClass entityClass = new EntityClass()
                .setEntityTypeId(entityType.getId())
                .setFirstName(request.getFirstName())
                .setLastName(request.getLastName())
                .setIdType(Integer.valueOf(request.getIdTypeId()))
                .setIdNumber(request.getIdNumber())
                .setInternalId(internalId)
                .setPvv(pvv)
                .setStatus(request.getStatus() != null ? request.getStatus() : EntityStatus.ACTIVE)
                .setLoginStatus(request.getLoginStatus() != null ? request.getLoginStatus() : LoginStatus.ACTIVE)
                .setEmail(request.getEmail())
                .setMfaSecretCode(mfaService.generateSecretCode())
                .setMfaBackupCodes(mfaService.generateBackupCodes(entitiesProperties.getMfa()))
                .setKycStatusId(request.getKycStatus() != null ? request.getKycStatus().ordinal() : 0)
                .setCitizenshipCountryId(checkCountryId(request.getCitizenshipCountryId()))
                .setGender(request.getGender())
                .setLocale(request.getLocale() != null ? request.getLocale() : Locale.ENGLISH)
                .setCreatedDate(Tool.currentDateTime());
        addMetadataIfNeed(entityClass, request, addToMetadata);
        log.debug("  Saving new entity: {}", entityClass);
        return entityRepository.save(entityClass);
    }

    @Override
    public EntityMsisdn saveMsisdn(EntityClass entityClass, MsisdnType type, String msisdn, String contactName) {
        log.debug("  Saving new {} msisdn: {}, contactName: {}", type, msisdn, contactName);
        return entityMsisdnRepository.save(new EntityMsisdn()
                .setEntityId(entityClass.getId())
                .setMsisdnType(type)
                .setMsisdn(msisdn)
                .setDescription(contactName)
                .setCreatedDate(Tool.currentDateTime()));
    }

    @Override
    public Address saveAddress(EntityClass entity, AddressType addressType, RegisterEntityRequest.Address address) {
        log.debug("  Saving new {} address: {}", addressType, address);
        return addressRepository.save(new Address()
                .setEntityId(entity.getId())
                .setAddressType(addressType)
                .setCountryId(checkCountryId(address.getCountryId()))
                .setCity(address.getCity())
                .setPostalCode(address.getPostalCode())
                .setAddress1(address.getAddress1())
                .setAddress2(address.getAddress2())
                .setNotes(address.getNotes()));
    }

    @Override
    public Account saveAccount(EntityClass entity, Currency currency, String accountTypeName, AuthorisationType authorisationType) {
        return saveAccount(entity, accountTypeName, currency, generateAccountNumber(), authorisationType);
    }

    private Account saveAccount(EntityClass entity, String accountTypeName, Currency currency, String accountNumber, AuthorisationType authorisationType) {
        AccountType accountType = accountTypeRepository.findByNameAndCurrencyId(accountTypeName, currency.getId()).orElseThrow(() ->
                new RegistrationException(EC1060, String.format("Account Type '%s' for '%s' does not exist", accountTypeName, currency.getIsoCode())));
        Account account = new Account()
                .setEntityId(entity.getId())
                .setAccountTypeId(accountType.getId())
                .setAccountNumber(accountNumber)
                .setAccountStatus(AccountStatus.ACTIVE)
                .setAuthorisationType(authorisationType)
                .setCreatedDate(Tool.currentDateTime());
        log.debug("  Saving new account: {}", account);
        return accountRepository.save(account);
    }

    private void saveInitiator(String identifier, Account account) {
        if (identifier != null) {
            Initiator initiator = initiatorRepository.findByIdentifier(identifier).orElseThrow(() ->
                    new RegistrationException(EC1012, String.format("Initiator '%s' does not exist", identifier)));
            initiator.setAccountId(account.getId());
        }
    }

    private Integer checkCountryId(Integer countryId) {
        if (countryId != null && !countryRepository.existsById(countryId)) {
            throw new ICEcashException(String.format("Country with ID: %s does not exist", countryId), EC1065);
        }
        return countryId;
    }

    private void addMetadataIfNeed(EntityClass entityClass, RegisterEntityRequest request, Map<String, Object> addToMetadata) {
        HashMap<String, Object> metadata = new HashMap<>();
        addMetaItemIfNeed(metadata, EntityMetaKey.Company, request.getCompany());
        addMetaItemIfNeed(metadata, EntityMetaKey.TransactionLimitTier, request.getTransactionLimitTier());
        addMetaItemIfNeed(metadata, EntityMetaKey.CorporateFee, request.getCorporateFee());
        addToMetadata.forEach((itemType, itemValue) -> addMetaItemIfNeed(metadata, itemType, itemValue));
        entityClass.setMeta(!metadata.isEmpty() ? metadata : null);
    }

    private void addMetaItemIfNeed(HashMap<String, Object> metadata, String itemType, Object itemValue) {
        if (!ObjectUtils.isEmpty(itemValue)) {
            if (!metaDataRepository.existsByName(itemType)) {
                metaDataRepository.save(new MetaData().setTable("entity").setName(itemType));
            }
            metadata.put(itemType, itemValue);
        }
    }

    /**
     * @return Random 16 digits number
     */
    @Override
    public String generateInternalId() {
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            String id = Tool.generateDigits(EntityClass.INTERNAL_NUM_LENGTH, true);
            if (!entityRepository.existsAccountByInternalId(id)) {
                return id;
            }
        }
        throw new RegistrationException(EC1009, "Cannot generate internal id");
    }

    /**
     * @return Random 8 digits number starting from 3
     */

    @Override
    public String generateAccountNumber() {
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            String id = Tool.generateDigits(Account.NUMBER_LENGTH, true, Account.NUMBER_PREFIX);
            if (!accountRepository.existsAccountByAccountNumber(id)) {
                return id;
            }
        }
        throw new RegistrationException(EC1009, "Cannot generate account number");
    }
}

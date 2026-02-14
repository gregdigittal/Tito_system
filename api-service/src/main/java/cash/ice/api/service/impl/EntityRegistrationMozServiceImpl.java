package cash.ice.api.service.impl;

import cash.ice.api.config.property.MozProperties;
import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.OtpType;
import cash.ice.api.dto.RegisterEntityRequest;
import cash.ice.api.dto.RegisterResponse;
import cash.ice.api.dto.moz.*;
import cash.ice.api.errors.MozRegistrationException;
import cash.ice.api.service.*;
import cash.ice.common.constant.EntityMetaKey;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import io.minio.ObjectWriteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static cash.ice.common.error.ErrorCodes.EC1001;
import static cash.ice.common.error.ErrorCodes.EC1062;
import static cash.ice.sqldb.entity.AccountType.*;
import static cash.ice.sqldb.entity.Currency.MZN;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntityRegistrationMozServiceImpl implements EntityRegistrationMozService {
    private static final String PORTUGESE_LOCALE_STRING = "pt";

    private final EntityRegistrationService entityRegistrationService;
    private final OtpService otpService;
    private final SecurityPvvService securityPvvService;
    private final KeycloakService keycloakService;
    private final FileMozService fileMozService;
    private final PermissionsGroupService permissionsGroupService;
    private final NotificationService notificationService;
    private final PermissionsService permissionsService;
    private final DocumentsService documentsService;
    private final EntityRepository entityRepository;
    private final EntityMsisdnRepository entityMsisdnRepository;
    private final CurrencyRepository currencyRepository;
    private final DictionaryRepository dictionaryRepository;
    private final LanguageRepository languageRepository;
    private final MozProperties mozProperties;

    @Override
    @Transactional(timeout = 30)
    @Deprecated
    public RegisterResponse registerEntity(RegisterEntityMozRequest request) {
        validateRequest(request);
        String internalId = entityRegistrationService.generateInternalId();
        Currency currency = currencyRepository.findByIsoCode(MZN).orElseThrow(() ->
                new MozRegistrationException(EC1062, String.format("Currency '%s' does not exist", MZN), false));
        RegisterResponse response = entityRegistrationService.registerEntity(
                toRegisterEntityRequest(request),
                request.getPin(),
                internalId,
                securityPvvService.acquirePvv(internalId, request.getPin()),
                entityRegistrationService.generateAccountNumber(),
                currency,
                false);
        ObjectWriteResponse photoResponse = null;
        try {
            Account subsidyAccount = entityRegistrationService.saveAccount(response.getEntity(), currency, SUBSIDY_ACCOUNT, null);
            Account prepaidAccount = entityRegistrationService.saveAccount(response.getEntity(), currency, PREPAID_TRANSPORT, null);
            permissionsGroupService.grantMozPermissionsToAccounts(List.of(mozProperties.getNewUserSecurityGroupId()),
                    response.getEntity(), response.getPrimaryAccount(), subsidyAccount, prepaidAccount);
            response.setAccountNumber(prepaidAccount.getAccountNumber());           // we use Prepaid account number instead of Primary
            photoResponse = fileMozService.savePhoto(request.getPhoto(), request.getPhotoFileName(), response.getEntity());
            sendSmsIfNeed(response.getEntity().getLocale(), response.getAccountNumber(), request.getMobile());
        } catch (Exception e) {
            keycloakService.removeUser(response.getEntity().getKeycloakId());
            if (photoResponse != null) {
                fileMozService.removePhoto(response.getEntity());
            }
            throw e;
        }
        return response;
    }

    private RegisterEntityRequest toRegisterEntityRequest(RegisterEntityMozRequest request) {
        return new RegisterEntityRequest()
                .setFirstName(request.getFirstName())
                .setLastName(request.getLastName())
                .setEmail(request.getEmail())
                .setMobile(request.getMobile())
                .setEntityType(EntityType.PRIVATE)
                .setIdTypeId(String.valueOf(request.getIdType().getDbId()))
                .setIdNumber(request.getIdNumber())
                .setLocale(request.getLocale());
    }

    @Override
    @Transactional(timeout = 30)
    public RegisterMozResponse registerUser(RegisterEntityMozRequest request, OptionalEntityRegisterData optionalData, AuthUser authUser, String otp, boolean removeDocumentsOnFail) {
        try {
            if (needValidateOtp(authUser)) {
                otpService.validateOtp(OtpType.MOZ_REG_USER, request.getMobile(), otp);
            }
            validateRequest(request);
            EntityClass authEntity = checkAndGetAgentAuthEntity(authUser, request.getAccountType());
            String pin = request.getPin() != null ? request.getPin() : Tool.generateDigits(4, false);
            String internalId = entityRegistrationService.generateInternalId();
            EntityClass entity = entityRegistrationService.saveEntity(
                    addOptionalData(optionalData, new RegisterEntityRequest()
                            .setEntityType(EntityType.PRIVATE)
                            .setFirstName(request.getFirstName())
                            .setLastName(request.getLastName())
                            .setIdTypeId(String.valueOf(request.getIdType().getDbId()))
                            .setIdNumber(request.getIdNumber())
                            .setEmail(request.getEmail())
                            .setLocale(request.getLocale())),
                    internalId,
                    securityPvvService.acquirePvv(internalId, pin),
                    Tool.newMetaMap().put(EntityMetaKey.AccountTypeMoz, request.getAccountType())
                            .putIfNonNull(EntityMetaKey.Nuit, request.getNuit())
                            .putIfNonNull(EntityMetaKey.CreatedByStaffId, authUser != null && authUser.isStaffMember() ? authUser.getPrincipal() : null)
                            .build());
            EntityMsisdn mobile = entityRegistrationService.saveMsisdn(entity, MsisdnType.PRIMARY, request.getMobile(),
                    optionalData != null && optionalData.getContactName() != null ? optionalData.getContactName() :
                            String.format("%s %s", request.getFirstName(), request.getLastName()));
            if (optionalData != null && optionalData.getAltMobile() != null) {
                entityRegistrationService.saveMsisdn(entity, MsisdnType.SECONDARY, optionalData.getAltMobile(), optionalData.getAltContactName());
            }
            if (optionalData != null && optionalData.getAddress() != null) {
                entityRegistrationService.saveAddress(entity, AddressType.PRIMARY, optionalData.getAddress());
            }
            Account prepaidAccount = registerAccounts(entity, request.getAccountType().getSecurityGroupId(),
                    optionalData != null ? optionalData.getAuthorisationType() : null);
            documentsService.assignDocumentToEntity(entity, request.getIdUploadDocumentId());
            documentsService.assignDocumentToEntity(entity, request.getNuitUploadDocumentId());
            return finishUserRegistration(entity, prepaidAccount.getAccountNumber(), mobile.getMsisdn(), authEntity,
                    pin, request.getPin() == null ? pin : null);
        } catch (Exception e) {
            removeDocumentsIfNeed(removeDocumentsOnFail, request.getIdUploadDocumentId(), request.getNuitUploadDocumentId());
            throw e;
        }
    }

    @Override
    @Transactional(timeout = 30)
    public RegisterMozResponse registerCorporateUser(RegisterCompanyMozRequest company, RegisterEntityMozRequest representative, OptionalEntityRegisterData optionalData, AuthUser authUser, String otp, boolean removeDocumentsOnFail) {
        try {
            if (needValidateOtp(authUser)) {
                otpService.validateOtp(OtpType.MOZ_REG_USER, company.getMobile(), otp);
            }
            validateCompany(company, IdTypeMoz.NUEL.getDbId());
            validateRequest(representative);
            EntityClass authEntity = checkAndGetAgentAuthEntity(authUser, representative.getAccountType());
            String pin = representative.getPin() != null ? representative.getPin() : Tool.generateDigits(4, false);
            String internalId = entityRegistrationService.generateInternalId();
            EntityClass entity = entityRegistrationService.saveEntity(
                    addOptionalData(optionalData, new RegisterEntityRequest()
                            .setEntityType(EntityType.BUSINESS)
                            .setFirstName(company.getName())
                            .setIdTypeId(String.valueOf(IdTypeMoz.NUEL.getDbId()))
                            .setIdNumber(company.getNuel())
                            .setEmail(company.getEmail())
                            .setLocale(representative.getLocale())),
                    internalId,
                    securityPvvService.acquirePvv(internalId, pin),
                    Tool.newMetaMap().put(EntityMetaKey.AccountTypeMoz, representative.getAccountType())
                            .putIfNonNull(EntityMetaKey.Nuit, company.getNuit())
                            .putIfNonNull(EntityMetaKey.CreatedByStaffId, authUser != null && authUser.isStaffMember() ? authUser.getPrincipal() : null)
                            .putIfNonNull(EntityMetaKey.Representative, Map.of(
                                    "firstName", representative.getFirstName(),
                                    "lastName", representative.getLastName(),
                                    "idType", representative.getIdType(),
                                    "idNumber", representative.getIdNumber(),
                                    "nuit", representative.getNuit(),
                                    "mobile", representative.getMobile(),
                                    "email", representative.getEmail()
                            )).build());
            EntityMsisdn mobile = entityRegistrationService.saveMsisdn(entity, MsisdnType.PRIMARY, company.getMobile(),
                    optionalData != null && optionalData.getContactName() != null ? optionalData.getContactName() : null);
            EntityMsisdn repMobile = entityRegistrationService.saveMsisdn(entity, MsisdnType.SECONDARY,
                    representative.getMobile(), String.format("%s %s", representative.getFirstName(), representative.getLastName()));
            if (optionalData != null && optionalData.getAltMobile() != null) {
                entityRegistrationService.saveMsisdn(entity, MsisdnType.SECONDARY, optionalData.getAltMobile(), optionalData.getAltContactName());
            }
            entityRegistrationService.saveAddress(entity, AddressType.BUSINESS, company.getAddress());
            if (optionalData != null && optionalData.getAddress() != null) {
                entityRegistrationService.saveAddress(entity, AddressType.PRIMARY, optionalData.getAddress());
            }
            Account prepaidAccount = registerAccounts(entity, representative.getAccountType().getSecurityGroupId(),
                    optionalData != null ? optionalData.getAuthorisationType() : null);
            documentsService.assignDocumentToEntity(entity, company.getNuelUploadDocumentId());
            documentsService.assignDocumentToEntity(entity, company.getNuitUploadDocumentId());
            documentsService.assignDocumentToEntity(entity, representative.getIdUploadDocumentId());
            documentsService.assignDocumentToEntity(entity, representative.getNuitUploadDocumentId());
            return finishUserRegistration(entity, prepaidAccount.getAccountNumber(), mobile.getMsisdn(), authEntity,
                    pin, representative.getPin() == null ? pin : null);
        } catch (Exception e) {
            removeDocumentsIfNeed(removeDocumentsOnFail, company.getNuelUploadDocumentId(), company.getNuitUploadDocumentId(), representative.getIdUploadDocumentId(), representative.getNuitUploadDocumentId());
            throw e;
        }
    }

    private boolean needValidateOtp(AuthUser authUser) {
        return authUser == null || !authUser.isStaffMember() ? mozProperties.isUserRegCheckOtp() : mozProperties.isEntityRegByStaffCheckOtp();
    }

    private RegisterEntityRequest addOptionalData(OptionalEntityRegisterData optionalData, RegisterEntityRequest request) {
        if (optionalData != null) {
            request.setStatus(optionalData.getStatus())
                    .setLoginStatus(optionalData.getLoginStatus())
                    .setKycStatus(optionalData.getKycStatus())
                    .setCitizenshipCountryId(optionalData.getCitizenshipCountryId())
                    .setGender(optionalData.getGender())
                    .setCorporateFee(optionalData.getCorporateFee())
                    .setTransactionLimitTier(optionalData.getTransactionLimitTier())
                    .setCompany(optionalData.getCompany());
        }
        return request;
    }

    private void removeDocumentsIfNeed(boolean needRemoveDocuments, Integer... documentsIds) {
        if (needRemoveDocuments) {
            try {
                for (Integer documentsId : documentsIds) {
                    if (documentsId != null) {
                        documentsService.deleteDocument(documentsId);
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public Dictionary getRegistrationAgreement(Locale locale, AccountTypeMoz accountType) {
        String lang = locale != null ? locale.getLanguage() : Locale.ENGLISH.getLanguage();
        Language language = languageRepository.findByLanguageKey(lang).orElseThrow(() ->
                new ICEcashException("Unknown language is assigned to user: " + lang, ErrorCodes.EC1039));
        String lookupKey = mozProperties.getRegAgreementPrefix() + accountType.getTypeString();
        return dictionaryRepository.findByLanguageIdAndLookupKey(language.getId(), lookupKey).orElseThrow(() ->
                new ICEcashException(String.format("dictionary for lookupKey: %s and language: %s is not available",
                        lookupKey, language.getLanguageKey()), ErrorCodes.EC1044));
    }

    private Account registerAccounts(EntityClass entity, int securityGroupId, AuthorisationType authorisationType) {
        Currency currency = currencyRepository.findByIsoCode(MZN).orElseThrow(() ->
                new MozRegistrationException(EC1062, String.format("Currency '%s' does not exist", MZN), false));
        Account primaryAccount = entityRegistrationService.saveAccount(entity, currency, PRIMARY_ACCOUNT, authorisationType);
        Account subsidyAccount = entityRegistrationService.saveAccount(entity, currency, SUBSIDY_ACCOUNT, authorisationType);
        Account prepaidAccount = entityRegistrationService.saveAccount(entity, currency, PREPAID_TRANSPORT, authorisationType);
        permissionsGroupService.grantMozPermissionsToAccounts(List.of(securityGroupId, AccountTypeMoz.CommuterRegular.getSecurityGroupId()),
                entity, primaryAccount, subsidyAccount, prepaidAccount);
        return prepaidAccount;
    }

    private RegisterMozResponse finishUserRegistration(EntityClass entity, String accountNumber, String msisdn,
                                                       EntityClass authEntity, String keycloakPassword, String smsPin) {
        String keycloakId = keycloakService.createUser(entity.keycloakUsername(), keycloakPassword,
                entity.getFirstName(), entity.getLastName(), entity.getEmail());
        try {
            entityRepository.save(entity.setKeycloakId(keycloakId)
                    .setRegByEntityId(authEntity != null ? authEntity.getId() : null));
            sendSmsIfNeed(entity.getLocale(), accountNumber, msisdn);
            if (smsPin != null) {
                notificationService.sendSmsPinCode(smsPin, msisdn);
            }
            return RegisterMozResponse.success(accountNumber, entity);
        } catch (Exception e) {
            keycloakService.removeUser(keycloakId);
            throw e;
        }
    }

    private EntityClass checkAndGetAgentAuthEntity(AuthUser authUser, AccountTypeMoz registrableUserType) {
        if (authUser != null && !authUser.isStaffMember()) {
            EntityClass authEntity = permissionsService.getAuthEntity(authUser);
            if (authEntity.getMeta() != null) {
                AccountTypeMoz accountType = AccountTypeMoz.valueOf(authEntity.getMeta().get(EntityMetaKey.AccountTypeMoz).toString());
                if (AccountTypeMoz.AgentRegular.equals(accountType) && mozProperties.getAgentRegularRegisterPermissions().contains(registrableUserType)
                        || AccountTypeMoz.AgentFematro.equals(accountType) && mozProperties.getAgentFematroRegisterPermissions().contains(registrableUserType)) {
                    return authEntity;
                }
            }
            throw new MozRegistrationException(ErrorCodes.EC1071, "You are not an agent or operation is not allowed", true);
        }
        return null;
    }

    private void validateRequest(RegisterEntityMozRequest request) {
        if (request.getPin() != null && !request.getPin().matches("^[0-9]{4,}$")) {
            throw new MozRegistrationException(EC1001, "PIN must contain only digits and have size at least 4", true);
        } else if (request.getEmail() != null && mozProperties.isValidateEmailUniqueness() && entityRepository.existsAccountByEmail(request.getEmail())) {
            throw new MozRegistrationException(EC1001, "Such email already exists", true);
        } else if (mozProperties.isValidatePhoneUniqueness() && entityMsisdnRepository.existsByMsisdn(request.getMobile())) {
            throw new MozRegistrationException(EC1001, "Such mobile number already exists", true);
        } else if (mozProperties.isValidateIdUniqueness() && entityRepository.existsAccountByIdNumberAndIdType(request.getIdNumber(), request.getIdType().getDbId())) {
            throw new MozRegistrationException(EC1001, "Such ID already exists", true);
        }
    }

    private void validateCompany(RegisterCompanyMozRequest company, int businessIdTypeId) {
        if (mozProperties.isValidateCompanyUniqueness() && entityRepository.existsAccountByFirstName(company.getName())) {
            throw new MozRegistrationException(EC1001, "Such company name already registered", true);
        } else if (mozProperties.isValidateCompanyNuelUniqueness() && entityRepository.existsAccountByIdNumberAndIdType(company.getNuel(), businessIdTypeId)) {
            throw new MozRegistrationException(EC1001, "Such ID already exists", true);
        } else if (company.getEmail() != null && mozProperties.isValidateEmailUniqueness() && entityRepository.existsAccountByEmail(company.getEmail())) {
            throw new MozRegistrationException(EC1001, "Such company email already exists", true);
        } else if (mozProperties.isValidatePhoneUniqueness() && entityMsisdnRepository.existsByMsisdn(company.getMobile())) {
            throw new MozRegistrationException(EC1001, "Such company mobile number already exists", true);
        }
    }

    private void sendSmsIfNeed(Locale locale, String accountNumber, String mobile) {
        if (mozProperties.isRegisterNotificationSmsEnable()) {
            notificationService.sendSmsMessage(String.format(PORTUGESE_LOCALE_STRING.equals(locale.toString()) ?
                            mozProperties.getRegisterNotificationSmsMessagePt() : mozProperties.getRegisterNotificationSmsMessageEn(),
                    accountNumber), mobile);
        }
    }
}

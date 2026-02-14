package cash.ice.api.service.impl;

import cash.ice.api.config.property.KenProperties;
import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.OtpType;
import cash.ice.api.dto.RegisterEntityRequest;
import cash.ice.api.dto.moz.AccountTypeKen;
import cash.ice.api.dto.moz.RegisterEntityKenRequest;
import cash.ice.api.dto.moz.RegisterKenResponse;
import cash.ice.api.errors.ForbiddenException;
import cash.ice.api.errors.KenRegistrationException;
import cash.ice.api.service.*;
import cash.ice.common.error.ApiValidationException;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.CurrencyRepository;
import cash.ice.sqldb.repository.EntityMsisdnRepository;
import cash.ice.sqldb.repository.EntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static cash.ice.common.error.ErrorCodes.EC1001;
import static cash.ice.common.error.ErrorCodes.EC1062;
import static cash.ice.sqldb.entity.AccountType.FNDS_ACCOUNT;
import static cash.ice.sqldb.entity.AccountType.PRIMARY_ACCOUNT;
import static cash.ice.sqldb.entity.Currency.KES;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntityRegistrationKenServiceImpl implements EntityRegistrationKenService {
    private final EntityRegistrationService entityRegistrationService;
    private final OtpService otpService;
    private final SecurityPvvService securityPvvService;
    private final DocumentsService documentsService;
    private final PermissionsService permissionsService;
    private final PermissionsGroupService permissionsGroupService;
    private final KeycloakService keycloakService;
    private final NotificationService notificationService;
    private final EntityRepository entityRepository;
    private final CurrencyRepository currencyRepository;
    private final EntityMsisdnRepository entityMsisdnRepository;
    private final KenProperties kenProperties;

    @Override
    @Transactional(timeout = 30)
    public RegisterKenResponse registerUser(RegisterEntityKenRequest request, AuthUser authUser, String otp, boolean removeDocumentsOnFail) {
        try {
            validateRequest(request, otp);
            EntityClass authEntity = checkAndGetAgentAuthEntity(authUser);
            String pin = request.getPin() != null ? request.getPin() : Tool.generateDigits(4, false);
            String internalId = entityRegistrationService.generateInternalId();
            EntityClass entity = entityRegistrationService.saveEntity(new RegisterEntityRequest()
                            .setEntityType(request.getAccountType().getEntityType())
                            .setFirstName(request.getFirstName())
                            .setLastName(request.getLastName())
                            .setIdTypeId(String.valueOf(request.getIdType().getDbId()))
                            .setIdNumber(request.getIdNumber())
                            .setEmail(request.getEmail())
                            .setLocale(request.getLocale()),
                    internalId,
                    securityPvvService.acquirePvv(internalId, pin),
                    Map.of());
            EntityMsisdn mobile = entityRegistrationService.saveMsisdn(entity, MsisdnType.PRIMARY, request.getMobile(),
                    String.format("%s %s", request.getFirstName(), request.getLastName()));
            Account fndsAccount = registerAccounts(entity, request.getAccountType().getSecurityGroupId());
            documentsService.assignDocumentToEntity(entity, request.getIdUploadDocumentId());
            documentsService.assignDocumentToEntity(entity, request.getBiometricUploadDocumentId());
            return finishUserRegistration(entity, fndsAccount.getAccountNumber(), mobile.getMsisdn(), authEntity, pin);
        } catch (Exception e) {
            removeDocumentsIfNeed(removeDocumentsOnFail, request.getIdUploadDocumentId(), request.getBiometricUploadDocumentId());
            throw e;
        }
    }

    private EntityClass checkAndGetAgentAuthEntity(AuthUser authUser) {
        if (authUser != null && !authUser.isStaffMember()) {
            EntityClass authEntity = permissionsService.getAuthEntity(authUser);
            if (permissionsGroupService.hasUserKenSecurityGroup(authEntity, AccountTypeKen.Agent.getSecurityGroupId())) {
                return authEntity;
            }
            throw new ForbiddenException("You are not an agent or operation is not allowed", ErrorCodes.EC1071);
        }
        return null;
    }

    private RegisterKenResponse finishUserRegistration(EntityClass entity, String accountNumber, String msisdn, EntityClass authEntity, String pin) {
        String keycloakId = keycloakService.createUser(entity.keycloakUsername(), pin,
                entity.getFirstName(), entity.getLastName(), entity.getEmail());
        try {
            entityRepository.save(entity.setKeycloakId(keycloakId)
                    .setRegByEntityId(authEntity != null ? authEntity.getId() : null));
            sendSmsIfNeed(accountNumber, pin, msisdn);
            return RegisterKenResponse.success(accountNumber, entity);
        } catch (Exception e) {
            keycloakService.removeUser(keycloakId);
            throw e;
        }
    }

    private void sendSmsIfNeed(String accountNumber, String pin, String mobile) {
        if (kenProperties.isRegisterNotificationSmsEnable()) {
            notificationService.sendSmsMessage(String.format(kenProperties.getRegisterNotificationSmsMessageEn(),
                    accountNumber, pin), mobile);
        }
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

    private Account registerAccounts(EntityClass entity, int securityGroupId) {
        Currency currency = currencyRepository.findByIsoCode(KES).orElseThrow(() ->
                new KenRegistrationException(EC1062, String.format("Currency '%s' does not exist", KES), false));
        Account primaryAccount = entityRegistrationService.saveAccount(entity, currency, PRIMARY_ACCOUNT, null);
        Account fndsAccount = entityRegistrationService.saveAccount(entity, currency, FNDS_ACCOUNT, null);
        permissionsGroupService.grantKenPermissionsToAccounts(List.of(securityGroupId), entity, primaryAccount, fndsAccount);
        return fndsAccount;
    }

    private void validateRequest(RegisterEntityKenRequest request, String otp) {
        if (kenProperties.isUserRegCheckOtp()) {
            otpService.validateOtp(OtpType.FNDS_REG_USER, request.getMobile(), otp);
        }
        if (request.getPin() != null && !request.getPin().matches("^[0-9]{4,}$")) {
            throw new ApiValidationException("PIN must have size 4-14 digits and contain only digits", EC1001);
        } else if (request.getEmail() != null && kenProperties.isValidateEmailUniqueness() && entityRepository.existsAccountByEmail(request.getEmail())) {
            throw new ApiValidationException("Such email already exists", EC1001);
        } else if (kenProperties.isValidatePhoneUniqueness() && entityMsisdnRepository.existsByMsisdn(request.getMobile())) {
            throw new ApiValidationException("Such mobile number already exists", EC1001);
        } else if (kenProperties.isValidateIdUniqueness() && entityRepository.existsAccountByIdNumberAndIdType(request.getIdNumber(), request.getIdType().getDbId())) {
            throw new ApiValidationException("Such ID already exists", EC1001);
        }
    }
}

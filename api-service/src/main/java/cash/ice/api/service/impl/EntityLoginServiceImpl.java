package cash.ice.api.service.impl;

import cash.ice.api.config.property.EntitiesProperties;
import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.LoginEntityRequest;
import cash.ice.api.dto.LoginMfaRequest;
import cash.ice.api.dto.LoginResponse;
import cash.ice.api.errors.LockLoginException;
import cash.ice.api.errors.UnexistingUserException;
import cash.ice.api.service.*;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import jakarta.ws.rs.NotAuthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static cash.ice.common.utils.Tool.checkPinIsValid;

@Service("EntityLoginService")
@RequiredArgsConstructor
@Primary
@Slf4j
public class EntityLoginServiceImpl implements EntityLoginService {
    protected final EntityRepository entityRepository;
    protected final AccountRepository accountRepository;
    private final EntityService entityService;
    private final SecurityPvvService securityPvvService;
    private final KeycloakService keycloakService;
    private final MfaService mfaService;
    private final NotificationService notificationService;
    private final PermissionsService permissionsService;
    private final InitiatorRepository initiatorRepository;
    private final EntityMsisdnRepository entityMsisdnRepository;
    private final ChannelRepository channelRepository;
    private final DictionaryRepository dictionaryRepository;
    private final EntitiesProperties entitiesProperties;

    @Override
    @Transactional(timeout = 30)
    public AccessTokenResponse simpleLogin(LoginEntityRequest request) {
        EntityClass entity = findActiveEntity(request.getUsername());
        if (entity.getLoginStatus() != LoginStatus.ACTIVE) {
            throw new ICEcashException(String.format("Entity is %s for login", entity.getId()), ErrorCodes.EC1035);
        }
        // Keycloak resource-owner password grant expects the plain password
        return keycloakService.loginUser(
                request.getGrantType(),
                entity.keycloakUsername(),
                request.getPassword(),
                request.getClientId(),
                request.getClientSecret());
    }

    @Override
    @Transactional(timeout = 30)
    public LoginResponse makeLogin(LoginEntityRequest request) {
        EntityClass entity = findActiveEntity(request.getUsername());
        // Keycloak resource-owner password grant expects the plain password, not the PVV (stored hash)
        AccessTokenResponse accessToken = loginEntity(entity.keycloakUsername(), request.getPassword());
        try {
            return mfaService.handleLogin(entity.idString(), accessToken, entity.getMfaType(), entity.getMfaSecretCode(), getMsisdn(entity), entitiesProperties.getMfa())
                    .setLocale(entity.getLocale())
                    .setEntity(entity);
        } catch (LockLoginException e) {
            throw lockUserLogin(entity, e.getInitialException());
        }
    }

    @Override
    public LoginResponse enterLoginMfaCode(LoginMfaRequest mfaRequest) {
        EntityClass entity = findActiveEntity(mfaRequest.getUsername());
        try {
            return mfaService.enterMfaCode(entity.idString(), mfaRequest.getCode(), entitiesProperties.getMfa())
                    .setLocale(entity.getLocale());
        } catch (LockLoginException e) {
            throw lockUserLogin(entity, e.getInitialException());
        }
    }

    @Override
    public LoginResponse enterLoginMfaBackupCode(LoginMfaRequest mfaRequest) {
        EntityClass entity = findActiveEntity(mfaRequest.getUsername());
        try {
            LoginResponse loginResponse = mfaService.enterBackupCode(entity.idString(), entity.getMfaBackupCodes(), mfaRequest.getCode(), entitiesProperties.getMfa())
                    .setLocale(entity.getLocale());
            if (loginResponse.getStatus() == LoginResponse.Status.SUCCESS &&
                    (loginResponse.getAccessToken() == null || loginResponse.getAccessToken().getToken() == null)) {
                throw new ICEcashException("Access token is expired", ErrorCodes.EC1038);
            }
            entityRepository.save(entity
                    .setMfaBackupCodes(entity.getMfaBackupCodes().stream()
                            .filter(code -> !code.equals(mfaRequest.getCode())).toList()));
            return loginResponse;
        } catch (LockLoginException e) {
            throw lockUserLogin(entity, e.getInitialException());
        }
    }

    @Override
    public boolean checkTotpCode(AuthUser authUser, String totpCode) {
        EntityClass entity = permissionsService.getAuthEntity(authUser);
        return mfaService.checkTotpCode(entity.getMfaSecretCode(), totpCode);
    }

    @Override
    public boolean resendOtpCode(String enterId) {
        EntityClass entity = findActiveEntity(enterId);
        mfaService.resendOtpCode(entity.idString(), getMsisdn(entity), entitiesProperties.getMfa());
        return true;
    }

    @Override
    public boolean forgotPassword(String email, boolean sendEmail, String requestIP) {
        EntityClass entity = entityRepository.findByEmail(email).orElseThrow(() -> {
            log.warn("Forgot password attempt for unknown email: {}, IP: {}", email, requestIP);
            throw new UnexistingUserException("email: " + email);
        });
        String forgotPasswordKey = mfaService.createForgotPasswordKey(entity.idString());
        return notificationService.sendEmailByTemplate(
                sendEmail, entitiesProperties.getForgotPasswordEmailTemplate(),
                entityService.getEntityLanguage(entity).getId(),
                entitiesProperties.getForgotPasswordEmailFrom(),
                List.of(entity.getEmail()),
                Map.of("$username$", entity.getFirstName(), "$key$", forgotPasswordKey));
    }

    @Override
    public EntityClass resetEntityPassword(String key, String newPassword) {
        checkPinIsValid(newPassword);
        String entityId = mfaService.lookupLoginByForgotPasswordKey(key);
        EntityClass entity = entityRepository.findById(Integer.parseInt(entityId))
                .orElseThrow(() -> new UnexistingUserException("entityId: " + entityId));
        EntityClass savedEntity = entityRepository.save(entity
                .setPvv(securityPvvService.acquirePvv(entity.getInternalId(), newPassword)));
        updateKeycloakUser(savedEntity, newPassword);
        return savedEntity;
    }

    @Transactional(timeout = 30)
    @Override
    public EntityClass updateEntityPassword(AuthUser authUser, String oldPassword, String newPassword) {
        checkPinIsValid(newPassword);
        EntityClass entity = permissionsService.getAuthEntity(authUser);
        String pvv = securityPvvService.acquirePvv(entity.getInternalId(), oldPassword);
        if (pvv.equals(entity.getPvv())) {
            EntityClass savedEntity = entityRepository.save(entity
                    .setPvv(securityPvvService.acquirePvv(entity.getInternalId(), newPassword)));
            updateKeycloakUser(savedEntity, newPassword);
            return savedEntity;
        } else {
            throw new ICEcashException("Old password is incorrect", ErrorCodes.EC1032);
        }
    }

    @Transactional(timeout = 30)
    @Override
    public EntityClass updateEntityLoginStatus(String enterId, LoginStatus loginStatus) {
        EntityClass entity = findEntity(enterId);
        if (loginStatus == LoginStatus.ACTIVE && entity.getLoginStatus() != LoginStatus.ACTIVE) {
            mfaService.cleanupLoginData(entity.idString());
        }
        return entityRepository.save(entity.setLoginStatus(loginStatus));
    }

    @Override
    public EntityClass updateEntityMfa(Integer entityId, MfaType mfaType) {
        return entityRepository.save(entityService.getEntityById(entityId).setMfaType(mfaType));
    }

    @Override
    @Transactional(timeout = 30)
    public EntityClass generateNewEntityPassword(Integer entityId) {
        EntityClass entity = entityService.getEntityById(entityId);
        String newPin = generateSecurePassword();
        EntityClass savedEntity = entityRepository.save(entity
                .setPvv(securityPvvService.acquirePvv(entity.getInternalId(), newPin)));
        updateKeycloakUser(savedEntity, newPin);
        notificationService.sendSmsPinCode(newPin, getMsisdn(entity));
        return savedEntity;
    }

    /** Generates a 12-character password with letters, digits and special chars (satisfies typical PIN policy). */
    private static String generateSecurePassword() {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder password = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    @Override
    public String getMfaQrCode(EntityClass entity) {
        String channelLanguageKey = channelRepository.findByCode("ONL").map(Channel::getLanguageKey)
                .orElseThrow(() -> new ICEcashException("language key for ONL channel is not available", ErrorCodes.EC1043));
        Language language = entityService.getEntityLanguage(entity);
        Dictionary dictionary = dictionaryRepository.findByLanguageIdAndLookupKey(language.getId(), channelLanguageKey)
                .orElseThrow(() -> new ICEcashException(String.format("dictionary for lookupKey: %s and language: %s is not available",
                        channelLanguageKey, language.getLanguageKey()), ErrorCodes.EC1044));
        return mfaService.getQrCode(dictionary.getValue(), entity.getEmail(), entity.getMfaSecretCode(), entitiesProperties.getMfa());
    }

    @Override
    public EntityClass findEntity(String enterId) {
        if (enterId.length() == Account.NUMBER_LENGTH && enterId.startsWith(Account.NUMBER_PREFIX)) {         // account number
            EntityClass entity = tryFindByAccountNumber(enterId);
            if (entity != null) {
                return entity;
            }
        }
        if (enterId.length() == 16) {                   // card number
            Optional<Initiator> initiator = initiatorRepository.findByIdentifier(enterId);
            if (initiator.isPresent()) {
                Optional<Account> account = accountRepository.findById(initiator.get().getAccountId());
                if (account.isPresent()) {
                    return entityRepository.findById(account.get().getEntityId())
                            .orElseThrow(() -> new ICEcashException("Entity with id=" + account.get().getEntityId() + " does not exist", ErrorCodes.EC1048));
                }
            }
        }

        // national id
        EntityClass entityClass = tryFindByIdNumber(enterId);
        if (entityClass != null) {
            return entityClass;
        } else {
            throw new UnexistingUserException(enterId);
        }
    }

    protected EntityClass tryFindByAccountNumber(String enterId) {
        List<Account> accounts = accountRepository.findByAccountNumber(enterId);
        if (accounts.size() == 1 || accounts.size() > 1 && accounts.stream().map(Account::getEntityId).distinct().count() == 1) {
            if (accounts.getLast().getAccountStatus() == AccountStatus.ACTIVE) {
                return entityRepository.findById(accounts.getLast().getEntityId())
                        .orElseThrow(() -> new ICEcashException("Entity with id=" + accounts.getLast().getEntityId() + " does not exist", ErrorCodes.EC1048));
            } else {
                throw new ICEcashException(String.format("Account '%s' is not active", enterId), ErrorCodes.EC1074, false);
            }
        } else if (accounts.size() > 1) {
            throw new ICEcashException(String.format("Multiple entities match '%s' account number", enterId), ErrorCodes.EC1013, false);
        } else {
            return null;
        }
    }

    protected EntityClass tryFindByIdNumber(String enterId) {
        List<EntityClass> entityClass = entityRepository.findByIdNumber(enterId);
        if (entityClass.size() > 1) {
            throw new ICEcashException(String.format("Multiple entities match '%s' ID number", enterId), ErrorCodes.EC1013, false);
        } else if (!entityClass.isEmpty()) {
            return entityClass.getLast();
        } else {
            return null;
        }
    }

    protected EntityClass findActiveEntity(String enterId) {
        EntityClass entity = findEntity(enterId);
        if (entity.getLoginStatus() != LoginStatus.ACTIVE) {
            throw new ICEcashException(String.format("Account is %s for login", entity.getLoginStatus()), ErrorCodes.EC1035);
        }
        return entity;
    }

    private AccessTokenResponse loginEntity(String username, String password) {
        try {
            return keycloakService.loginUser(
                    null,
                    username,
                    password,
                    null,
                    null);
        } catch (NotAuthorizedException e) {
            return null;
        }
    }

    private void updateKeycloakUser(EntityClass entity, String password) {
        keycloakService.updateUser(entity.getKeycloakId(), password, entity.getFirstName(),
                entity.getLastName(), entity.getEmail());
    }

    private String getMsisdn(EntityClass entity) {
        return entityMsisdnRepository.findByEntityIdAndPrimaryMsisdn(entity.getId())
                .map(EntityMsisdn::getMsisdn).orElse(null);
    }

    private NotAuthorizedException lockUserLogin(EntityClass entity, NotAuthorizedException exception) {
        log.info("Entity locked: {}", entity.idString());
        entityRepository.save(entity.setLoginStatus(LoginStatus.LOCKED));
        return exception;
    }
}

package cash.ice.api.service.impl;

import cash.ice.api.config.property.StaffProperties;
import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.LoginEntityRequest;
import cash.ice.api.dto.LoginMfaRequest;
import cash.ice.api.dto.LoginResponse;
import cash.ice.api.entity.backoffice.StaffMember;
import cash.ice.api.errors.LockLoginException;
import cash.ice.api.errors.RegistrationException;
import cash.ice.api.errors.UnexistingUserException;
import cash.ice.api.service.*;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.ChannelRepository;
import cash.ice.sqldb.repository.DictionaryRepository;
import jakarta.ws.rs.NotAuthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static cash.ice.common.utils.Tool.checkPinIsValid;

@Service
@Slf4j
@RequiredArgsConstructor
public class StaffMemberLoginServiceImpl implements StaffMemberLoginService {
    private final StaffMemberService staffMemberService;
    private final SecurityPvvService securityPvvService;
    private final KeycloakService backofficeKeycloakService;
    private final PermissionsService permissionsService;
    private final MfaService mfaService;
    private final NotificationService notificationService;
    private final ChannelRepository channelRepository;
    private final DictionaryRepository dictionaryRepository;
    private final StaffProperties staffProperties;

    @Override
    @Transactional
    public StaffMember registerStaffMember(StaffMember staffMember, String password) {
        if (staffMemberService.isStaffMemberExist(staffMember.getEmail())) {
            throw new RegistrationException("such email already registered", ErrorCodes.EC1042);
        }
        String pin = (password != null) ? password : Tool.generateDigits(staffProperties.getPasswordDigitsAmount(), false);
        checkPinIsValid(pin);
        String pinKey = staffMemberService.generateStaffMemberPinKey();
        String pvv = securityPvvService.acquirePvv(pinKey, pin);
        String keycloakId = backofficeKeycloakService.createStaffMember(staffMember.getEmail(), pvv,
                staffMember.getFirstName(), staffMember.getLastName(), staffMember.getEmail());
        try {
            StaffMember savedStaffMember = staffMemberService.save(staffMember
                    .setLoginStatus(LoginStatus.ACTIVE)
                    .setPinKey(pinKey)
                    .setPvv(pvv)
                    .setMfaType(MfaType.OTP)
                    .setMfaSecretCode(mfaService.generateSecretCode())
                    .setKeycloakId(keycloakId)
                    .setMfaBackupCodes(mfaService.generateBackupCodes(staffProperties.getMfa()))
                    .setLocale(staffMember.getLocale() != null ? staffMember.getLocale() : Locale.ENGLISH)
                    .setCreatedDate(Tool.currentDateTime()));
            if (staffProperties.isPasswordSendSms() && password == null) {
                notificationService.sendSmsPinCode(pin, staffMember.getMsisdn());
            }
            return savedStaffMember;
        } catch (Exception e) {
            backofficeKeycloakService.removeUser(keycloakId);
            throw e;
        }
    }

    @Override
    public StaffMember activateNewStaffMember(String key, String newPassword) {
        checkPinIsValid(newPassword);
        String login = mfaService.lookupLoginByForgotPasswordKey(key);
        StaffMember staffMember = staffMemberService.findStaffMember(login);

        String pvv = securityPvvService.acquirePvv(staffMember.getPinKey(), newPassword);
        String keycloakId = backofficeKeycloakService.createStaffMember(staffMember.getEmail(), pvv,
                staffMember.getFirstName(), staffMember.getLastName(), staffMember.getEmail());
        return staffMemberService.save(staffMember
                .setPvv(securityPvvService.acquirePvv(staffMember.getPinKey(), newPassword))
                .setKeycloakId(keycloakId));
    }

    @Override
    public AccessTokenResponse loginFormStaffMember(LoginEntityRequest loginRequest) {
        StaffMember staffMember = staffMemberService.findActiveStaffMember(loginRequest.getUsername());
        String pvv = securityPvvService.acquirePvv(staffMember.getPinKey(), loginRequest.getPassword());
        return loginStaffMember(staffMember.getEmail(), pvv);
    }

    @Override
    public LoginResponse loginStaffMember(LoginEntityRequest loginRequest) {
        StaffMember staffMember = staffMemberService.findActiveStaffMember(loginRequest.getUsername());
        if (staffMember.getKeycloakId() == null) {
            throw new ICEcashException("User account was not activated", ErrorCodes.EC1049);
        }
        String pvv = securityPvvService.acquirePvv(staffMember.getPinKey(), loginRequest.getPassword());
        AccessTokenResponse accessToken = loginStaffMember(staffMember.getEmail(), pvv);
        try {
            LoginResponse response = mfaService.handleLogin(staffMember.getEmail(), accessToken, staffMember.getMfaType(), staffMember.getMfaSecretCode(), staffMember.getMsisdn(), staffProperties.getMfa());
            saveLastLoginIfNeed(response, staffMember);
            return response;
        } catch (LockLoginException e) {
            throw lockUserLogin(staffMember, e.getInitialException());
        }
    }

    @Override
    public LoginResponse enterLoginMfaCode(LoginMfaRequest mfaRequest) {
        StaffMember staffMember = staffMemberService.findActiveStaffMember(mfaRequest.getUsername());
        try {
            LoginResponse response = mfaService.enterMfaCode(staffMember.getEmail(), mfaRequest.getCode(), staffProperties.getMfa());
            saveLastLoginIfNeed(response, staffMember);
            return response;
        } catch (LockLoginException e) {
            throw lockUserLogin(staffMember, e.getInitialException());
        }
    }

    @Override
    public LoginResponse enterLoginMfaBackupCode(LoginMfaRequest mfaRequest) {
        StaffMember staffMember = staffMemberService.findActiveStaffMember(mfaRequest.getUsername());
        try {
            LoginResponse loginResponse = mfaService.enterBackupCode(staffMember.getEmail(), staffMember.getMfaBackupCodes(), mfaRequest.getCode(), staffProperties.getMfa());
            staffMemberService.save(staffMember
                    .setLastLogin(Tool.currentDateTime())
                    .setMfaBackupCodes(staffMember.getMfaBackupCodes().stream()
                            .filter(code -> !code.equals(mfaRequest.getCode())).toList()));
            if (loginResponse.getStatus() == LoginResponse.Status.SUCCESS && loginResponse.getAccessToken() == null) {
                loginResponse.setAccessToken(loginStaffMember(staffMember.getEmail(), staffMember.getPvv()))
                        .setMfaType(staffMember.getMfaType());
            }
            return loginResponse;
        } catch (LockLoginException e) {
            throw lockUserLogin(staffMember, e.getInitialException());
        }
    }

    @Override
    public boolean checkTotpCode(AuthUser authUser, String totpCode) {
        StaffMember staffMember = permissionsService.getAuthStaffMember(authUser);
        return mfaService.checkTotpCode(staffMember.getMfaSecretCode(), totpCode);
    }

    private void saveLastLoginIfNeed(LoginResponse loginResponse, StaffMember staffMember) {
        if (loginResponse != null && loginResponse.getAccessToken() != null) {
            staffMemberService.save(staffMember.setLastLogin(Tool.currentDateTime()));
        }
    }

    @Override
    public boolean resendOtpCode(String username) {
        StaffMember staffMember = staffMemberService.findActiveStaffMember(username);
        mfaService.resendOtpCode(staffMember.getEmail(), staffMember.getMsisdn(), staffProperties.getMfa());
        return true;
    }

    @Override
    @Transactional
    public boolean forgotPassword(String email, String url, boolean sendEmail, String ip) {
        StaffMember staffMember = staffMemberService.findStaffMemberOrElse(email, () -> {
            log.warn("Forgot password attempt for unknown email: {}, IP: {}", email, ip);
            throw new UnexistingUserException("email: " + email);
        });
        String forgotPasswordKey = mfaService.createForgotPasswordKey(staffMember.getEmail());
        return notificationService.sendEmailByTemplate(
                sendEmail, staffProperties.getForgotPasswordEmailTemplate(),
                staffMemberService.getStaffMemberLanguage(staffMember).getId(),
                staffProperties.getForgotPasswordEmailFrom(),
                List.of(staffMember.getEmail()),
                Map.of("$firstname", staffMember.getFirstName(),
                        "$surname", staffMember.getLastName(),
                        "$dateTime", Tool.getZimDateTimeString(),
                        "$url", (url != null ? url : staffProperties.getResetPasswordUrl()),
                        "$key", forgotPasswordKey));
    }

    @Override
    @Transactional
    public StaffMember resetStaffMemberPassword(String key, String newPassword) {
        checkPinIsValid(newPassword);
        String login = mfaService.lookupLoginByForgotPasswordKey(key);
        StaffMember staffMember = staffMemberService.findStaffMember(login);
        StaffMember savedStaffMember = staffMemberService.save(staffMember
                .setPvv(securityPvvService.acquirePvv(staffMember.getPinKey(), newPassword)));
        updateKeycloakUser(savedStaffMember, savedStaffMember.getPvv());
        return savedStaffMember;
    }

    @Override
    @Transactional
    public StaffMember updateStaffMemberPassword(AuthUser authUser, String oldPassword, String newPassword) {
        checkPinIsValid(newPassword);
        StaffMember staffMember = permissionsService.getAuthStaffMember(authUser);
        String pvv = securityPvvService.acquirePvv(staffMember.getPinKey(), oldPassword);
        if (pvv.equals(staffMember.getPvv())) {
            StaffMember savedStaffMember = staffMemberService.save(staffMember
                    .setPvv(securityPvvService.acquirePvv(staffMember.getPinKey(), newPassword)));
            updateKeycloakUser(savedStaffMember, savedStaffMember.getPvv());
            return savedStaffMember;
        } else {
            throw new ICEcashException("Old password is incorrect", ErrorCodes.EC1032);
        }
    }

    @Override
    @Transactional
    public StaffMember updateStaffMemberLoginStatus(String email, LoginStatus loginStatus) {
        StaffMember staffMember = staffMemberService.findStaffMember(email);
        if (loginStatus == LoginStatus.ACTIVE && staffMember.getLoginStatus() != LoginStatus.ACTIVE) {
            mfaService.cleanupLoginData(staffMember.getEmail());
        }
        return staffMemberService.save(staffMember.setLoginStatus(loginStatus));
    }

    @Override
    public String getMfaQrCode(StaffMember staffMember) {
        String channelLanguageKey = channelRepository.findByCode("ADM").map(Channel::getLanguageKey).orElse(null);
        if (channelLanguageKey == null) {
            throw new ICEcashException("language key for ADM channel is not available", ErrorCodes.EC1043);
        }
        Language language = staffMemberService.getStaffMemberLanguage(staffMember);
        Dictionary dictionary = dictionaryRepository.findByLanguageIdAndLookupKey(language.getId(), channelLanguageKey)
                .orElseThrow(() -> new ICEcashException(String.format("dictionary for lookupKey: %s and language: %s is not available",
                        channelLanguageKey, language.getLanguageKey()), ErrorCodes.EC1044));
        return mfaService.getQrCode(dictionary.getValue(), staffMember.getEmail(), staffMember.getMfaSecretCode(), staffProperties.getMfa());
    }

    private AccessTokenResponse loginStaffMember(String email, String password) {
        try {
            return backofficeKeycloakService.loginStaffMember(email, password);
        } catch (NotAuthorizedException e) {
            return null;
        }
    }

    private NotAuthorizedException lockUserLogin(StaffMember staffMember, NotAuthorizedException exception) {
        log.info("StaffMember locked: {}", staffMember.getEmail());
        staffMemberService.save(staffMember.setLoginStatus(LoginStatus.LOCKED));
        return exception;
    }

    private void updateKeycloakUser(StaffMember staffMember, String password) {
        backofficeKeycloakService.updateUser(staffMember.getKeycloakId(), password, staffMember.getFirstName(),
                staffMember.getLastName(), staffMember.getEmail());
    }
}

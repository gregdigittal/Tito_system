package cash.ice.api.service.impl;

import cash.ice.api.config.property.StaffProperties;
import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.ConfigInput;
import cash.ice.api.entity.backoffice.StaffMember;
import cash.ice.api.errors.UnexistingUserException;
import cash.ice.api.repository.backoffice.StaffMemberRepository;
import cash.ice.api.service.KeycloakService;
import cash.ice.api.service.MfaService;
import cash.ice.api.service.NotificationService;
import cash.ice.api.service.PermissionsService;
import cash.ice.common.constant.IceCashProfile;
import cash.ice.sqldb.repository.LanguageRepository;
import cash.ice.sqldb.repository.SecurityGroupRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile(IceCashProfile.NOT_PROD)
@Slf4j
public class StaffMemberServiceUatImpl extends StaffMemberServiceImpl {
    public StaffMemberServiceUatImpl(KeycloakService backofficeKeycloakService, MfaService mfaService, PermissionsService permissionsService, NotificationService notificationService, StaffMemberRepository staffMemberRepository, LanguageRepository languageRepository, SecurityGroupRepository securityGroupRepository, StaffProperties staffProperties) {
        super(staffMemberRepository, backofficeKeycloakService, mfaService, permissionsService, notificationService, languageRepository, securityGroupRepository, staffProperties);
    }

    @Override
    public StaffMember getAuthStaffMember(AuthUser authUser, ConfigInput config) {
        if (config != null && config.getAuth() != null) {
            return staffMemberRepository.findById(config.getAuth()).orElseThrow(() ->
                    new UnexistingUserException(String.valueOf(config.getAuth())));
        } else {
            return super.getAuthStaffMember(authUser, config);
        }
    }

    @Override
    protected void sendUpdatedStaffMemberEmail(StaffMember staffMember, StaffMember updater, List<String> changeDescriptions, ConfigInput config, boolean sendEmail) {
        if (config == null || !config.isNoEmails()) {
            super.sendUpdatedStaffMemberEmail(staffMember, updater, changeDescriptions, config, sendEmail);
        } else {
            log.debug("  skip sending update email, changes: {}", changeDescriptions);
        }
    }
}

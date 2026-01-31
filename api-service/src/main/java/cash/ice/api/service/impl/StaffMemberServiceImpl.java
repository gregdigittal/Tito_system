package cash.ice.api.service.impl;

import cash.ice.api.config.property.StaffProperties;
import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.ConfigInput;
import cash.ice.api.dto.SortInput;
import cash.ice.api.entity.backoffice.StaffMember;
import cash.ice.api.errors.RegistrationException;
import cash.ice.api.errors.UnexistingUserException;
import cash.ice.api.repository.backoffice.StaffMemberRepository;
import cash.ice.api.service.*;
import cash.ice.api.util.CsvUtil;
import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.LanguageRepository;
import cash.ice.sqldb.repository.SecurityGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Profile(IceCashProfile.PROD)
@Slf4j
@RequiredArgsConstructor
public class StaffMemberServiceImpl implements StaffMemberService {
    private static final String[] CSV_HEADER = {"ID", "Entity ID", "Name", "Email", "Security Group", "Status", "Last Login", "MFA"};

    protected final StaffMemberRepository staffMemberRepository;
    private final KeycloakService backofficeKeycloakService;
    private final MfaService mfaService;
    private final PermissionsService permissionsService;
    private final NotificationService notificationService;
    private final LanguageRepository languageRepository;
    private final SecurityGroupRepository securityGroupRepository;
    private final StaffProperties staffProperties;

    @Override
    public StaffMember getAuthStaffMember(AuthUser authUser, ConfigInput config) {
        return permissionsService.getAuthStaffMember(authUser);
    }

    @Override
    public StaffMember getStaffMemberById(Integer id) {
        return staffMemberRepository.findById(id).orElseThrow(() -> new UnexistingUserException("id: " + id));
    }

    @Override
    public Iterable<StaffMember> searchStaffMembers(String searchText, LoginStatus status, int page, int size, SortInput sort) {
        PageRequest pageable = PageRequest.of(page, size, SortInput.toSort(sort));
        if (StringUtils.isBlank(searchText)) {
            if (status != null) {
                return staffMemberRepository.findByLoginStatus(status, pageable);
            } else {
                return staffMemberRepository.findAll(pageable);
            }
        } else if (searchText.matches("^[0-9]+$")) {
            return staffMemberRepository.findByIdOrNamesOrEmail((status != null ? status.name() : null), searchText, pageable);
        } else {
            String[] words = searchText.split("\\s+");
            return staffMemberRepository.findByNamesOrEmail(
                    (status != null ? status.name() : null),
                    words.length > 0 ? words[0] : "",
                    words.length > 1 ? words[1] : "",
                    pageable);
        }
    }

    @Override
    public String getUsersCsv(String searchText, LoginStatus status, boolean header, Character delimiter, String rowDelimiter) {
        Iterable<StaffMember> staffMembers = searchStaffMembers(searchText, status, 0, staffProperties.getExportCsv().getMaxRecords() + 1, null);
        if (Tool.size(staffMembers) > staffProperties.getExportCsv().getMaxRecords()) {
            throw new ICEcashException("Max users amount limit exceeded", ErrorCodes.EC1050);
        }
        Map<Integer, String> securityGroupMap = securityGroupRepository.findAll().stream()
                .collect(Collectors.toMap(SecurityGroup::getId, SecurityGroup::getName));
        CSVFormat csvFormat = CsvUtil.createCsvFormat(header, CSV_HEADER, delimiter, rowDelimiter);
        try {
            return CsvUtil.listToCsv(staffMembers, csvFormat, (staffMember, index) -> {
                List<String> list = new ArrayList<>();
                list.add(String.valueOf(index));
                list.add(String.valueOf(staffMember.getId()));
                list.add(staffMember.getFirstName() + (staffMember.getLastName() != null ? " " + staffMember.getLastName() : ""));
                list.add(staffMember.getEmail());
                list.add(staffMember.getSecurityGroupId() != null ? securityGroupMap.get(staffMember.getSecurityGroupId()) : null);
                list.add(staffMember.getLoginStatus() != null ? staffMember.getLoginStatus().toString() : null);
                list.add(staffMember.getLastLogin() != null ? Tool.getZimDateTimeString(staffMember.getLastLogin()) : null);
                list.add(staffMember.getMfaType() != null ? staffMember.getMfaType().toString() : null);
                return list;
            });
        } catch (IOException e) {
            throw new ICEcashException(e.getMessage(), ErrorCodes.EC1051);
        }
    }

    @Override
    @Transactional
    public StaffMember createNewStaffMember(StaffMember staffMember, String url, boolean sendEmail) {
        if (isStaffMemberExist(staffMember.getEmail())) {
            throw new RegistrationException("such email already registered", ErrorCodes.EC1042);
        }
        StaffMember savedStaffMember = staffMemberRepository.save(staffMember
                .setLoginStatus(LoginStatus.ACTIVE)
                .setPinKey(generateStaffMemberPinKey())
                .setLocale(staffMember.getLocale() != null ? staffMember.getLocale() : Locale.ENGLISH)
                .setMfaType(MfaType.OTP)
                .setMfaSecretCode(mfaService.generateSecretCode())
                .setMfaBackupCodes(mfaService.generateBackupCodes(staffProperties.getMfa()))
                .setCreatedDate(Tool.currentDateTime()));
        String newUserKey = mfaService.createForgotPasswordKey(staffMember.getEmail());
        notificationService.sendEmailByTemplate(
                sendEmail, staffProperties.getCreateUserEmailTemplate(),
                getStaffMemberLanguage(staffMember).getId(),
                staffProperties.getForgotPasswordEmailFrom(),
                List.of(staffMember.getEmail()),
                Map.of("$firstname", staffMember.getFirstName(),
                        "$surname", staffMember.getLastName(),
                        "$dateTime", Tool.getZimDateTimeString(),
                        "$url", (url != null ? url : staffProperties.getActivateUserUrl()),
                        "$key", newUserKey));
        return savedStaffMember;
    }

    @Override
    public boolean isExistsId(Integer idTypeId, String idNumber) {
        return staffMemberRepository.existsStaffMemberByIdNumberTypeAndIdNumber(idTypeId, idNumber);
    }

    @Override
    public boolean isExistsEmail(String email) {
        return staffMemberRepository.existsStaffMemberByEmail(email);
    }

    @Override
    @Transactional
    public StaffMember updateStaffMember(StaffMember staffMember, StaffMember staffMemberDetails, @Nullable StaffMember updater, @Nullable ConfigInput config, boolean sendEmail) {
        boolean emailChanged = !Objects.equals(staffMemberDetails.getEmail(), staffMember.getEmail());
        List<String> changeDescriptions = getChangeDescriptions(staffMemberDetails, staffMember);
        if (!changeDescriptions.isEmpty()) {
            if (staffMemberDetails.getLoginStatus() == LoginStatus.ACTIVE && staffMember.getLoginStatus() != LoginStatus.ACTIVE) {
                mfaService.cleanupLoginData(staffMemberDetails.getEmail());
            }
            StaffMember savedStaffMember = staffMemberRepository.save(staffMember.updateData(staffMemberDetails));
            if (emailChanged) {
                backofficeKeycloakService.updateStaffMemberUsername(staffMember.getKeycloakId(), staffMemberDetails.getEmail());
            }
            sendUpdatedStaffMemberEmail(staffMember, updater, changeDescriptions, config, sendEmail);
            return savedStaffMember;
        } else {
            log.debug("  No changes, skip updating");
            return staffMember;
        }
    }

    @Override
    public StaffMember updateMsisdn(StaffMember staffMember, String msisdn) {
        return staffMemberRepository.save(staffMember.setMsisdn(msisdn));
    }

    @Override
    public StaffMember updateMfaType(StaffMember staffMember, MfaType mfaType) {
        return staffMemberRepository.save(staffMember.setMfaType(mfaType));
    }

    protected void sendUpdatedStaffMemberEmail(StaffMember staffMember, StaffMember updater, List<String> changeDescriptions, ConfigInput config, boolean sendEmail) {
        if (staffProperties.isEmailAfterUpdate()) {
            notificationService.sendEmailByTemplate(
                    sendEmail, staffProperties.getUpdateUserEmailTemplate(),
                    getStaffMemberLanguage(staffMember).getId(),
                    staffProperties.getForgotPasswordEmailFrom(),
                    List.of(staffMember.getEmail()),
                    Map.of("$firstname", staffMember.getFirstName(),
                            "$surname", staffMember.getLastName(),
                            "$dateTime", Tool.getZimDateTimeString(),
                            "$userName", (updater != null ? updater.getFirstName() + " " + updater.getLastName() : ""),
                            "$updateDescription", layoutChangeDescription(changeDescriptions)));
        }
    }

    private String layoutChangeDescription(List<String> changeDescriptions) {
        AtomicInteger index = new AtomicInteger();
        return changeDescriptions.stream().map(change -> staffProperties.getUpdateUserEmailChangeDescriptionTemplate()
                        .replace("$change", change)
                        .replace("$index", String.valueOf(index.incrementAndGet())))
                .collect(Collectors.joining());
    }

    private List<String> getChangeDescriptions(StaffMember staffMemberDetails, StaffMember staffMember) {
        List<String> changes = new ArrayList<>();
        checkChange(staffMemberDetails, staffMember, StaffMember::getEmail, "Updated Email", changes);
        checkChange(staffMemberDetails, staffMember, StaffMember::getFirstName, "Updated First Name", changes);
        checkChange(staffMemberDetails, staffMember, StaffMember::getLastName, "Updated Surname", changes);
        checkChange(staffMemberDetails, staffMember, StaffMember::getIdNumber, "Updated ID number", changes);
        checkChange(staffMemberDetails, staffMember, StaffMember::getIdNumberType, "Updated ID number type", changes);
        checkChange(staffMemberDetails, staffMember, StaffMember::getMsisdn, "Updated Phone", changes);
        checkChange(staffMemberDetails, staffMember, StaffMember::getDepartment, "Updated Department", changes);
        checkChange(staffMemberDetails, staffMember, StaffMember::getMfaType, "Updated MFA type", changes);
        checkChange(staffMemberDetails, staffMember, StaffMember::getLocale, "Updated Locale", changes);
        checkChange(staffMemberDetails, staffMember, StaffMember::getSecurityGroupId, "Updated Permissions", changes);
        checkChange(staffMemberDetails, staffMember, StaffMember::getLoginStatus, "Updated status", changes);
        return changes;
    }

    private void checkChange(StaffMember staffMemberDetails, StaffMember staffMember, Function<StaffMember, Object> getter, String changeMessage, List<String> changes) {
        if (!Objects.equals(getter.apply(staffMemberDetails), getter.apply(staffMember))) {
            changes.add(changeMessage);
        }
    }

    @Override
    public StaffMember generateNewBackupCodes(Integer id) {
        return staffMemberRepository.save(getStaffMemberById(id)
                .setMfaBackupCodes(mfaService.generateBackupCodes(staffProperties.getMfa())));
    }

    @Override
    public StaffMember generateNewBackupCodes(AuthUser authUser) {
        StaffMember staffMember = permissionsService.getAuthStaffMember(authUser);
        return staffMemberRepository.save(staffMember
                .setMfaBackupCodes(mfaService.generateBackupCodes(staffProperties.getMfa())));
    }

    @Override
    @Transactional
    public StaffMember deleteStaffMember(Integer id) {
        return deleteStaffMember(getStaffMemberById(id));
    }

    @Override
    @Transactional
    public StaffMember deleteStaffMember(AuthUser authUser) {
        return deleteStaffMember(permissionsService.getAuthStaffMember(authUser));
    }

    private StaffMember deleteStaffMember(StaffMember staffMember) {
        staffMemberRepository.delete(staffMember);
        backofficeKeycloakService.removeUser(staffMember.getKeycloakId());
        return staffMember;
    }

    @Override
    public StaffMember save(StaffMember staffMember) {
        return staffMemberRepository.save(staffMember);
    }

    @Override
    public StaffMember findActiveStaffMember(String email) {
        StaffMember staffMember = staffMemberRepository.findStaffMemberByEmail(email)
                .orElseThrow(() -> new UnexistingUserException(email));
        if (staffMember.getLoginStatus() != LoginStatus.ACTIVE) {
            throw new ICEcashException(String.format("Account is %s for login", staffMember.getLoginStatus()), ErrorCodes.EC1035);
        }
        return staffMember;
    }

    @Override
    public StaffMember findStaffMember(String email) {
        return staffMemberRepository.findStaffMemberByEmail(email)
                .orElseThrow(() -> new UnexistingUserException("email: " + email));
    }

    @Override
    public StaffMember findStaffMemberOrElse(String email, Supplier<StaffMember> staffMemberSupplier) {
        return staffMemberRepository.findStaffMemberByEmail(email).orElseGet(staffMemberSupplier);
    }

    @Override
    public boolean isStaffMemberExist(String email) {
        return staffMemberRepository.existsStaffMemberByEmail(email);
    }

    @Override
    public Language getStaffMemberLanguage(StaffMember staffMember) {
        Locale locale = staffMember.getLocale() != null ? staffMember.getLocale() : Locale.ENGLISH;
        return languageRepository.findByLanguageKey(locale.getLanguage())
                .orElseThrow(() -> new ICEcashException("Unknown language is assigned to user: " +
                        locale.getLanguage(), ErrorCodes.EC1039));
    }

    @Override
    public String generateStaffMemberPinKey() {
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            String id = Tool.generateDigits(EntityClass.INTERNAL_NUM_LENGTH, true);
            if (!staffMemberRepository.existsAccountByPinKey(id)) {
                return id;
            }
        }
        throw new RegistrationException(ErrorCodes.EC1009, "Cannot generate pin key");
    }
}

package cash.ice.api.service.impl;

import cash.ice.api.config.property.MfaProperties;
import cash.ice.api.config.property.StaffProperties;
import cash.ice.api.entity.backoffice.StaffMember;
import cash.ice.api.repository.backoffice.StaffMemberRepository;
import cash.ice.api.service.KeycloakService;
import cash.ice.api.service.MfaService;
import cash.ice.api.service.NotificationService;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.sqldb.entity.Language;
import cash.ice.sqldb.entity.LoginStatus;
import cash.ice.sqldb.entity.MfaType;
import cash.ice.sqldb.entity.SecurityGroup;
import cash.ice.sqldb.repository.LanguageRepository;
import cash.ice.sqldb.repository.SecurityGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffMemberServiceImplTest {
    private static final String ID_NUMBER = "12345678";
    private static final int ID_NUMBER_TYPE = 3;
    private static final String DEPARTMENT = "ICEcash";
    private static final int SECURITY_GROUP_ID = 2;
    private static final String NEW_USER_KEY = "key";
    private static final String EMAIL_TEMPLATE = "template";
    private static final int LANGUAGE_ID = 1;
    private static final String EMAIL = "user@ice.cash";
    private static final String SECRET_CODE = "secretCode";
    private static final String MSISDN = "123456789012";
    private static final String FIRST_NAME = "someFirstName";
    private static final String LAST_NAME = "someLastName";
    private static final String KEYCLOAK_ID = "keycloakId";

    @Mock
    private MfaService mfaService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private KeycloakService keycloakService;
    @Mock
    private StaffMemberRepository staffMemberRepository;
    @Mock
    private LanguageRepository languageRepository;
    @Mock
    private SecurityGroupRepository securityGroupRepository;
    @Mock
    private StaffProperties staffProperties;
    @InjectMocks
    private StaffMemberServiceImpl service;

    @Test
    void testRegisterStaffMember() {
        StaffMember newStaffMember = new StaffMember().setEmail(EMAIL).setFirstName(FIRST_NAME).setLastName(LAST_NAME)
                .setIdNumber(ID_NUMBER).setIdNumberType(ID_NUMBER_TYPE).setMsisdn(MSISDN).setDepartment(DEPARTMENT).setSecurityGroupId(SECURITY_GROUP_ID);
        MfaProperties mfaProperties = new MfaProperties();
        List<String> mfaBackupCodes = List.of("code1", "code2");

        when(staffMemberRepository.existsStaffMemberByEmail(EMAIL)).thenReturn(false);
        when(mfaService.generateSecretCode()).thenReturn(SECRET_CODE);
        when(staffProperties.getMfa()).thenReturn(mfaProperties);
        when(mfaService.generateBackupCodes(mfaProperties)).thenReturn(mfaBackupCodes);
        when(staffMemberRepository.save(any())).thenAnswer(invocation -> invocation.getArguments()[0]);
        when(mfaService.createForgotPasswordKey(EMAIL)).thenReturn(NEW_USER_KEY);
        when(staffProperties.getCreateUserEmailTemplate()).thenReturn(EMAIL_TEMPLATE);
        when(languageRepository.findByLanguageKey(Locale.ENGLISH.getLanguage())).thenReturn(Optional.of(new Language().setId(LANGUAGE_ID)));
        when(notificationService.sendEmailByTemplate(eq(true), eq(EMAIL_TEMPLATE), eq(LANGUAGE_ID), any(), eq(List.of(EMAIL)), any())).thenReturn(true);

        StaffMember actualStaffMember = service.createNewStaffMember(newStaffMember, "urlForEmail", true);
        assertThat(actualStaffMember.getLoginStatus()).isEqualTo(LoginStatus.ACTIVE);
        assertThat(actualStaffMember.getMfaType()).isEqualTo(MfaType.OTP);
        assertThat(actualStaffMember.getMfaSecretCode()).isEqualTo(SECRET_CODE);
        assertThat(actualStaffMember.getMfaBackupCodes()).isEqualTo(mfaBackupCodes);
        assertThat(actualStaffMember.getLocale()).isEqualTo(Locale.ENGLISH);
        assertThat(actualStaffMember.getCreatedDate()).isNotNull();
    }

    @Test
    void testUpdateStaffMember() {
        StaffMember staffMember = new StaffMember().setKeycloakId(KEYCLOAK_ID);
        StaffMember staffMemberUpdate = new StaffMember().setEmail(EMAIL).setFirstName(FIRST_NAME).setLastName(LAST_NAME)
                .setIdNumber(ID_NUMBER).setIdNumberType(ID_NUMBER_TYPE).setMsisdn(MSISDN).setDepartment(DEPARTMENT)
                .setSecurityGroupId(SECURITY_GROUP_ID).setMfaType(MfaType.TOTP).setLoginStatus(LoginStatus.ACTIVE);
        String updateUserEmailTemplate = "updateUserEmailTemplate";

        when(staffMemberRepository.save(any())).thenAnswer(invocation -> invocation.getArguments()[0]);
        when(staffProperties.isEmailAfterUpdate()).thenReturn(true);
        when(staffProperties.getUpdateUserEmailTemplate()).thenReturn(updateUserEmailTemplate);
        when(staffProperties.getUpdateUserEmailChangeDescriptionTemplate()).thenReturn("template");
        when(languageRepository.findByLanguageKey(Locale.ENGLISH.getLanguage())).thenReturn(Optional.of(new Language().setId(LANGUAGE_ID)));
        when(notificationService.sendEmailByTemplate(eq(true), eq(updateUserEmailTemplate), eq(LANGUAGE_ID), any(), eq(List.of(EMAIL)), any())).thenReturn(true);

        StaffMember actualStaffMember = service.updateStaffMember(staffMember, staffMemberUpdate, null, null, true);
        assertThat(actualStaffMember.getEmail()).isEqualTo(EMAIL);
        assertThat(actualStaffMember.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(actualStaffMember.getLastName()).isEqualTo(LAST_NAME);
        assertThat(actualStaffMember.getIdNumber()).isEqualTo(ID_NUMBER);
        assertThat(actualStaffMember.getIdNumberType()).isEqualTo(ID_NUMBER_TYPE);
        assertThat(actualStaffMember.getMsisdn()).isEqualTo(MSISDN);
        assertThat(actualStaffMember.getDepartment()).isEqualTo(DEPARTMENT);
        assertThat(actualStaffMember.getSecurityGroupId()).isEqualTo(SECURITY_GROUP_ID);
        assertThat(actualStaffMember.getMfaType()).isEqualTo(MfaType.TOTP);
        assertThat(actualStaffMember.getLoginStatus()).isEqualTo(LoginStatus.ACTIVE);
        verify(mfaService).cleanupLoginData(EMAIL);
        verify(keycloakService).updateStaffMemberUsername(KEYCLOAK_ID, EMAIL);
    }

    @Test
    void testSearchAllStaffMembers() {
        String searchText = null;
        LoginStatus loginStatus = null;
        Page<StaffMember> staffMembers = new PageImpl<>(List.of(new StaffMember().setEmail(EMAIL)));
        when(staffMemberRepository.findAll(any(PageRequest.class))).thenReturn(staffMembers);

        Iterable<StaffMember> actualStaffMembers = service.searchStaffMembers(searchText, loginStatus, 0, 30, null);
        assertThat(actualStaffMembers).isEqualTo(staffMembers);
    }

    @Test
    void testSearchActiveStaffMembers() {
        String searchText = null;
        LoginStatus loginStatus = LoginStatus.ACTIVE;
        Page<StaffMember> staffMembers = new PageImpl<>(List.of(new StaffMember().setEmail(EMAIL)));
        when(staffMemberRepository.findByLoginStatus(eq(loginStatus), any())).thenReturn(staffMembers);

        Iterable<StaffMember> actualStaffMembers = service.searchStaffMembers(searchText, loginStatus, 0, 30, null);
        assertThat(actualStaffMembers).isEqualTo(staffMembers);
    }

    @Test
    void testSearchStaffMembersById() {
        String searchText = "111";
        LoginStatus loginStatus = LoginStatus.ACTIVE;
        List<StaffMember> staffMembers = List.of(new StaffMember().setEmail(EMAIL));
        when(staffMemberRepository.findByIdOrNamesOrEmail(eq(loginStatus.name()), eq(searchText), any())).thenReturn(staffMembers);

        Iterable<StaffMember> actualStaffMembers = service.searchStaffMembers(searchText, loginStatus, 0, 30, null);
        assertThat(actualStaffMembers).isEqualTo(staffMembers);
    }

    @Test
    void testSearchStaffMembersByNames() {
        String searchText = "John Doe";
        LoginStatus loginStatus = LoginStatus.ACTIVE;
        List<StaffMember> staffMembers = List.of(new StaffMember().setEmail(EMAIL));
        when(staffMemberRepository.findByNamesOrEmail(eq(loginStatus.name()), eq("John"), eq("Doe"), any())).thenReturn(staffMembers);

        Iterable<StaffMember> actualStaffMembers = service.searchStaffMembers(searchText, loginStatus, 0, 30, null);
        assertThat(actualStaffMembers).isEqualTo(staffMembers);
    }

    @Test
    void testGetUsersCsv() {
        String searchText = "John Doe";
        List<StaffMember> staffMembers = List.of(
                new StaffMember().setId(1).setFirstName("John").setLastName("Doe").setEmail("jd@ice.cash")
                        .setSecurityGroupId(1).setLoginStatus(LoginStatus.ACTIVE).setMfaType(MfaType.OTP),
                new StaffMember().setId(1).setFirstName("Jane").setLastName("Dow").setEmail("jane@ice.cash")
                        .setSecurityGroupId(2).setLoginStatus(LoginStatus.INACTIVE).setMfaType(MfaType.TOTP)
                        .setLastLogin(LocalDateTime.of(2023, 1, 24, 10, 20, 30)));
        when(staffMemberRepository.findByNamesOrEmail(eq(null), eq("John"), eq("Doe"), any())).thenReturn(staffMembers);
        when(staffProperties.getExportCsv()).thenReturn(new StaffProperties.ExportCsv().setMaxRecords(30000));
        when(securityGroupRepository.findAll()).thenReturn(List.of(new SecurityGroup().setId(1).setName("Admin")));

        String csv = service.getUsersCsv(searchText, null, true, null, null);
        assertThat(csv).isEqualTo("""
                ID,Entity ID,Name,Email,Security Group,Status,Last Login,MFA\r
                1,1,John Doe,jd@ice.cash,Admin,ACTIVE,,OTP\r
                2,1,Jane Dow,jane@ice.cash,,INACTIVE,2023-01-24 10:20:30,TOTP\r
                """);
    }

    @Test
    void testGetUsersCsvLimitExceed() {
        String searchText = "John Doe";
        int maxRecords = 2;
        List<StaffMember> staffMembers = List.of(new StaffMember(), new StaffMember(), new StaffMember());

        when(staffMemberRepository.findByNamesOrEmail(eq(null), eq("John"), eq("Doe"), any())).thenReturn(staffMembers);
        when(staffProperties.getExportCsv()).thenReturn(new StaffProperties.ExportCsv().setMaxRecords(maxRecords));
        ICEcashException exception = assertThrows(ICEcashException.class, () -> service.getUsersCsv(searchText, null, true, null, null));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.EC1050);
    }
}
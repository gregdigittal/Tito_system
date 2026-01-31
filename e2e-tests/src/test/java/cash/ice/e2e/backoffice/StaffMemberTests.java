package cash.ice.e2e.backoffice;

import cash.ice.*;
import cash.ice.common.constant.EntityMetaKey;
import cash.ice.sqldb.entity.AddressType;
import cash.ice.sqldb.entity.EntityType;
import cash.ice.sqldb.entity.KYC;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static cash.ice.BackofficeRequests.*;
import static cash.ice.GraphQlRequests.*;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StaffMemberTests {
    public static final String FIRST_NAME2 = "Test commuter";
    public static final String LAST_NAME2 = "Test regular commuter";
    public static final String ID_NUMBER2 = "1213141520";
    public static final String NUIT = "12341238";
    public static final String NUIT2 = "12341239";
    public static final String EMAIL2 = "test.backoffice.user2@ice.cash";
    public static final String GENDER = "MALE";
    public static final String ACTIVE = "ACTIVE";
    public static final String CONTACT1 = "contact1";
    public static final String CONTACT2 = "contact2";
    public static final String AUTH_TYPE = "SINGLE";
    public static final boolean CORPORATE_FEE = true;
    public static final String TIER = "Tier2";
    public static final String KYC1 = "PARTIAL";
    public static final String COMPANY = "testCompany1";
    public static final String CITY = "Harare";
    public static final String ZIP = "12345";
    public static final String ADDR_1 = "addr1";
    public static final String ADDR_2 = "addr2";
    public static final String NOTES = "notes1";
    private static final String EMAIL = "test.backoffice.user@ice.cash";
    private static final String MOBILE = "000000000000";
    private static final String PASSWORD = "1234";
    private static final String FIRST_NAME = "Test user";
    private static final String LAST_NAME = "Some test user";
    private static final String ID_NUMBER = "1213141515";
    private static final String NATIONAL_ID_TYPE = "Mozambican National ID";
    private static final String PASSPORT_ID_TYPE = "Mozambican Passport";
    private static final String NUEL_ID_TYPE = "Mozambican NUEL";
    private static final String SECURITY_GROUP_NAME = "Admin";
    private static final String SECURITY_GROUP_NAME2 = "Moz default";
    public static final String NUEL = "testNuel";
    private final RestHelper rest = new RestHelper();
    private final GraphQLHelper graphQL = new GraphQLHelper(rest);
    private final ApiUtil apiUtil = new ApiUtil(rest, graphQL);

    private Integer staffMemberId;
    private Integer zimCountryId;
    private Integer mozCountryId;
    private Integer nationalIdTypeId;
    private Integer passportIdTypeId;
    private Integer nuelIdTypeId;
    private Integer securityGroupId;
    private Integer securityGroupId2;
    private String token;

    @BeforeAll
    public void init() {
        var countries = graphQL.call(getCountries)
                .print("  countries");
        zimCountryId = countries.getInt("[isoCode = ZIM].id ");
        mozCountryId = countries.getInt("[isoCode = MOZ].id");

        var idTypes = graphQL.call(getIdTypes)
                .print("  id types");
        nationalIdTypeId = idTypes.getInt("[description = {NATIONAL_ID_TYPE}].id", NATIONAL_ID_TYPE);
        passportIdTypeId = idTypes.getInt("[description = {PASSPORT_ID_TYPE}].id", PASSPORT_ID_TYPE);
        nuelIdTypeId = idTypes.getInt("[description = {NUEL_ID_TYPE}].id", NUEL_ID_TYPE);
        assertThat(nationalIdTypeId).isNotNull();
        assertThat(passportIdTypeId).isNotNull();
        assertThat(nuelIdTypeId).isNotNull();

        var securityGroups = graphQL.call(getSecurityGroups)
                .print("  security groups");
        securityGroupId = securityGroups.getInt("[name = {SECURITY_GROUP_NAME}].id", SECURITY_GROUP_NAME);
        securityGroupId2 = securityGroups.getInt("[name = {SECURITY_GROUP_NAME2}].id", SECURITY_GROUP_NAME2);
        assertThat(securityGroupId).isNotNull();
        assertThat(securityGroupId2).isNotNull();

        var staffMember = createStaffMember(EMAIL, FIRST_NAME, LAST_NAME, nationalIdTypeId, ID_NUMBER, securityGroupId)
                .print("  create backoffice user");
        staffMemberId = staffMember.getInt("id");
        token = loginTokenBackofficeUser(EMAIL, PASSWORD, true);
    }

    private Wrapper createStaffMember(String email, String firstName, String lastName, Integer idTypeId, String idNumber, Integer securityGroupId) {
        var staffMember = graphQL.call(format(newStaffMember, email, firstName, lastName, idNumber, idTypeId, MOBILE, "ICEcash", securityGroupId))
                .print("  backoffice user");

        String regKey = rest.sendSimpleGetRequest("/user/backoffice/forgot/key", "login=" + EMAIL);
        System.out.println("  regKey: " + regKey);
        assertThat(regKey).isNotBlank();

        graphQL.call(format(activateNewStaffMember, regKey, PASSWORD))
                .print("  activate");
        return staffMember;
    }

    @Test
    public void testRegisterStaffMember() {
        var user = graphQL.call(getCurrentStaffMember, token)
                .print("  user");
        checkStaffMember(user);
    }

    private void checkStaffMember(Wrapper userWrapper) {
        assertThat(userWrapper.getInt("id")).isGreaterThan(0);
        assertThat(userWrapper.getStr("email")).isEqualTo(EMAIL);
        assertThat(userWrapper.getStr("firstName")).isEqualTo(FIRST_NAME);
        assertThat(userWrapper.getStr("lastName")).isEqualTo(LAST_NAME);
        assertThat(userWrapper.getStr("idNumber")).isEqualTo(ID_NUMBER);
        assertThat(userWrapper.getInt("idNumberType")).isEqualTo(nationalIdTypeId);
        assertThat(userWrapper.getStr("msisdn")).isEqualTo(MOBILE);
        assertThat(userWrapper.getStr("department")).isEqualTo("ICEcash");
        assertThat(userWrapper.getInt("securityGroupId")).isEqualTo(securityGroupId);
        assertThat(userWrapper.getStr("securityGroup.name")).isEqualTo(SECURITY_GROUP_NAME);
        assertThat(userWrapper.getStr("loginStatus")).isEqualTo("ACTIVE");
        assertThat(userWrapper.getStr("mfaType")).isEqualTo("OTP");
        assertThat(userWrapper.getStr("mfaSecretCode")).isNotBlank();
        assertThat(userWrapper.getStr("mfaQrCode")).isNotBlank();
        assertThat(userWrapper.getStr("mfaQrCode")).startsWith("data:image/png;base64");
        assertThat(userWrapper.getStrList("mfaBackupCodes").size()).isEqualTo(6);
        assertThat(userWrapper.getStr("locale")).isEqualTo("en");
        assertThat(userWrapper.getStr("createdDate")).isNotNull();
    }

    @Test
    public void testLoginInactive() {
        var updateLoginStatus = graphQL.call(format(updateStaffMemberLoginStatus, EMAIL, "INACTIVE"))
                .print("  update INACTIVE login status");
        assertThat(updateLoginStatus.getStr("loginStatus")).isEqualTo("INACTIVE");

        var exception = assertThrows(GraphQLError.class, () -> loginTokenBackofficeUser(EMAIL, PASSWORD, true))
                .print("  locked exception");
        assertThat(exception.getErrorMessage()).isEqualTo("Account is INACTIVE for login");
        assertThat(exception.getErrorCode()).isEqualTo("101-IC1225-0035");

        updateLoginStatus = graphQL.call(format(updateStaffMemberLoginStatus, EMAIL, "ACTIVE"))
                .print("  update ACTIVE login status");
        assertThat(updateLoginStatus.getStr("loginStatus")).isEqualTo("ACTIVE");

        String newToken = loginTokenBackofficeUser(EMAIL, PASSWORD, false);
        assertThat(newToken).isNotBlank();
    }

    @Test
    public void testLoginLockUnlock() {
        String wrongPassword = "1111";
        GraphQLError exception = assertThrows(GraphQLError.class, () -> loginTokenBackofficeUser(EMAIL, wrongPassword, false));
        checkUnauthorized(exception);
        exception = assertThrows(GraphQLError.class, () -> loginTokenBackofficeUser(EMAIL, wrongPassword, false));
        checkUnauthorized(exception);
        exception = assertThrows(GraphQLError.class, () -> loginTokenBackofficeUser(EMAIL, wrongPassword, false));
        checkUnauthorized(exception);

        exception = assertThrows(GraphQLError.class, () -> loginTokenBackofficeUser(EMAIL, PASSWORD, true));
        System.out.println("locked exception: " + exception);
        assertThat(exception.getErrorMessage()).isEqualTo("Account is LOCKED for login");
        assertThat(exception.getErrorCode()).isEqualTo("101-IC1225-0035");

        graphQL.call(format(updateStaffMemberLoginStatus, EMAIL, "ACTIVE"))
                .print("  update ACTIVE login status");

        String newToken = loginTokenBackofficeUser(EMAIL, PASSWORD, false);
        assertThat(newToken).isNotBlank();
    }

    private void checkUnauthorized(GraphQLError exception) {
        System.out.println("unauthorized exception: " + exception);
        assertThat(exception.getClassification()).isEqualTo("UNAUTHORIZED");
        assertThat(exception.getErrorMessage()).isEqualTo("HTTP 401 Unauthorized");
        assertThat(exception.getErrorCode()).isEqualTo("101-IC1146-0010");
    }

    @Test
    public void testBackupCodes() {
        var user = graphQL.call(getCurrentStaffMember, token)
                .print("  user");
        String backupCode = user.getStrList("mfaBackupCodes").getFirst();

        var enterBackup = graphQL.call(format(enterStaffMemberBackupCode, EMAIL, backupCode))
                .print("  enter backup code");
        String newToken = enterBackup.getStr("accessToken.token");
        assertThat(enterBackup.getStr("status")).isEqualTo("SUCCESS");
        assertThat(enterBackup.getStr("mfaType")).isEqualTo("OTP");
        assertThat(newToken).isNotBlank();

        user = graphQL.call(getCurrentStaffMember, token)
                .print("  user");
        List<String> backupCodes = user.getStrList("mfaBackupCodes");
        assertFalse(backupCodes.contains(backupCode));

        var exception = graphQL.callForError(format(enterStaffMemberBackupCode, EMAIL, backupCode));       // reuse code
        checkUnauthorized(exception);

        var generateBackupCodes = graphQL.call(generateStaffMemberBackupCodes, newToken)           // generate new backup codes
                .print("  generate backup codes");
        List<String> newBackupCodes = generateBackupCodes.getStrList("mfaBackupCodes");
        assertNotEquals(backupCodes, newBackupCodes);
        assertThat(newBackupCodes.size()).isEqualTo(6);

        enterBackup = graphQL.call(format(enterStaffMemberBackupCode, EMAIL, newBackupCodes.getFirst()))
                .print("  enter backup code");
        assertThat(enterBackup.getStr("status")).isEqualTo("SUCCESS");
        assertThat(enterBackup.getStr("accessToken.token")).isNotBlank();
    }

    @Test
    public void testUpdatePassword() {
        String newPassword = "4321";
        graphQL.call(format(updateStaffMemberPassword, PASSWORD, newPassword), token)
                .print("  update password");

        GraphQLError exception = assertThrows(GraphQLError.class, () -> loginTokenBackofficeUser(EMAIL, PASSWORD, false));
        checkUnauthorized(exception);
        String newToken = loginTokenBackofficeUser(EMAIL, newPassword, false);
        assertThat(newToken).isNotBlank();

        graphQL.call(format(updateStaffMemberPassword, newPassword, PASSWORD), newToken)       // return password
                .print("  return password");

        exception = assertThrows(GraphQLError.class, () -> loginTokenBackofficeUser(EMAIL, newPassword, false));
        checkUnauthorized(exception);
        newToken = loginTokenBackofficeUser(EMAIL, PASSWORD, false);
        assertThat(newToken).isNotBlank();
    }

    @Test
    public void testForgotPassword() {
        String newPassword = "4321";
        graphQL.call(format(staffMemberForgotPassword, EMAIL, "url"))
                .print("  forgot password");

        String forgotKey = rest.sendSimpleGetRequest("/user/backoffice/forgot/key", "login=" + EMAIL);
        System.out.println("  forgotKey: " + forgotKey);
        assertThat(forgotKey).isNotBlank();

        graphQL.call(format(staffMemberResetPassword, forgotKey, newPassword))
                .print("  reset password");

        String newToken = loginTokenBackofficeUser(EMAIL, newPassword, false);
        var userWrapper = graphQL.call(getCurrentStaffMember, newToken)
                .print("  user");
        assertThat(userWrapper).isNotNull();

        graphQL.call(format(staffMemberForgotPassword, EMAIL, "url"))        // return password
                .print("  forgot password");

        forgotKey = rest.sendSimpleGetRequest("/user/backoffice/forgot/key", "login=" + EMAIL);
        System.out.println("  forgotKey: " + forgotKey);
        assertThat(forgotKey).isNotBlank();

        graphQL.call(format(staffMemberResetPassword, forgotKey, PASSWORD))
                .print("  reset password");
    }

    @Test
    public void testRefreshInvalidateToken() {
        var login = loginBackofficeUser(EMAIL, PASSWORD, false);
        String loginToken = login.getStr("accessToken.token");
        String refreshToken = login.getStr("accessToken.refreshToken");

        graphQL.call(getCurrentStaffMember, loginToken)
                .print("  user");

        var refresh = graphQL.call(format(refreshStaffMemberToken, refreshToken))
                .print("  refresh token");
        loginToken = refresh.getStr("accessToken.token");
        String newRefreshToken = refresh.getStr("accessToken.refreshToken");
        assertNotEquals(refreshToken, newRefreshToken);

        graphQL.call(getCurrentStaffMember, loginToken)
                .print("  user");

        graphQL.call(format(invalidateStaffMemberToken, newRefreshToken))        // invalidate
                .print("  invalidate token");

        var exception = graphQL.callForError(format(refreshStaffMemberToken, newRefreshToken))
                .print("  refresh exception");
        assertThat(exception.getErrorMessage()).isEqualTo("Refresh token failed");
        assertThat(exception.getErrorCode()).isEqualTo("101-IC1116-0004");

        exception = graphQL.callForError(format(refreshStaffMemberToken, refreshToken))
                .print("  refresh exception");
        assertThat(exception.getErrorMessage()).isEqualTo("Refresh token failed");
    }

    @Test
    public void testUpdateStaffMemberMsisdn() {
        var updateMsisdn = graphQL.call(format(updateStaffMemberMsisdn, "111111111111"), token)
                .print("  update msisdn");
        assertThat(updateMsisdn.getStr("msisdn")).isEqualTo("111111111111");

        var user = graphQL.call(getCurrentStaffMember, token)
                .print("  user");
        assertThat(user.getStr("msisdn")).isEqualTo("111111111111");

        updateMsisdn = graphQL.call(format(updateStaffMemberMsisdn, MOBILE), token)
                .print("  return msisdn");
        assertThat(updateMsisdn.getStr("msisdn")).isEqualTo(MOBILE);

        user = graphQL.call(getCurrentStaffMember, token)
                .print("  user");
        assertThat(user.getStr("msisdn")).isEqualTo(MOBILE);
    }

    @Test
    public void testUpdateStaffMember() {
        graphQL.call(format(updateStaffMember, "test.backoffice.user.upd@ice.cash", "Test user upd", "Some test user upd",
                        "1514131212", passportIdTypeId, "111111111111", "NewDept", "TOTP", "en", "INACTIVE", securityGroupId2), token)
                .print("  update user");

        var user = graphQL.call(getCurrentStaffMember, token)
                .print("  user");
        assertThat(user.getStr("email")).isEqualTo("test.backoffice.user.upd@ice.cash");
        assertThat(user.getStr("firstName")).isEqualTo("Test user upd");
        assertThat(user.getStr("lastName")).isEqualTo("Some test user upd");
        assertThat(user.getStr("idNumber")).isEqualTo("1514131212");
        assertThat(user.getInt("idNumberType")).isEqualTo(passportIdTypeId);
        assertThat(user.getStr("msisdn")).isEqualTo("111111111111");
        assertThat(user.getStr("department")).isEqualTo("NewDept");
        assertThat(user.getInt("securityGroupId")).isEqualTo(securityGroupId2);
        assertThat(user.getStr("securityGroup.name")).isEqualTo(SECURITY_GROUP_NAME2);
        assertThat(user.getStr("loginStatus")).isEqualTo("INACTIVE");
        assertThat(user.getStr("mfaType")).isEqualTo("TOTP");
        assertThat(user.getStr("locale")).isEqualTo("en");

        graphQL.call(format(updateStaffMember, EMAIL, FIRST_NAME, LAST_NAME,
                        ID_NUMBER, nationalIdTypeId, MOBILE, "ICEcash", "OTP", "en", "ACTIVE", securityGroupId), token)
                .print("  update user");

        user = graphQL.call(getCurrentStaffMember, token)
                .print("  user");
        checkStaffMember(user);
    }

    @Test
    public void testUpdateStaffMemberTotpMfaType() {
        var user = graphQL.call(getCurrentStaffMember, token)
                .print("  user");
        String mfaSecretCode = user.getStr("mfaSecretCode");
        assertThat(mfaSecretCode).isNotBlank();

        var updateMfaType = graphQL.call(format(updateStaffMemberMfaType, "TOTP"), token)     // set totp mfa
                .print("  update mfa type");
        assertThat(updateMfaType.getStr("mfaType")).isEqualTo("TOTP");

        graphQL.call(format(loginStaffMember, EMAIL, PASSWORD))
                .print("  login");
        assertThat(updateMfaType.getStr("mfaType")).isEqualTo("TOTP");

        String totp = rest.sendSimpleGetRequest("/user/totp/code", "mfaSecretCode=" + mfaSecretCode);             // get totp
        System.out.println("  totp: " + totp);
        assertThat(totp.length()).isEqualTo(6);

        var checkTotp = graphQL.call(format(checkStaffMemberTotpCode, totp), token)                // check totp
                .print("  check totp");
        assertThat(checkTotp.getBool()).isTrue();

        var loginMfa = graphQL.call(format(loginMfaStaffMember, EMAIL, totp))
                .print("  mfa");
        assertThat(loginMfa.getStr("status")).isEqualTo("SUCCESS");
        assertThat(loginMfa.getStr("mfaType")).isEqualTo("TOTP");
        assertThat(loginMfa.getStr("accessToken.token")).isNotNull();

        graphQL.call(format(updateStaffMemberMfaType, "OTP"), token)               // return otp mfa
                .print("  update mfa type");
    }

    @Test
    public void testSearchStaffMembers() {
        var users = graphQL.call(format(searchStaffMembers, staffMemberId, null), token)
                .print("  search users");
        assertThat(users.getInt("[ 0 ].id")).isEqualTo(staffMemberId);

        users = graphQL.call(format(searchStaffMembers, staffMemberId, "ACTIVE"), token)
                .print("  search users 2");
        assertThat(users.getInt("[ 0 ].id")).isEqualTo(staffMemberId);

        users = graphQL.call(format(searchStaffMembers, staffMemberId, "INACTIVE"), token)
                .print("  search users 3");
        assertThat(users.getListSize("")).isEqualTo(0);

        users = graphQL.call(format(searchStaffMembers, EMAIL, null), token)
                .print("  search users 4");
        assertThat(users.getStr("[ 0 ].email")).isEqualTo(EMAIL);

        users = graphQL.call(format(searchStaffMembers, FIRST_NAME, null), token)
                .print("  search users 5");
        assertThat(users.getStr("[ 0 ].firstName")).isEqualTo(FIRST_NAME);

        users = graphQL.call(format(searchStaffMembers, LAST_NAME, null), token)
                .print("  search users 6");
        assertThat(users.getStr("[ 0 ].lastName")).isEqualTo(LAST_NAME);

        users = graphQL.call(format(searchStaffMembers, format("%s %s", FIRST_NAME, LAST_NAME), null), token)
                .print("  search users 7");
        assertThat(users.getStr("[ 0 ].firstName")).isEqualTo(FIRST_NAME);
        assertThat(users.getStr("[ 0 ].lastName")).isEqualTo(LAST_NAME);

        users = graphQL.call(format(searchStaffMembers, EMAIL2, null), token)
                .print("  search users 8");
        assertThat(users.getListSize("")).isEqualTo(0);
    }

    @Test
    public void testRegisterEntityOld() {
        var userIdExists = graphQL.call(format(existsUserId, nationalIdTypeId, ID_NUMBER2, true))
                .print("  user id exists");
        assertThat(userIdExists.getBool()).isEqualTo(false);

        var userEmailExists = graphQL.call(format(existsUserEmail, EMAIL2, true))
                .print("  user email exists");
        assertThat(userEmailExists.getBool()).isEqualTo(false);

        graphQL.call(format(existsUserMsisdn, MOBILE))
                .print("  user msisdn exists");

        var entity = graphQL.call(format(registerEntity, ACTIVE, ACTIVE, EntityType.PRIVATE, FIRST_NAME2, LAST_NAME2, nationalIdTypeId, ID_NUMBER2,
                        KYC1, CONTACT1, MOBILE, CONTACT2, MOBILE, EMAIL2, GENDER, AUTH_TYPE, CORPORATE_FEE, TIER, mozCountryId,
                        zimCountryId, CITY, ZIP, ADDR_1, ADDR_2, NOTES, COMPANY), token)
                .print("  register entity");
        int entityId = entity.getInt("id");
        try {
            checkEntity(entity);
            checkZimAccounts(entity);

            userIdExists = graphQL.call(format(existsUserId, nationalIdTypeId, ID_NUMBER2, true))
                    .print("  user id exists");
            assertThat(userIdExists.getBool()).isEqualTo(true);

            userEmailExists = graphQL.call(format(existsUserEmail, EMAIL2, true))
                    .print("  user email exists");
            assertThat(userEmailExists.getBool()).isEqualTo(true);

            String accountNumber = entity.getStr("accounts[0].accountNumber");
            String pin = rest.sendSimpleGetRequest("/users/pin/entity", "username=" + accountNumber);
            System.out.println("  PIN: " + pin);

            var login = graphQL.call(format(loginRequestStr, accountNumber, pin))         // login
                    .print("  login entity");
            String entityToken = login.getStr("accessToken.token");

            var getEntity = graphQL.call(getEntityFull, entityToken)
                    .print("  get entity");
            checkEntity(getEntity);
            checkZimAccounts(entity);

            var entityTypes = graphQL.call(getEntityTypes)
                    .print("  entity types");
            Integer personalEntityTypeId = entityTypes.getInt("[description = Personal].id");
            Integer businessEntityTypeId = entityTypes.getInt("[description = Business].id");
            assertThat(personalEntityTypeId).isNotNull();
            assertThat(businessEntityTypeId).isNotNull();

            var updateEntity = graphQL.call(format(updateEntityByStaff, entityId, businessEntityTypeId, "newFirstName", "newLastName",
                            passportIdTypeId, "3333333", "FEMALE", "FROZEN", "1975-11-22", mozCountryId, "test.backoffice.user2@ice.cash", "TOTP", "pt"), token)
                    .print("  update entity");
            assertThat(updateEntity.getStr("email")).isEqualTo("test.backoffice.user2@ice.cash");
            assertThat(updateEntity.getInt("entityTypeId")).isEqualTo(businessEntityTypeId);
            assertThat(updateEntity.getStr("firstName")).isEqualTo("newFirstName");
            assertThat(updateEntity.getStr("lastName")).isEqualTo("newLastName");
            assertThat(updateEntity.getInt("idTypeId")).isEqualTo(passportIdTypeId);
            assertThat(updateEntity.getStr("idNumber")).isEqualTo("3333333");
            assertThat(updateEntity.getStr("gender")).isEqualTo("FEMALE");
            assertThat(updateEntity.getStr("status")).isEqualTo("FROZEN");
            assertThat(updateEntity.getStr("birthDate")).isEqualTo("1975-11-22");
            assertThat(updateEntity.getInt("citizenshipCountryId")).isEqualTo(mozCountryId);
            assertThat(updateEntity.getStr("mfaType")).isEqualTo("TOTP");
            assertThat(updateEntity.getStr("locale")).isEqualTo("pt");

            graphQL.call(format(updateEntityByStaff, entityId, personalEntityTypeId, FIRST_NAME2, LAST_NAME2,       // return entity data
                            nationalIdTypeId, ID_NUMBER2, GENDER, ACTIVE, "1975-11-22", mozCountryId, EMAIL2, null, "en"), token)
                    .print("  update entity");
            getEntity = graphQL.call(getEntityFull, entityToken)
                    .print("  get entity");
            checkEntity(getEntity);
            checkZimAccounts(entity);
            assertThat(getEntity.getStr("birthDate")).isEqualTo("1975-11-22");

        } finally {
            rest.deleteMozUser(entityId);
        }
    }

    @Test
    public void testRegisterMozIndividualEntity() {
        var entityTypes = graphQL.call(getEntityTypes)
                .print("  entity types");
        Integer privateEntityTypeId = entityTypes.getInt("[description = Private].id");
        assertThat(privateEntityTypeId).isNotNull();

        var user = graphQL.call(format(mozRegIndividualUserByStaff, "CommuterRegular", FIRST_NAME2, LAST_NAME2,
                        "ID", ID_NUMBER2, null, NUIT2, null, MOBILE, EMAIL2, "en",
                        ACTIVE, ACTIVE, GENDER, mozCountryId, CONTACT1, CONTACT2, MOBILE, AUTH_TYPE, CORPORATE_FEE, TIER, KYC1, COMPANY,
                        zimCountryId, CITY, ZIP, ADDR_1, ADDR_2, NOTES), token)
                .print("  commuter");
        assertThat(user.getInt("id")).isGreaterThan(0);
        int entityId = user.getInt("id");
        try {
            assertThat(user.getInt("entityTypeId")).isEqualTo(privateEntityTypeId);
            assertThat(user.getStr("entityType.description")).isEqualTo(EntityType.PRIVATE);
            checkEntity(user);
            assertThat(user.getStr("metaData." + EntityMetaKey.AccountTypeMoz)).isEqualTo("CommuterRegular");
            assertThat(user.getStr("metaData." + EntityMetaKey.Nuit)).isEqualTo(NUIT2);
            assertThat(user.getStr("metaData." + EntityMetaKey.CreatedByStaffId)).isNotBlank();
            checkMozAccounts(user);
            checkMozRelationships(user, 1013, List.of("MOZ_TOPUP", "MOZ_PROFILE", "MOZ_TAG_MANAGEMENT", "MOZ_STATEMENT"));      // Moz Commuter

            String accountNumber = user.getStr("accounts[0].accountNumber");
            String pin = rest.sendSimpleGetRequest("/users/pin/entity", "username=" + accountNumber);
            System.out.println("  PIN: " + pin);

            var login = graphQL.call(format(loginRequestStr, accountNumber, pin))         // login
                    .print("  login entity");
            String entityToken = login.getStr("accessToken.token");

            var getEntity = graphQL.call(getEntityFull, entityToken)
                    .print("  get entity");
            checkEntity(getEntity);
            assertThat(user.getStr("metaData." + EntityMetaKey.AccountTypeMoz)).isEqualTo("CommuterRegular");
            assertThat(user.getStr("metaData." + EntityMetaKey.Nuit)).isEqualTo(NUIT2);
            assertThat(user.getStr("metaData." + EntityMetaKey.CreatedByStaffId)).isNotBlank();
            checkMozAccounts(user);
            checkMozRelationships(user, 1013, List.of("MOZ_TOPUP", "MOZ_PROFILE", "MOZ_TAG_MANAGEMENT", "MOZ_STATEMENT"));      // Moz Commuter
        } finally {
            rest.deleteMozUser(entityId);
        }
    }

    @Test
    public void testRegisterMozCorporateEntity() {
        var entityTypes = graphQL.call(getEntityTypes)
                .print("  entity types");
        Integer privateEntityTypeId = entityTypes.getInt("[description = Business].id");
        assertThat(privateEntityTypeId).isNotNull();

        var user = graphQL.call(format(mozRegCorporateUserByStaff, COMPANY, NUEL, null, NUIT, null, MOBILE, EMAIL2,
                        mozCountryId, "Maputo", "23456", "addr3", "addr4", "notes2",
                        "CommuterStudent", FIRST_NAME2, LAST_NAME2, "ID", ID_NUMBER2, null, NUIT2, null, MOBILE, EMAIL2, "en",
                        ACTIVE, ACTIVE, GENDER, mozCountryId, CONTACT1, CONTACT2, MOBILE, AUTH_TYPE, CORPORATE_FEE, TIER, KYC1,
                        zimCountryId, CITY, ZIP, ADDR_1, ADDR_2, NOTES), token)
                .print("  commuter");
        assertThat(user.getInt("id")).isGreaterThan(0);
        int entityId = user.getInt("id");
        try {
            assertThat(user.getInt("entityTypeId")).isEqualTo(privateEntityTypeId);
            assertThat(user.getStr("entityType.description")).isEqualTo(EntityType.BUSINESS);
            checkCorporateEntity(user);
            assertThat(user.getStr("metaData." + EntityMetaKey.AccountTypeMoz)).isEqualTo("CommuterStudent");
            assertThat(user.getStr("metaData." + EntityMetaKey.Nuit)).isEqualTo(NUIT);
            assertThat(user.getStr("metaData." + EntityMetaKey.CreatedByStaffId)).isNotBlank();
            checkMozAccounts(user);
            checkMozRelationships(user, 1013, List.of("MOZ_TOPUP", "MOZ_PROFILE", "MOZ_TAG_MANAGEMENT", "MOZ_STATEMENT"));      // Moz Commuter

            String accountNumber = user.getStr("accounts[0].accountNumber");
            String pin = rest.sendSimpleGetRequest("/users/pin/entity", "username=" + accountNumber);
            System.out.println("  PIN: " + pin);

            var login = graphQL.call(format(loginRequestStr, accountNumber, pin))         // login
                    .print("  login entity");
            String entityToken = login.getStr("accessToken.token");

            var getEntity = graphQL.call(getEntityFull, entityToken)
                    .print("  get entity");
            checkCorporateEntity(getEntity);
            assertThat(user.getStr("metaData." + EntityMetaKey.AccountTypeMoz)).isEqualTo("CommuterStudent");
            assertThat(user.getStr("metaData." + EntityMetaKey.Nuit)).isEqualTo(NUIT);
            assertThat(user.getStr("metaData." + EntityMetaKey.CreatedByStaffId)).isNotBlank();
            checkMozAccounts(user);
            checkMozRelationships(user, 1013, List.of("MOZ_TOPUP", "MOZ_PROFILE", "MOZ_TAG_MANAGEMENT", "MOZ_STATEMENT"));      // Moz Commuter
        } finally {
            rest.deleteMozUser(entityId);
        }
    }

    private void checkEntity(Wrapper entity) {
        assertThat(entity.getStr("email")).isEqualTo(EMAIL2);
        assertThat(entity.getStr("status")).isEqualTo(ACTIVE);
        assertThat(entity.getStr("loginStatus")).isEqualTo(ACTIVE);
        assertThat(entity.getStr("firstName")).isEqualTo(FIRST_NAME2);
        assertThat(entity.getStr("lastName")).isEqualTo(LAST_NAME2);
        assertThat(entity.getInt("idTypeId")).isEqualTo(nationalIdTypeId);
        assertThat(entity.getStr("idType.description")).isEqualTo(NATIONAL_ID_TYPE);
        assertThat(entity.getStr("idNumber")).isEqualTo(ID_NUMBER2);
        assertThat(entity.getInt("kycStatusId")).isEqualTo(KYC.PARTIAL.ordinal());
        assertThat(entity.getInt("citizenshipCountryId")).isEqualTo(mozCountryId);
        assertThat(entity.getStr("mfaType")).isEqualTo(null);
        assertThat(entity.getStrList("mfaBackupCodes").size()).isEqualTo(6);
        assertThat(entity.getStr("createdDate")).isNotNull();

        assertThat(entity.getStr("metaData." + EntityMetaKey.Company)).isEqualTo(COMPANY);
        assertThat(entity.getStr("metaData." + EntityMetaKey.TransactionLimitTier)).isEqualTo(TIER);
        assertThat(entity.toBool("metaData." + EntityMetaKey.CorporateFee)).isEqualTo(CORPORATE_FEE);

        assertThat(entity.getListSize("msisdn")).isEqualTo(2);
        assertThat(entity.getStr("msisdn[0].msisdnType")).isEqualTo("PRIMARY");
        assertThat(entity.getStr("msisdn[0].msisdn")).isEqualTo(MOBILE);
        assertThat(entity.getStr("msisdn[0].description")).isEqualTo(CONTACT1);
        assertThat(entity.getStr("msisdn[1].msisdnType")).isEqualTo("SECONDARY");
        assertThat(entity.getStr("msisdn[1].msisdn")).isEqualTo(MOBILE);
        assertThat(entity.getStr("msisdn[1].description")).isEqualTo(CONTACT2);

        assertThat(entity.getListSize("address")).isEqualTo(1);
        assertThat(entity.getStr("address[0].addressType")).isEqualTo(AddressType.PRIMARY.toString());
        assertThat(entity.getInt("address[0].countryId")).isEqualTo(zimCountryId);
        assertThat(entity.getStr("address[0].city")).isEqualTo(CITY);
        assertThat(entity.getStr("address[0].postalCode")).isEqualTo(ZIP);
        assertThat(entity.getStr("address[0].address1")).isEqualTo(ADDR_1);
        assertThat(entity.getStr("address[0].address2")).isEqualTo(ADDR_2);
        assertThat(entity.getStr("address[0].notes")).isEqualTo(NOTES);
    }

    private void checkCorporateEntity(Wrapper entity) {
        assertThat(entity.getStr("email")).isEqualTo(EMAIL2);
        assertThat(entity.getStr("status")).isEqualTo(ACTIVE);
        assertThat(entity.getStr("loginStatus")).isEqualTo(ACTIVE);
        assertThat(entity.getStr("firstName")).isEqualTo(COMPANY);
        assertThat(entity.getStr("lastName")).isEqualTo(null);
        assertThat(entity.getInt("idTypeId")).isEqualTo(nuelIdTypeId);
        assertThat(entity.getStr("idType.description")).isEqualTo(NUEL_ID_TYPE);
        assertThat(entity.getStr("idNumber")).isEqualTo(NUEL);
        assertThat(entity.getInt("kycStatusId")).isEqualTo(KYC.PARTIAL.ordinal());
        assertThat(entity.getInt("citizenshipCountryId")).isEqualTo(mozCountryId);
        assertThat(entity.getStr("mfaType")).isEqualTo(null);
        assertThat(entity.getStrList("mfaBackupCodes").size()).isEqualTo(6);
        assertThat(entity.getStr("createdDate")).isNotNull();

        assertThat(entity.getStr("metaData." + EntityMetaKey.TransactionLimitTier)).isEqualTo(TIER);
        assertThat(entity.toBool("metaData." + EntityMetaKey.CorporateFee)).isEqualTo(CORPORATE_FEE);

        assertThat(entity.getListSize("msisdn")).isEqualTo(3);
        assertThat(entity.getStr("msisdn[0].msisdnType")).isEqualTo("PRIMARY");
        assertThat(entity.getStr("msisdn[0].msisdn")).isEqualTo(MOBILE);
        assertThat(entity.getStr("msisdn[0].description")).isEqualTo(CONTACT1);
        assertThat(entity.getStr("msisdn[1].msisdnType")).isEqualTo("SECONDARY");
        assertThat(entity.getStr("msisdn[1].msisdn")).isEqualTo(MOBILE);
        assertThat(entity.getStr("msisdn[1].description")).isEqualTo(format("%s %s", FIRST_NAME2, LAST_NAME2));
        assertThat(entity.getStr("msisdn[2].msisdnType")).isEqualTo("SECONDARY");
        assertThat(entity.getStr("msisdn[2].msisdn")).isEqualTo(MOBILE);
        assertThat(entity.getStr("msisdn[2].description")).isEqualTo(CONTACT2);

        assertThat(entity.getListSize("address")).isEqualTo(2);
        assertThat(entity.getStr("address[0].addressType")).isEqualTo(AddressType.BUSINESS.toString());
        assertThat(entity.getInt("address[0].countryId")).isEqualTo(mozCountryId);
        assertThat(entity.getStr("address[0].city")).isEqualTo("Maputo");
        assertThat(entity.getStr("address[0].postalCode")).isEqualTo("23456");
        assertThat(entity.getStr("address[0].address1")).isEqualTo("addr3");
        assertThat(entity.getStr("address[0].address2")).isEqualTo("addr4");
        assertThat(entity.getStr("address[0].notes")).isEqualTo("notes2");
        assertThat(entity.getStr("address[1].addressType")).isEqualTo(AddressType.PRIMARY.toString());
        assertThat(entity.getInt("address[1].countryId")).isEqualTo(zimCountryId);
        assertThat(entity.getStr("address[1].city")).isEqualTo(CITY);
        assertThat(entity.getStr("address[1].postalCode")).isEqualTo(ZIP);
        assertThat(entity.getStr("address[1].address1")).isEqualTo(ADDR_1);
        assertThat(entity.getStr("address[1].address2")).isEqualTo(ADDR_2);
        assertThat(entity.getStr("address[1].notes")).isEqualTo(NOTES);
    }

    private void checkZimAccounts(Wrapper entity) {
        assertThat(entity.getListSize("accounts")).isEqualTo(1);
        assertThat(entity.getStr("accounts[ 0 ].accountNumber")).isNotBlank();
        assertThat(entity.getStr("accounts[ 0 ].accountType.name")).isEqualTo("Primary");
        assertThat(entity.getStr("accounts[ 0 ].accountType.currency.isoCode")).isEqualTo("ZWL");
        assertThat(entity.toBool("accounts[ 0 ].accountType.active")).isTrue();
        assertThat(entity.getStr("accounts[ 0 ].accountStatus")).isEqualTo("ACTIVE");
        assertThat(entity.getStr("accounts[ 0 ].authorisationType")).isEqualTo(AUTH_TYPE);
        assertThat(entity.getDbl("accounts[ 0 ].balance")).isEqualTo(0.0);
    }

    private void checkMozAccounts(Wrapper entity) {
        assertThat(entity.getListSize("accounts")).isEqualTo(3);
        assertThat(entity.getStr("accounts[ 0 ].accountNumber")).isNotBlank();
        assertThat(entity.getStr("accounts[ 0 ].accountType.name")).isEqualTo("Primary");
        assertThat(entity.getStr("accounts[ 0 ].accountType.currency.isoCode")).isEqualTo("MZN");
        assertThat(entity.toBool("accounts[ 0 ].accountType.active")).isTrue();
        assertThat(entity.getStr("accounts[ 0 ].accountStatus")).isEqualTo("ACTIVE");
        assertThat(entity.getStr("accounts[ 0 ].authorisationType")).isEqualTo(AUTH_TYPE);
        assertThat(entity.getDbl("accounts[ 0 ].balance")).isEqualTo(0.0);
        assertThat(entity.getStr("accounts[ 1 ].accountNumber")).isNotBlank();
        assertThat(entity.getStr("accounts[ 1 ].accountType.name")).isEqualTo("Subsidy");
        assertThat(entity.getStr("accounts[ 1 ].accountType.currency.isoCode")).isEqualTo("MZN");
        assertThat(entity.toBool("accounts[ 1 ].accountType.active")).isTrue();
        assertThat(entity.getStr("accounts[ 1 ].accountStatus")).isEqualTo("ACTIVE");
        assertThat(entity.getStr("accounts[ 1 ].authorisationType")).isEqualTo(AUTH_TYPE);
        assertThat(entity.getDbl("accounts[ 1 ].balance")).isEqualTo(0.0);
        assertThat(entity.getStr("accounts[ 2 ].accountNumber")).isNotBlank();
        assertThat(entity.getStr("accounts[ 2 ].accountType.name")).isEqualTo("Prepaid");
        assertThat(entity.getStr("accounts[ 2 ].accountType.currency.isoCode")).isEqualTo("MZN");
        assertThat(entity.toBool("accounts[ 2 ].accountType.active")).isTrue();
        assertThat(entity.getStr("accounts[ 2 ].accountStatus")).isEqualTo("ACTIVE");
        assertThat(entity.getStr("accounts[ 2 ].authorisationType")).isEqualTo(AUTH_TYPE);
        assertThat(entity.getDbl("accounts[ 2 ].balance")).isEqualTo(0.0);
    }

    private void checkMozRelationships(Wrapper entity, int securityGroupId, List<String> securityRights) {
        assertThat(entity.getListSize("relationships[ last ].securityGroupMoz")).isEqualTo(1);
        assertThat(entity.getInt("relationships[ last ].securityGroupMoz[ 0 ].id")).isEqualTo(securityGroupId);
        assertThat(entity.toBool("relationships[ last ].securityGroupMoz[ 0 ].active")).isTrue();
        assertThat(entity.getStrList("relationships[ last ].securityGroupMoz[ 0 ].rightsList")).isEqualTo(securityRights);
    }

    @AfterAll
    public void destroy() {
        if (staffMemberId != null) {
            graphQL.call(format(deleteStaffMember, staffMemberId))
                    .print("  delete backoffice user");
        }
    }

    private String loginTokenBackofficeUser(String email, String password, boolean resendOTP) {
        return loginBackofficeUser(email, password, resendOTP).getStr("accessToken.token");
    }

    private Wrapper loginBackofficeUser(String email, String password, boolean resendOTP) {
        var login = graphQL.call(format(loginStaffMember, email, password))
                .print("  login");
        assertThat(login.getStr("status")).isEqualTo("MFA_REQUIRED");
        assertThat(login.getStr("mfaType")).isEqualTo("OTP");
        assertThat(login.getStr("msisdn")).isEqualTo(MOBILE);
        assertThat(login.getStr("accessToken")).isNull();

        if (resendOTP) {
            graphQL.call(format(resendStaffMemberOTP, email))
                    .print("  resend OTP");
        }
        String otp = rest.sendSimpleGetRequest("/users/pin/staff/otp", "email=" + email);
        System.out.println("  otp: " + otp);
        assertThat(otp).isNotBlank();

        var loginMfa = graphQL.call(format(loginMfaStaffMember, email, otp))
                .print("  mfa");
        assertThat(loginMfa.getStr("status")).isEqualTo("SUCCESS");
        assertThat(loginMfa.getStr("mfaType")).isEqualTo("OTP");
        assertThat(loginMfa.getStr("accessToken.token")).isNotBlank();
        assertThat(loginMfa.getStr("accessToken.refreshToken")).isNotBlank();
        assertThat(loginMfa.getInt("accessToken.expiresIn")).isGreaterThan(0);
        assertThat(loginMfa.getInt("accessToken.refreshExpiresIn")).isGreaterThan(0);
        return loginMfa;
    }
}

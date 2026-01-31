package cash.ice.e2e.common;

import cash.ice.ApiUtil;
import cash.ice.GraphQLError;
import cash.ice.GraphQLHelper;
import cash.ice.RestHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static cash.ice.GraphQlRequests.*;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EntityAuthTests {
    private static final String FIRST_NAME = "testUser";
    private static final String LAST_NAME = "e2eTest";
    private static final String EMAIL = "test.e2e@ice.cash";
    private static final String ID_NUMBER = "1213141541";

    private final RestHelper rest = new RestHelper();
    private final GraphQLHelper graphQL = new GraphQLHelper(rest);
    private final ApiUtil apiUtil = new ApiUtil(rest, graphQL);

    private Integer entityId;
    private Integer accountId;
    private String password;
    private String entityToken;
    private String accountNumber;

    @BeforeAll
    public void init() {
        System.out.println("  init()");
        createEntity();
    }

    private void createEntity() {
        var entity = graphQL.call(String.format(simpleRegisterEntity, "Personal", FIRST_NAME, LAST_NAME, 1, ID_NUMBER, "000000000000", EMAIL))
                .print("  new entity");
        entityId = entity.getInt("id");
        accountId = entity.getInt("accounts[0].id");
        accountNumber = entity.getStr("accounts[0].accountNumber");
        assertThat(entity.getStr("loginStatus")).isEqualTo("ACTIVE");
        assertThat(entity.getStr("mfaType")).isNull();

        password = rest.sendSimpleGetRequest("/users/pin/entity", "username=" + accountNumber);
        System.out.println("  password: " + password);
        entityToken = apiUtil.mozLogin(accountNumber, password);
    }

    @Test
    public void testSimpleLogin() {
        var login = graphQL.call(format(loginRequestStr, accountNumber, password))
                .print("  login");
        assertThat(login.getStr("status")).isEqualTo("SUCCESS");
        assertThat(login.getStr("mfaType")).isNull();
        assertThat(login.getStr("accessToken.token")).isNotBlank();
        assertThat(login.getInt("accessToken.expiresIn")).isGreaterThan(0);              // 3600
        assertThat(login.getStr("accessToken.refreshToken")).isNotBlank();
        assertThat(login.getInt("accessToken.refreshExpiresIn")).isGreaterThan(0);       // 1800
        assertThat(login.getStr("accessToken.error")).isNull();
        assertThat(login.getStr("accessToken.errorDescription")).isNull();

        graphQL.call(getEntityFull, login.getStr("accessToken.token"))
                .print("  entity");
    }

    @Test
    public void testOtpLogin() {
        var updateMfaType = graphQL.call(format(updateMfa, entityId, "OTP"))
                .print("  update mfa type");
        assertThat(updateMfaType.getStr("mfaType")).isEqualTo("OTP");

        var login = graphQL.call(format(loginRequestStr, accountNumber, password))
                .print("  login");
        assertThat(login.getStr("status")).isEqualTo("MFA_REQUIRED");
        assertThat(login.getStr("mfaType")).isEqualTo("OTP");
        assertThat(login.getObject("accessToken")).isNull();

        String otp = rest.sendSimpleGetRequest("/users/pin/entity/otp", "entityId=" + entityId);
        System.out.println("  otp: " + otp);
        assertThat(otp).isNotBlank();

        var loginMfa = graphQL.call(String.format(enterLoginMfa, accountNumber, otp))
                .print("  mfa");
        assertThat(loginMfa.getStr("status")).isEqualTo("SUCCESS");
        assertThat(loginMfa.getStr("mfaType")).isEqualTo("OTP");
        assertThat(loginMfa.getStr("accessToken.token")).isNotBlank();
        assertThat(loginMfa.getInt("accessToken.expiresIn")).isGreaterThan(0);              // 3600
        assertThat(loginMfa.getStr("accessToken.refreshToken")).isNotBlank();
        assertThat(loginMfa.getInt("accessToken.refreshExpiresIn")).isGreaterThan(0);       // 1800
        assertThat(loginMfa.getStr("accessToken.error")).isNull();
        assertThat(loginMfa.getStr("accessToken.errorDescription")).isNull();

        graphQL.call(getEntityFull, loginMfa.getStr("accessToken.token"))
                .print("  entity");

        updateMfaType = graphQL.call(format(updateMfa, entityId, null))
                .print("  update mfa type");
        assertThat(updateMfaType.getStr("mfaType")).isNull();
    }

    @Test
    public void testTotpLogin() {
        String mfaSecretCode = rest.sendSimpleGetRequest("/users/pin/entity/mfa/secret", "entityId=" + entityId);             // get mfaSecretCode
        System.out.println("  mfaSecretCode: " + mfaSecretCode);
        assertThat(mfaSecretCode).isNotBlank();

        var updateMfaType = graphQL.call(format(updateMfa, entityId, "TOTP"))           // set totp mfa
                .print("  update mfa type");
        assertThat(updateMfaType.getStr("mfaType")).isEqualTo("TOTP");

        graphQL.call(format(loginRequestStr, accountNumber, password))
                .print("  login");
        assertThat(updateMfaType.getStr("mfaType")).isEqualTo("TOTP");

        String totp = rest.sendSimpleGetRequest("/user/totp/code", "mfaSecretCode=" + mfaSecretCode);             // get totp
        System.out.println("  totp: " + totp);
        assertThat(totp.length()).isEqualTo(6);

        var checkTotp = graphQL.call(format(checkTotpCode, totp), entityToken)                // check totp
                .print("  check totp");
        assertThat(checkTotp.getBool()).isTrue();

        var loginMfa = graphQL.call(format(enterLoginMfa, accountNumber, totp))
                .print("  mfa");
        assertThat(loginMfa.getStr("status")).isEqualTo("SUCCESS");
        assertThat(loginMfa.getStr("mfaType")).isEqualTo("TOTP");
        assertThat(loginMfa.getStr("accessToken.token")).isNotNull();

        graphQL.call(format(updateMfa, entityId, null))                             // return null mfa
                .print("  update mfa type");
    }

    @Test
    public void testLoginInactive() {
        var updLoginStatus = graphQL.call(format(updateLoginStatus, accountNumber, "INACTIVE"), entityToken)
                .print("  update INACTIVE login status");
        assertThat(updLoginStatus.getStr("loginStatus")).isEqualTo("INACTIVE");

        var exception = graphQL.callForError(format(loginRequestStr, accountNumber, password))
                .print("  locked exception");
        assertThat(exception.getErrorMessage()).isEqualTo("Account is INACTIVE for login");
        assertThat(exception.getErrorCode()).isEqualTo("101-IC1225-0035");

        updLoginStatus = graphQL.call(format(updateLoginStatus, accountNumber, "ACTIVE"), entityToken)
                .print("  update ACTIVE login status");
        assertThat(updLoginStatus.getStr("loginStatus")).isEqualTo("ACTIVE");

        var login = graphQL.call(format(loginRequestStr, accountNumber, password))
                .print("  login");
        assertThat(login.getStr("accessToken.token")).isNotBlank();
    }

    //    @Test                                 // todo
    public void testLoginLockUnlock() {
        String wrongPassword = "1111";
        var exception = graphQL.callForError(format(loginRequestStr, accountNumber, wrongPassword))
                .print("  wrong password exception");
        checkUnauthorized(exception);
        exception = graphQL.callForError(format(loginRequestStr, accountNumber, wrongPassword))
                .print("  wrong password exception");
        checkUnauthorized(exception);
        exception = graphQL.callForError(format(loginRequestStr, accountNumber, wrongPassword))
                .print("  wrong password exception");
        checkUnauthorized(exception);

        exception = graphQL.callForError(format(loginRequestStr, accountNumber, wrongPassword))
                .print("  locked exception");
        assertThat(exception.getErrorMessage()).isEqualTo("Account is LOCKED for login");
        assertThat(exception.getErrorCode()).isEqualTo("101-IC1225-0035");

        graphQL.call(format(updateLoginStatus, accountNumber, "ACTIVE"), entityToken)
                .print("  update ACTIVE login status");

        var login = graphQL.call(format(loginRequestStr, accountNumber, password))
                .print("  login");
        assertThat(login.getStr("accessToken.token")).isNotBlank();
    }

    private void checkUnauthorized(GraphQLError exception) {
        System.out.println("unauthorized exception: " + exception);
        assertThat(exception.getClassification()).isEqualTo("UNAUTHORIZED");
        assertThat(exception.getErrorMessage()).isEqualTo("HTTP 401 Unauthorized");
        assertThat(exception.getErrorCode()).isEqualTo("101-IC1146-0010");
    }

    @Test
    public void testBackupCodes() {
        var user = graphQL.call(getEntityFull, entityToken)
                .print("  entity");
        String backupCode = user.getStrList("mfaBackupCodes").getFirst();

        var enterBackup = graphQL.call(format(enterBackupCode, accountNumber, backupCode))
                .print("  enter backup code");
        String newToken = enterBackup.getStr("accessToken.token");
        assertThat(enterBackup.getStr("status")).isEqualTo("SUCCESS");
        assertThat(enterBackup.getStr("mfaType")).isEqualTo(null);
        assertThat(newToken).isNotBlank();

        user = graphQL.call(getEntityFull, entityToken)
                .print("  entity");
        List<String> backupCodes = user.getStrList("mfaBackupCodes");
        assertFalse(backupCodes.contains(backupCode));

        var exception = graphQL.callForError(format(enterBackupCode, accountNumber, backupCode));       // reuse code
        checkUnauthorized(exception);

        var generateBackupCodes = graphQL.call(generateNewBackupCodesForCurrentEntity, newToken)           // generate new backup codes
                .print("  generate backup codes");
        List<String> newBackupCodes = generateBackupCodes.getStrList("mfaBackupCodes");
        assertNotEquals(backupCodes, newBackupCodes);
        assertThat(newBackupCodes.size()).isEqualTo(6);

        enterBackup = graphQL.call(format(enterBackupCode, accountNumber, newBackupCodes.getFirst()))
                .print("  enter backup code");
        assertThat(enterBackup.getStr("status")).isEqualTo("SUCCESS");
        assertThat(enterBackup.getStr("accessToken.token")).isNotBlank();
    }

    @Test
    public void testUpdatePassword() {
        graphQL.call(format(generateNewPassword, entityId), entityToken)                // generate new password
                .print("  update password");
        String newPassword = rest.sendSimpleGetRequest("/users/pin/entity", "username=" + accountNumber);
        assertThat(newPassword).isNotEqualTo(password);

        var exception = graphQL.callForError(format(loginRequestStr, accountNumber, password))
                .print("  wrong password exception");
        checkUnauthorized(exception);
        var login = graphQL.call(format(loginRequestStr, accountNumber, newPassword))
                .print("  login");
        String newToken = login.getStr("accessToken.token");
        assertThat(newToken).isNotBlank();

        graphQL.call(format(updatePassword, newPassword, password), newToken)           // update password (return)
                .print("  return password");

        exception = graphQL.callForError(format(loginRequestStr, accountNumber, newPassword))
                .print("  wrong password exception");
        checkUnauthorized(exception);
        login = graphQL.call(format(loginRequestStr, accountNumber, password))
                .print("  login");
        assertThat(login.getStr("accessToken.token")).isNotBlank();
    }

    @Test
    public void testForgotPassword() {
        String newPassword = "4321";
        graphQL.call(format(forgotPassword, EMAIL))
                .print("  forgot password");

        String forgotKey = rest.sendSimpleGetRequest("/user/backoffice/forgot/key", "login=" + entityId);
        System.out.println("  forgotKey: " + forgotKey);
        assertThat(forgotKey).isNotBlank();

        graphQL.call(format(resetPassword, forgotKey, newPassword))
                .print("  reset password");

        var login = graphQL.call(format(loginRequestStr, accountNumber, newPassword))
                .print("  login");
        String newToken = login.getStr("accessToken.token");

        var userWrapper = graphQL.call(getEntityFull, newToken)
                .print("  user");
        assertThat(userWrapper).isNotNull();

        graphQL.call(format(forgotPassword, EMAIL))        // return password
                .print("  forgot password");

        forgotKey = rest.sendSimpleGetRequest("/user/backoffice/forgot/key", "login=" + entityId);
        System.out.println("  forgotKey: " + forgotKey);
        assertThat(forgotKey).isNotBlank();

        graphQL.call(format(resetPassword, forgotKey, password))
                .print("  reset password");
    }

    @Test
    public void testRefreshInvalidateToken() {
        var login = graphQL.call(format(loginRequestStr, accountNumber, password))
                .print("  login");
        String loginToken = login.getStr("accessToken.token");
        String refreshToken = login.getStr("accessToken.refreshToken");

        graphQL.call(getEntityFull, loginToken)
                .print("  user");

        var refresh = graphQL.call(format(refreshAccessTokenRequestStr, refreshToken))
                .print("  refresh token");
        loginToken = refresh.getStr("accessToken.token");
        String newRefreshToken = refresh.getStr("accessToken.refreshToken");
        assertNotEquals(refreshToken, newRefreshToken);

        graphQL.call(getEntityFull, loginToken)
                .print("  user");

        graphQL.call(format(invalidateAccessTokenRequestStr, newRefreshToken))        // invalidate
                .print("  invalidate token");

        var exception = graphQL.callForError(format(refreshAccessTokenRequestStr, newRefreshToken))
                .print("  refresh exception");
        assertThat(exception.getErrorMessage()).isEqualTo("Refresh token failed");
        assertThat(exception.getErrorCode()).isEqualTo("101-IC1116-0004");

        exception = graphQL.callForError(format(refreshAccessTokenRequestStr, refreshToken))
                .print("  refresh exception");
        assertThat(exception.getErrorMessage()).isEqualTo("Refresh token failed");
    }

    @Test
    public void testAccountBlockUnblock() {
        var updateAccount = graphQL.call(format(updateAccountActive, accountId, false), entityToken)
                .print("  update account active");
        try {
            assertThat(updateAccount.getStr("accountStatus")).isEqualTo("FROZEN");
            var accounts = graphQL.call(getAccounts, entityToken)
                    .print("  accounts");
            assertThat(accounts.getStr("[ accountNumber = {accountNumber} ].accountStatus", accountNumber)).isEqualTo("FROZEN");

            var exception = graphQL.callForError(format(loginRequestStr, accountNumber, password))
                    .print("  login");
            assertThat(exception.getErrorMessage()).isEqualTo(String.format("Account '%s' is not active", accountNumber));
        } finally {
            updateAccount = graphQL.call(format(updateAccountActive, accountId, true), entityToken)
                    .print("  update account active");
            assertThat(updateAccount.getStr("accountStatus")).isEqualTo("ACTIVE");
            var accounts = graphQL.call(getAccounts, entityToken)
                    .print("  accounts");
            assertThat(accounts.getStr("[ accountNumber = {accountNumber} ].accountStatus", accountNumber)).isEqualTo("ACTIVE");
        }
    }

    @Test
    public void testSearchEntities() {
        var entities = graphQL.call(format(searchEntities, "ENTITY_ID", entityId, true), entityToken)
                .print("  search entities (id, exact)");
        assertThat(entities.getListSize("content")).isEqualTo(1);
        assertThat(entities.getInt("content[ 0 ].id")).isEqualTo(entityId);

        entities = graphQL.call(format(searchEntities, "ENTITY_ID", entityId, false), entityToken)
                .print("  search entities (id)");
        assertThat(entities.getListSize("content")).isGreaterThan(0);
        assertThat(entities.getInt("content[ 0 ].id")).isEqualTo(entityId);

        entities = graphQL.call(format(searchEntities, "ACCOUNT_NUMBER", accountNumber, true), entityToken)
                .print("  search entities (account, exact)");
        assertThat(entities.getListSize("content")).isEqualTo(1);
        assertThat(entities.getStr("content[ 0 ].accounts[ 0 ].accountNumber")).isEqualTo(accountNumber);

        entities = graphQL.call(format(searchEntities, "ACCOUNT_NUMBER", accountNumber, false), entityToken)
                .print("  search entities (account)");
        assertThat(entities.getListSize("content")).isGreaterThan(0);
        assertThat(entities.getStr("content[ 0 ].accounts[ 0 ].accountNumber")).isEqualTo(accountNumber);

        entities = graphQL.call(format(searchEntities, "NAMES", format("%s %s", FIRST_NAME, LAST_NAME), true), entityToken)
                .print("  search entities (names, exact)");
        assertThat(entities.getListSize("content")).isEqualTo(1);
        assertThat(entities.getStr("content[ 0 ].firstName")).isEqualTo(FIRST_NAME);
        assertThat(entities.getStr("content[ 0 ].lastName")).isEqualTo(LAST_NAME);

        entities = graphQL.call(format(searchEntities, "NAMES", format("%s %s", FIRST_NAME, LAST_NAME), false), entityToken)
                .print("  search entities (names)");
        assertThat(entities.getListSize("content")).isGreaterThan(0);
        assertThat(entities.getStr("content[ 0 ].firstName")).isEqualTo(FIRST_NAME);
        assertThat(entities.getStr("content[ 0 ].lastName")).isEqualTo(LAST_NAME);

        entities = graphQL.call(format(searchEntities, "ID_NUMBER", ID_NUMBER, true), entityToken)
                .print("  search entities (id_num, exact)");
        assertThat(entities.getListSize("content")).isEqualTo(1);
        assertThat(entities.getStr("content[ 0 ].idNumber")).isEqualTo(ID_NUMBER);

        entities = graphQL.call(format(searchEntities, "ID_NUMBER", ID_NUMBER, false), entityToken)
                .print("  search entities (id_num)");
        assertThat(entities.getListSize("content")).isGreaterThan(0);
        assertThat(entities.getStr("content[ 0 ].idNumber")).isEqualTo(ID_NUMBER);
    }

    @AfterAll
    public void destroy() {
        System.out.println("  destroy()");
        if (entityId != null) {
            rest.deleteMozUser(entityId);
        }
    }
}

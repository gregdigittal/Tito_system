package cash.ice.e2e.moz;

import cash.ice.ApiUtil;
import cash.ice.GraphQLHelper;
import cash.ice.RestHelper;
import cash.ice.Wrapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;

import static cash.ice.GraphQlRequests.*;
import static cash.ice.sqldb.entity.InitiatorType.TAG;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MozMe60Tests {
    private static final String MOBILE = "000000000000";
    private static final String EMAIL = "test.user@ice.cash";
    private static final String FIRST_NAME = "Test user";
    private static final String LAST_NAME = "Some test user";
    private static final String ID_NUMBER = "1213141515";
    private static final String PASSWORD = "1234";
    private static final String DEVICE_SERIAL = "test-device-serial";
    private static final String TEST_TAG = "testTag";

    private final RestHelper rest = new RestHelper();
    private final GraphQLHelper graphQL = new GraphQLHelper(rest);
    private final ApiUtil apiUtil = new ApiUtil(rest, graphQL);

    private Integer entityId;
    private Wrapper userWrapper;
    private String token;

    @BeforeAll
    public void init() {
        System.out.println("  init()");
        Wrapper userWrapper = rest.sendPostMultipartRequest("/moz/user/register", body -> {
            body.add("firstName", "Test user");
            body.add("lastName", "Some test user");
            body.add("email", EMAIL);
            body.add("mobile", MOBILE);
            body.add("idNumber", ID_NUMBER);
            body.add("pin", PASSWORD);
            body.add("locale", "en");
            body.add("photo", rest.getByteArrayResource("account.png", "SomePhoto.png"));
        });
        System.out.println("  user register response: " + userWrapper);
        String accountNumber = userWrapper.getStr("accountNumber");
        System.out.println("  accountNumber: " + accountNumber);

        token = apiUtil.mozLogin(accountNumber, PASSWORD);
        this.userWrapper = graphQL.call(mozUserDetailsRequestStr, token)
                .print("  user details");
        entityId = this.userWrapper.getInt("id");
        System.out.println("  entityId: " + entityId);
    }

    @AfterAll
    public void destroy() {
        System.out.println("  destroy()");
        if (entityId != null) {
            rest.deleteMozUser(entityId);
        }
    }

    @Test
    public void testGetUserDetails() {
        assertThat(userWrapper.getStr("firstName")).isEqualTo(FIRST_NAME);
        assertThat(userWrapper.getStr("lastName")).isEqualTo(LAST_NAME);
        assertThat(userWrapper.getStr("email")).isEqualTo("test.user@ice.cash");
        assertThat(userWrapper.getStr("msisdn[0].msisdn")).isEqualTo(MOBILE);
        assertThat(userWrapper.getStr("idNumber")).isEqualTo(ID_NUMBER);
        assertThat(userWrapper.getStr("idType.description")).isEqualTo("Mozambican National ID");
        assertThat(userWrapper.getStr("entityType.description")).isEqualTo("Private");
        assertThat(userWrapper.getStr("status")).isEqualTo("ACTIVE");
        assertThat(userWrapper.getStr("loginStatus")).isEqualTo("ACTIVE");
        assertThat(userWrapper.getStr("locale")).isEqualTo("en");
        assertThat(userWrapper.getStr("createdDate")).isNotNull();

        assertThat(userWrapper.getListSize("accounts")).isEqualTo(3);
        assertThat(userWrapper.getListSize("relationships")).isEqualTo(3);

        assertThat(userWrapper.getStr("accounts[ 0 ].accountNumber")).isNotNull();
        assertThat(userWrapper.getStr("accounts[ 0 ].accountType.name")).isEqualTo("Primary");
        assertThat(userWrapper.getInt("accounts[ 0 ].accountType.currencyId")).isGreaterThan(0);
        assertThat(userWrapper.getStr("accounts[ 0 ].createdDate")).isNotNull();
        assertThat(userWrapper.getInt("relationships[ 0 ].partnerAccountId")).isGreaterThan(0);
        assertThat(userWrapper.getInt("relationships[ 0 ].partnerAccountId")).isEqualTo(userWrapper.getInt("accounts[ 0 ].id"));
        assertThat(userWrapper.getInt("relationships[ 0 ].securityGroups.MOZ[ 0 ]")).isGreaterThan(0);

        assertThat(userWrapper.getStr("accounts[ 1 ].accountNumber")).isNotNull();
        assertThat(userWrapper.getStr("accounts[ 1 ].accountType.name")).isEqualTo("Subsidy");
        assertThat(userWrapper.getInt("accounts[ 1 ].accountType.currencyId")).isGreaterThan(0);
        assertThat(userWrapper.getStr("accounts[ 1 ].createdDate")).isNotNull();
        assertThat(userWrapper.getInt("relationships[ 1 ].partnerAccountId")).isGreaterThan(0);
        assertThat(userWrapper.getInt("relationships[ 1 ].partnerAccountId")).isEqualTo(userWrapper.getInt("accounts[ 1 ].id"));
        assertThat(userWrapper.getInt("relationships[ 1 ].securityGroups.MOZ[ 0 ]")).isGreaterThan(0);

        assertThat(userWrapper.getStr("accounts[ 2 ].accountNumber")).isNotNull();
        assertThat(userWrapper.getStr("accounts[ 2 ].accountType.name")).isEqualTo("Prepaid");
        assertThat(userWrapper.getInt("accounts[ 2 ].accountType.currencyId")).isGreaterThan(0);
        assertThat(userWrapper.getStr("accounts[ 2 ].createdDate")).isNotNull();
        assertThat(userWrapper.getInt("relationships[ 2 ].partnerAccountId")).isGreaterThan(0);
        assertThat(userWrapper.getInt("relationships[ 2 ].partnerAccountId")).isEqualTo(userWrapper.getInt("accounts[ 2 ].id"));
        assertThat(userWrapper.getInt("relationships[ 2 ].securityGroups.MOZ[ 0 ]")).isGreaterThan(0);
    }

    @Test
    public void testUpdateLocale() {
        graphQL.call(format(mozUpdateLocaleRequestStr, "pt"), token)
                .print("  update locale");
        var updatedUserWrapper = graphQL.call(mozUserDetailsRequestStr, token)
                .print("  user details");
        assertThat(updatedUserWrapper.getStr("locale")).isEqualTo("pt");
        graphQL.call(format(mozUpdateLocaleRequestStr, "en"), token)
                .print("  update locale");
    }

    @Test
    public void testRefreshToken() {
        var login = graphQL.call(format(loginRequestStr, entityId, PASSWORD))
                .print("  login");
        String refreshToken = login.getStr("accessToken.refreshToken");
        var token = graphQL.call(format(refreshAccessTokenRequestStr, refreshToken))
                .print("  refresh access token");
        assertThat(token.getStr("status")).isEqualTo("SUCCESS");
        assertThat(token.getStr("accessToken.tokenType")).isEqualTo("Bearer");
        graphQL.call(mozUserDetailsRequestStr, token.getStr("accessToken.token"))
                .print("  user details");
    }

    @Test
    public void testLogout() {
        var loginWrapper = graphQL.call(format(loginRequestStr, entityId, PASSWORD))
                .print("  login");
        String refreshToken = loginWrapper.getStr("accessToken.refreshToken");
        graphQL.call(format(invalidateAccessTokenRequestStr, refreshToken))
                .print("  invalidate access token");
    }

    @Test
    public void testPosDevices() {
        String deviceCode = apiUtil.mozRegisterPosDevice(DEVICE_SERIAL);
        try {
            assertThat(deviceCode).isNotBlank();
            var devices = graphQL.call(mozGetPosDevices, token)
                    .print("  initial devices");
            assertThat(devices.getInt("total")).isEqualTo(0);
            assertThat(devices.getObjectList("content")).isEmpty();

            String primaryAccount = userWrapper.getStr("accounts[ accountType.name = Primary ].accountNumber");
            rest.sendPostMultipartRequest("/moz/me60/device/activate", body -> {
                body.add("serialOrCode", deviceCode);
                body.add("accountNumber", primaryAccount);
            });
            devices = graphQL.call(mozGetPosDevices, token)
                    .print("  devices after activate");
            assertThat(devices.getInt("total")).isEqualTo(1);
            assertThat(devices.getStr("content[ 0 ].code")).isEqualTo(deviceCode);
            assertThat(devices.getStr("content[ 0 ].serial")).isEqualTo(DEVICE_SERIAL);
            assertThat(devices.getStr("content[ 0 ].status")).isEqualTo("ACTIVE");
            assertThat(devices.getStr("content[ 0 ].account.accountNumber")).isEqualTo(primaryAccount);
            assertThat(devices.getStr("content[ 0 ].metaData.imei")).isEqualTo("test-imei");
            assertThat(devices.getStr("content[ 0 ].metaData.imsi")).isEqualTo("test-imsi");
            assertThat(devices.getStr("content[ 0 ].createdDate")).isNotNull();

            var accountInfoWrapper = rest.sendPostRequest("/moz/me60/account/info", Map.of("deviceSerial", DEVICE_SERIAL))
                    .print("  account info");
            assertThat(accountInfoWrapper.getInt("accountId")).isEqualTo(userWrapper.getInt("accounts[ accountType.name = Primary ].id"));
            assertThat(accountInfoWrapper.getStr("accountNumber")).isEqualTo(userWrapper.getStr("accounts[ accountType.name = Primary ].accountNumber"));
            assertThat(accountInfoWrapper.getStr("accountType")).isEqualTo("MZN Primary Wallet");
            assertThat(accountInfoWrapper.getStr("firstName")).isEqualTo(FIRST_NAME);
            assertThat(accountInfoWrapper.getStr("lastName")).isEqualTo(LAST_NAME);
            assertThat(accountInfoWrapper.getStr("deviceCode")).isEqualTo(deviceCode);
            assertThat(accountInfoWrapper.getStr("deviceStatus")).isEqualTo("ACTIVE");
        } finally {
            rest.sendSimpleDeleteRequest("/moz/me60/device/remove", "serialOrCode=" + deviceCode);
        }
    }

    @Test
    public void testLinkTagOld() {
        rest.sendSimplePostMultipartRequest(format("/user/%s/transactions/clear", entityId), body -> {
        });
        apiUtil.topupAccount(userWrapper.getStr("accounts[ accountType.name = Prepaid ].id"), "9999.0");
        apiUtil.topupAccount(userWrapper.getStr("accounts[ accountType.name = Subsidy ].id"), "888.0");
        String prepaidAccount = userWrapper.getStr("accounts[ accountType.name = Prepaid ].accountNumber");
        String deviceCode = apiUtil.mozRegisterPosDevice(DEVICE_SERIAL);
        try {
            rest.sendPostMultipartRequest("/moz/me60/device/activate", body -> {
                body.add("serialOrCode", deviceCode);
                body.add("accountNumber", userWrapper.getStr("accounts[ accountType.name = Primary ].accountNumber"));
            });
            String requestId = rest.sendSimplePostRequest("/moz/me60/tag/link", Map.of(
                    "deviceSerial", DEVICE_SERIAL,
                    "accountNumber", prepaidAccount,
                    "dateTime", "2023-02-07T12:30:10"
            ));
            assertThat(requestId).isNotBlank();

            String otp = rest.sendSimpleGetRequest("/moz/me60/tag/link/otp", "requestId=" + requestId);
            assertThat(otp).isNotBlank();

            Wrapper otpResponseWrapper = rest.sendPostRequest("/moz/me60/tag/link/otp", Map.of(
                    "requestId", requestId,
                    "otp", otp
            ));
            System.out.println("  link otp response: " + otpResponseWrapper);
            assertThat(otpResponseWrapper.getDbl("prepaidBalance")).isEqualTo(9999.0);
            assertThat(otpResponseWrapper.getDbl("subsidyBalance")).isEqualTo(888.0);

            Wrapper addTagWrapper = rest.sendPostMultipartRequest("/moz/me60/tag/link/clear", body -> body.add(TAG, TEST_TAG));
            try {
                System.out.println("  create tag response: " + addTagWrapper);
                assertThat(addTagWrapper.getInt("initiatorTypeId")).isGreaterThan(0);
                assertThat(addTagWrapper.getInt("initiatorCategoryId")).isGreaterThan(0);
                assertThat(addTagWrapper.getInt("initiatorStatusId")).isGreaterThan(0);
                assertThat(addTagWrapper.getStr("identifier")).isEqualTo(TEST_TAG);
                assertThat(addTagWrapper.getStr("accountId")).isNull();
                assertThat(addTagWrapper.getStr("createdDate")).isNotNull();

                String registerTagWrapper = rest.sendSimplePostRequest("/moz/me60/tag/link/register", Map.of(
                        "requestId", requestId,
                        "tagNumber", TEST_TAG
                ));
                System.out.println("  link register response: " + registerTagWrapper);
                assertThat(registerTagWrapper).isNotNull();

                var tagsWrapper = graphQL.call(mozGetPaymentDevices, token)
                        .print("  user tags");
                assertThat(tagsWrapper.getInt("total")).isEqualTo(1);
                assertThat(tagsWrapper.getStr("content[0].initiatorType.description")).isEqualTo(TAG);
                assertThat(tagsWrapper.getStr("content[0].initiatorCategory.category")).isEqualTo("MZ Transport");
                assertThat(tagsWrapper.getStr("content[0].identifier")).isEqualTo(TEST_TAG);
                assertThat(tagsWrapper.getStr("content[0].initiatorStatus.name")).isEqualTo("Active");
            } finally {
                rest.sendSimpleDeleteRequest("/moz/me60/tag/remove", "tag=" + TEST_TAG);
            }
        } finally {
            rest.sendSimpleDeleteRequest("/moz/me60/device/remove", "serialOrCode=" + deviceCode);
        }
    }

    @Test
    public void testLinkTag() {
        rest.sendSimplePostMultipartRequest(format("/user/%s/transactions/clear", entityId), body -> {
        });
        apiUtil.topupAccount(userWrapper.getStr("accounts[ accountType.name = Prepaid ].id"), "9999.0");
        apiUtil.topupAccount(userWrapper.getStr("accounts[ accountType.name = Subsidy ].id"), "888.0");
        String prepaidAccount = userWrapper.getStr("accounts[ accountType.name = Prepaid ].accountNumber");
        String deviceCode = apiUtil.mozRegisterPosDevice(DEVICE_SERIAL);
        try {
            rest.sendPostMultipartRequest("/moz/me60/device/activate", body -> {
                body.add("serialOrCode", deviceCode);
                body.add("accountNumber", userWrapper.getStr("accounts[ accountType.name = Primary ].accountNumber"));
            });

            var link1 = graphQL.call(format(mozLinkTagStart, DEVICE_SERIAL, prepaidAccount, "2023-02-07 12:30:10"))
                    .print("  link tag 1");
            String requestId = link1.getStr();
            assertThat(requestId).isNotBlank();

            String otp = rest.sendSimpleGetRequest("/moz/me60/tag/link/otp", "requestId=" + requestId);
            assertThat(otp).isNotBlank();

            var linkOtp = graphQL.call(format(mozLinkTagOtp, requestId, otp))
                    .print("  link tag 2 (otp)");
            assertThat(linkOtp.getDbl("prepaidBalance")).isEqualTo(9999.0);
            assertThat(linkOtp.getDbl("subsidyBalance")).isEqualTo(888.0);
            assertThat(linkOtp.getStr("firstName")).isEqualTo(FIRST_NAME);
            assertThat(linkOtp.getStr("lastName")).isEqualTo(LAST_NAME);

            var addTag = rest.sendPostMultipartRequest("/moz/me60/tag/link/clear", body -> body.add(TAG, TEST_TAG))
                    .print("  create tag response");
            try {
                assertThat(addTag.getInt("initiatorTypeId")).isGreaterThan(0);
                assertThat(addTag.getInt("initiatorCategoryId")).isGreaterThan(0);
                assertThat(addTag.getInt("initiatorStatusId")).isGreaterThan(0);
                assertThat(addTag.getStr("identifier")).isEqualTo(TEST_TAG);
                assertThat(addTag.getStr("accountId")).isNull();
                assertThat(addTag.getStr("createdDate")).isNotNull();

                var registerTag = graphQL.call(format(mozLinkTagRegister, requestId, TEST_TAG))
                        .print("  link tag 3 (register)");
                assertThat(registerTag).isNotNull();

                var tags = graphQL.call(mozGetPaymentDevices, token)
                        .print("  user tags");
                assertThat(tags.getInt("total")).isEqualTo(1);
                assertThat(tags.getStr("content[0].initiatorType.description")).isEqualTo(TAG);
                assertThat(tags.getStr("content[0].initiatorCategory.category")).isEqualTo("MZ Transport");
                assertThat(tags.getStr("content[0].identifier")).isEqualTo(TEST_TAG);
                assertThat(tags.getStr("content[0].initiatorStatus.name")).isEqualTo("Active");
            } finally {
                rest.sendSimpleDeleteRequest("/moz/me60/tag/remove", "tag=" + TEST_TAG);
            }
        } finally {
            rest.sendSimpleDeleteRequest("/moz/me60/device/remove", "serialOrCode=" + deviceCode);
        }
    }
}

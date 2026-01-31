package cash.ice.e2e.moz;

import cash.ice.ApiUtil;
import cash.ice.GraphQLHelper;
import cash.ice.RestHelper;
import cash.ice.Wrapper;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static cash.ice.GraphQlRequests.*;
import static cash.ice.sqldb.entity.InitiatorType.TAG;
import static cash.ice.sqldb.entity.TransactionCode.TSF;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MozPaymentsTests {
    private static final String DEVICE_SERIAL = "test-driver-device";
    private static final String COMMUTER_TAG = "testTag";

    private final RestHelper rest = new RestHelper();
    private final GraphQLHelper graphQL = new GraphQLHelper(rest);
    private final ApiUtil apiUtil = new ApiUtil(rest, graphQL);

    private Integer driverEntityId;
    private Integer commuterEntityId;
    private String driverToken;
    private String commuterToken;
    private Wrapper driverWrapper;
    private Wrapper commuterWrapper;
    private String deviceCode;
    private Wrapper tagWrapper;

    @BeforeAll
    public void init() {
        System.out.println("  init()");
        createDriver();
        createCommuter();
        commuterWrapper = graphQL.call(mozUserDetailsRequestStr, commuterToken)
                .print("  commuter details");
    }

    private void createDriver() {
        Wrapper user = registerUser("Test Driver", "test.driver@ice.cash", "000000000000", "1213141516", "1234");
        driverToken = apiUtil.mozLogin(user.getStr("accountNumber"), "1234");
        driverWrapper = graphQL.call(mozUserDetailsRequestStr, driverToken)
                .print("  driver details");
        driverEntityId = driverWrapper.getInt("id");

        deviceCode = apiUtil.mozRegisterPosDevice(DEVICE_SERIAL);
        rest.sendPostMultipartRequest("/moz/me60/device/activate", body -> {
            body.add("serialOrCode", deviceCode);
            body.add("accountNumber", driverWrapper.getStr("accounts[ accountType.name = Primary ].accountNumber"));
        });
    }

    private void createCommuter() {
        Wrapper user = registerUser("Test Commuter", "test.commuter@ice.cash", "000000000000", "1213141517", "1234");
        commuterToken = apiUtil.mozLogin(user.getStr("accountNumber"), "1234");
        commuterWrapper = graphQL.call(mozUserDetailsRequestStr, commuterToken)
                .print("  commuter details");
        commuterEntityId = commuterWrapper.getInt("id");

        String requestId = rest.sendSimplePostRequest("/moz/me60/tag/link", Map.of(
                "deviceSerial", DEVICE_SERIAL,
                "accountNumber", commuterWrapper.getStr("accounts[ accountType.name = Prepaid ].accountNumber"),
                "dateTime", "2023-02-07T12:30:10"
        ));
        String otp = rest.sendSimpleGetRequest("/moz/me60/tag/link/otp", "requestId=" + requestId);
        rest.sendSimplePostRequest("/moz/me60/tag/link/otp", Map.of(
                "requestId", requestId,
                "otp", otp
        ));
        tagWrapper = rest.sendPostMultipartRequest("/moz/me60/tag/link/clear", body -> body.add(TAG, COMMUTER_TAG));
        rest.sendSimplePostRequest("/moz/me60/tag/link/register", Map.of(
                "requestId", requestId,
                "tagNumber", COMMUTER_TAG
        ));
    }

    private Wrapper registerUser(String firstName, String email, String mobile, String idNumber, String pin) {
        return rest.sendPostMultipartRequest("/moz/user/register", body -> {
            body.add("firstName", firstName);
            body.add("lastName", "testUser");
            body.add("email", email);
            body.add("mobile", mobile);
            body.add("idNumber", idNumber);
            body.add("pin", pin);
            body.add("locale", "en");
        });
    }

    @AfterAll
    public void destroy() {
        System.out.println("  destroy()");
        if (tagWrapper != null) {
            rest.sendSimpleDeleteRequest("/moz/me60/tag/remove", "tag=" + COMMUTER_TAG);
        }
        if (deviceCode != null) {
            rest.sendSimpleDeleteRequest("/moz/me60/device/remove", "serialOrCode=" + deviceCode);
        }
        if (commuterEntityId != null) {
            rest.deleteMozUser(commuterEntityId);
        }
        if (driverEntityId != null) {
            rest.deleteMozUser(driverEntityId);
        }
    }

    @BeforeEach
    public void initAccounts() {
        rest.sendSimplePostMultipartRequest(format("/user/%s/transactions/clear", commuterEntityId), body -> {
        });
        rest.sendSimplePostMultipartRequest(format("/user/%s/transactions/clear", driverEntityId), body -> {
        });
        topupAccount(commuterWrapper.getStr("accounts[ accountType.name = Prepaid ].id"), "10000.0");
        topupAccount(commuterWrapper.getStr("accounts[ accountType.name = Subsidy ].id"), "1000.0");
    }

    private void topupAccount(String accountId, String amount) {
        rest.sendSimplePostMultipartRequest("/moz/account/topup", body -> {
            body.add("account", accountId);
            body.add("amount", amount);
            body.add("reference", "Test deposit");
        });
    }

    @Test
    public void testPaymentOld() {
        String prepaidNumber = commuterWrapper.getStr("accounts[ accountType.name = Prepaid ].accountNumber");
        String subsidyNumber = commuterWrapper.getStr("accounts[ accountType.name = Subsidy ].accountNumber");

        String vendorRef = UUID.randomUUID().toString();
        Map<String, Object> requestBody = Map.of(
                "vendorRef", vendorRef,
                "tx", TSF,
                "initiatorType", TAG,
                "initiator", COMMUTER_TAG,
                "currency", "MZN",
                "amount", 10,
                "apiVersion", "1",
                "date", "2021-09-08T12:30",
                "meta", Map.of(
                        "deviceCode", deviceCode,
                        "requestBalanceAccountTypes", List.of()
                )
        );
        rest.sendPostRequest("/moz/me60/payment", requestBody);
        var payment = rest.sendPostRequest("/moz/me60/payment", requestBody)
                .print("  payment response");
        assertThat(payment.getStr("status")).isEqualTo("SUCCESS");
        assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment.getStr("transactionId")).isNotNull();
        assertThat(payment.getStr("date")).isNotNull();
        assertThat(payment.getDbl("balance")).isEqualTo(9992.0);
        assertThat(payment.getDbl("subsidyBalance")).isEqualTo(998.0);
        assertThat(payment.getDbl("accountBalances.{prepaidNumber}.balance", prepaidNumber)).isEqualTo(9992.0);
        assertThat(payment.getDbl("accountBalances.{subsidyNumber}.balance", subsidyNumber)).isEqualTo(998.0);

        var commuter = graphQL.call(mozUserDetailsRequestStr, commuterToken)
                .print("  commuter details");
        assertThat(commuter.getDbl("accounts[ accountType.name = Prepaid ].balance")).isEqualTo(9992.0);
        assertThat(commuter.getDbl("accounts[ accountType.name = Subsidy ].balance")).isEqualTo(998.0);

        var driver = graphQL.call(mozUserDetailsRequestStr, driverToken)
                .print("  driver details");
        assertThat(driver.getDbl("accounts[ accountType.name = Primary ].balance")).isEqualTo(10.0);

        checkStatement(vendorRef);
    }

    @Test
    public void testPayment() {
        String vendorRef = UUID.randomUUID().toString();
        var payment = graphQL.call(format(mozMakePayment,
                        vendorRef,
                        TSF,
                        TAG,
                        COMMUTER_TAG,
                        "MZN",
                        10,
                        "2023-02-07 12:30:10",
                        deviceCode))
                .print("  payment response");
        assertThat(payment.getStr("status")).isEqualTo("SUCCESS");
        assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment.getStr("transactionId")).isNotNull();
        assertThat(payment.getStr("date")).isNotNull();
        assertThat(payment.getDbl("balance")).isEqualTo(9992.0);

        var commuter = graphQL.call(mozUserDetailsRequestStr, commuterToken)
                .print("  commuter details");
        assertThat(commuter.getDbl("accounts[ accountType.name = Prepaid ].balance")).isEqualTo(9992.0);
        assertThat(commuter.getDbl("accounts[ accountType.name = Subsidy ].balance")).isEqualTo(998.0);

        var driver = graphQL.call(mozUserDetailsRequestStr, driverToken)
                .print("  driver details");
        assertThat(driver.getDbl("accounts[ accountType.name = Primary ].balance")).isEqualTo(10.0);

        checkStatement(vendorRef);
    }

    private void checkStatement(String vendorRef) {
        var prepaidStatement = graphQL.call(format(mozGetStatement, "Prepaid", "MZN"), commuterToken)
                .print("  commuter Prepaid statement response");
        assertThat(prepaidStatement.getInt("total")).isEqualTo(2);
        assertThat(prepaidStatement.getStr("content[ 0 ].transactionCode.code")).isEqualTo("DEP");
        assertThat(prepaidStatement.getStr("content[ 0 ].statementDate")).isNotBlank();
        assertThat(prepaidStatement.getDbl("content[ 0 ].amount")).isEqualTo(10000.0);
        assertThat(prepaidStatement.getListSize("content[ 0 ].lines")).isEqualTo(1);
        assertThat(prepaidStatement.getStr("content[ 0 ].lines[ 0 ].transactionCode.code")).isEqualTo("DEP");
        assertThat(prepaidStatement.getStr("content[ 0 ].lines[ 0 ].description")).isEqualTo("Test deposit received for Test Commuter testUser");
        assertThat(prepaidStatement.getDbl("content[ 0 ].lines[ 0 ].amount")).isEqualTo(10000.0);
        assertThat(prepaidStatement.getStr("content[ 1 ].transactionCode.code")).isEqualTo(TSF);
        assertThat(prepaidStatement.getStr("content[ 1 ].statementDate")).isNotBlank();
        assertThat(prepaidStatement.getDbl("content[ 1 ].amount")).isEqualTo(-8.0);
        assertThat(prepaidStatement.getListSize("content[ 1 ].lines")).isEqualTo(1);
        assertThat(prepaidStatement.getStr("content[ 1 ].lines[ 0 ].transactionCode.code")).isEqualTo(TSF);
        assertThat(prepaidStatement.getStr("content[ 1 ].lines[ 0 ].description")).isEqualTo(format("MZ NFC payment done by Test Commuter testUser ref(%s)", vendorRef));
        assertThat(prepaidStatement.getDbl("content[ 1 ].lines[ 0 ].amount")).isEqualTo(-8.0);

        var subsidyStatement = graphQL.call(format(mozGetStatement, "Subsidy", "MZN"), commuterToken)
                .print("  commuter Subsidy statement response");
        assertThat(subsidyStatement.getInt("total")).isEqualTo(2);
        assertThat(subsidyStatement.getStr("content[ 0 ].transactionCode.code")).isEqualTo("DEP");
        assertThat(subsidyStatement.getStr("content[ 0 ].statementDate")).isNotBlank();
        assertThat(subsidyStatement.getDbl("content[ 0 ].amount")).isEqualTo(1000.0);
        assertThat(subsidyStatement.getListSize("content[ 0 ].lines")).isEqualTo(1);
        assertThat(subsidyStatement.getStr("content[ 0 ].lines[ 0 ].transactionCode.code")).isEqualTo("DEP");
        assertThat(subsidyStatement.getStr("content[ 0 ].lines[ 0 ].description")).isEqualTo("Test deposit received for Test Commuter testUser");
        assertThat(subsidyStatement.getDbl("content[ 0 ].lines[ 0 ].amount")).isEqualTo(1000.0);
        assertThat(subsidyStatement.getStr("content[ 1 ].transactionCode.code")).isEqualTo(TSF);
        assertThat(subsidyStatement.getStr("content[ 1 ].statementDate")).isNotBlank();
        assertThat(subsidyStatement.getDbl("content[ 1 ].amount")).isEqualTo(-2.0);
        assertThat(subsidyStatement.getListSize("content[ 1 ].lines")).isEqualTo(1);
        assertThat(subsidyStatement.getStr("content[ 1 ].lines[ 0 ].transactionCode.code")).isEqualTo(TSF);
        assertThat(subsidyStatement.getStr("content[ 1 ].lines[ 0 ].description")).isEqualTo(format("MZ NFC payment done by Test Commuter testUser ref(%s)", vendorRef));
        assertThat(subsidyStatement.getDbl("content[ 1 ].lines[ 0 ].amount")).isEqualTo(-2.0);

        var driverStatement = graphQL.call(format(mozGetStatement, "Primary", "MZN"), driverToken)
                .print("  driver Primary statement response");
        assertThat(driverStatement.getInt("total")).isEqualTo(1);
        assertThat(driverStatement.getStr("content[ 0 ].transactionCode.code")).isEqualTo(TSF);
        assertThat(driverStatement.getStr("content[ 0 ].statementDate")).isNotBlank();
        assertThat(driverStatement.getDbl("content[ 0 ].amount")).isEqualTo(10.0);
        assertThat(driverStatement.getListSize("content[ 0 ].lines")).isEqualTo(2);
        assertThat(driverStatement.getStr("content[ 0 ].lines[ 0 ].transactionCode.code")).isEqualTo(TSF);
        assertThat(driverStatement.getStr("content[ 0 ].lines[ 0 ].description")).isEqualTo(format("MZ NFC payment received for Test Driver testUser ref(%s)", vendorRef));
        assertThat(driverStatement.getDbl("content[ 0 ].lines[ 0 ].amount")).isEqualTo(8.0);
        assertThat(driverStatement.getStr("content[ 0 ].lines[ 1 ].transactionCode.code")).isEqualTo(TSF);
        assertThat(driverStatement.getStr("content[ 0 ].lines[ 1 ].description")).isEqualTo(format("MZ NFC payment received for Test Driver testUser ref(%s)", vendorRef));
        assertThat(driverStatement.getDbl("content[ 0 ].lines[ 1 ].amount")).isEqualTo(2.0);
    }

    @Test
    public void testOffloadPayment() {
        var payment = rest.sendPostRequest("/moz/me60/payment/offload", Map.of(
                        TAG, COMMUTER_TAG,
                        "offloadTransactions", List.of(
                                deviceCode + "1680556821" + 1500
                        )))
                .print("  payment response");
        assertThat(payment.getStr("status")).isEqualTo("SUCCESS");
        assertThat(payment.getStr("date")).isNotNull();
        assertThat(payment.getDbl("balance")).isEqualTo(9988.0);
        assertThat(payment.getDbl("subsidyBalance")).isEqualTo(997.0);

        var commuter = graphQL.call(mozUserDetailsRequestStr, commuterToken)
                .print("  commuter details");
        assertThat(commuter.getDbl("accounts[ accountType.name = Prepaid ].balance")).isEqualTo(9988.0);
        assertThat(commuter.getDbl("accounts[ accountType.name = Subsidy ].balance")).isEqualTo(997.0);

        var driver = graphQL.call(mozUserDetailsRequestStr, driverToken)
                .print("  driver details");
        assertThat(driver.getDbl("accounts[ accountType.name = Primary ].balance")).isEqualTo(15.0);
    }
}

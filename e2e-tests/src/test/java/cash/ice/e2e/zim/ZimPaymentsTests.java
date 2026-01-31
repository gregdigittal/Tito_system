package cash.ice.e2e.zim;

import cash.ice.ApiUtil;
import cash.ice.GraphQLHelper;
import cash.ice.RestHelper;
import cash.ice.Wrapper;
import cash.ice.common.utils.Tool;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.UUID;

import static cash.ice.GraphQlRequests.*;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ZimPaymentsTests {
    public static final String FIRST_NAME = "testUser";
    public static final String LAST_NAME = "e2eTest";

    private final RestHelper rest = new RestHelper();
    private final GraphQLHelper graphQL = new GraphQLHelper(rest);
    private final ApiUtil apiUtil = new ApiUtil(rest, graphQL);

    private Integer entityId;
    private String entityToken;
    private Integer accountId;
    private String accountNumber;
    private String usdAccountNumber;
    private String mznAccountNumber;

    @BeforeAll
    public void init() {
        System.out.println("  init()");
        createEntity();
    }

    private void createEntity() {
        var entity = graphQL.call(format(simpleRegisterEntity, "Personal", FIRST_NAME, LAST_NAME, 1, "1213141541", "000000000000", "test.e2e@ice.cash"))
                .print("  new entity");
        entityId = entity.getInt("id");
        accountId = entity.getInt("accounts[0].id");
        accountNumber = entity.getStr("accounts[0].accountNumber");
        usdAccountNumber = createAccount(entityId, "Primary", "USD");
        mznAccountNumber = createAccount(entityId, "Primary", "MZN");
        rest.sendSimplePostMultipartRequest(format("/user/%s/kyc/%s", entityId, "FULL"), body -> {
        });
        String pin = rest.sendSimpleGetRequest("/users/pin/entity", "username=" + accountNumber);
        System.out.println("  PIN: " + pin);
        entityToken = apiUtil.mozLogin(accountNumber, pin);
    }

    @AfterAll
    public void destroy() {
        System.out.println("  destroy()");
        if (entityId != null) {
            rest.deleteMozUser(entityId);
        }
    }

    @BeforeEach
    public void initAccounts() {
        rest.sendSimplePostMultipartRequest(format("/user/%s/transactions/clear", entityId), body -> {
        });
    }

    @Test
    public void testPaygoPayment() {
        BigDecimal initialDrBalance = getBalance("ICEcash Legacy System", "Online Suspense", "ZWL");        // todo "Commission Accrued Wallet"
        System.out.println("  initial dr balance: " + initialDrBalance);

        String vendorRef = UUID.randomUUID().toString();
        var payment = graphQL.call(format(makePaygoPayment, vendorRef, "PGCBZ", "paygo", "770000000", "ZWL", 1.00,
                        accountId, "2023-02-07 12:30:10", "Insurance policy #993819"))
                .print("  payment response");
        checkPaymentIsPROCESSING(payment, vendorRef);

        Wrapper response = payment;
        long waitingTime = 0;
        boolean hasPayload = false;
        while (!hasPayload || waitingTime > 60_000) {
            Tool.sleep(200);
            waitingTime += 200;
            response = graphQL.call(format(paymentResponse, vendorRef))
                    .print("  payment response");
            hasPayload = response.getObject("payload") != null;
        }

        assertThat(response.getStr("status")).isEqualTo("PROCESSING");
        assertThat(response.getStr("payload.qr64")).isEqualTo("data:image:simulatedImage");
        assertThat(response.getStr("payload.payGoId")).isNotBlank();
        assertThat(response.getStr("payload.deviceReference")).isNotBlank();

        String payGoId = response.getStr("payload.payGoId");
        rest.sendSimplePostMultipartRequestFullUrl(RestHelper.paygoHost + "/api/paygo/simulate", body -> {
            body.add("type", "SUCCESS");
            body.add("payGoId", payGoId);
            body.add("currencyCode", "ZWL");
        });

        payment = pollForPaymentResponse(payment, vendorRef, 200, 60_000);
        checkPaymentIsSUCCESS(payment, vendorRef, null);                                                 // todo

        checkBalance(entityToken, accountNumber, 1.00);
//        checkTransactions(entityToken, "Primary", "ZWL", "PGCBZ", 1.0,
//                String.format("PayGo CBZ Merchant Account received for %s %s ref(%s)", FIRST_NAME, LAST_NAME, vendorRef));       // todo
        BigDecimal updatedDrBalance = getBalance("ICEcash Legacy System", "Online Suspense", "ZWL");
        System.out.println("  updated dr balance: " + updatedDrBalance);
        assertThat(updatedDrBalance).isEqualTo(initialDrBalance.subtract(BigDecimal.ONE));
    }

    @Test
    public void testPaymentFrozenAccount() {
        var updateAccount = graphQL.call(format(updateAccountActive, accountId, false), entityToken)
                .print("  update account active");
        assertThat(updateAccount.getStr("accountStatus")).isEqualTo("FROZEN");
        try {
            BigDecimal initialDrBalance = getBalance("EcoCash Pool Account General", "Online Suspense", "ZWL");
            System.out.println("  initial dr balance: " + initialDrBalance);
            String vendorRef = UUID.randomUUID().toString();

            var payment = graphQL.call(format(makeInboundPayment, vendorRef, "EPAYG", "ecocash", "770000000",
                            "ZWL", 1.00, "2023-02-07 12:30:10", accountNumber))
                    .print("  payment response");
            checkPaymentIsPROCESSING(payment, vendorRef);
            payment = pollForPaymentResponse(payment, vendorRef, 200, 60_000);
            assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
            assertThat(payment.getStr("status")).isEqualTo("ERROR");
            assertThat(payment.getStr("errorCode")).isEqualTo("103-IC1142-3016");
            assertThat(payment.getStr("message")).isNotBlank();
            assertThat(payment.getStr("date")).isNotNull();

            checkBalance(entityToken, accountNumber, 0.00);
            var statement = graphQL.call(format(getAccountStatement, "Primary", "ZWL"), entityToken)
                    .print("  Primary statement response");
            assertThat(statement.getInt("total")).isEqualTo(0);
            assertThat(statement.getObjectList("content").size()).isEqualTo(0);

            BigDecimal drBalance = getBalance("EcoCash Pool Account General", "Online Suspense", "ZWL");
            System.out.println("  updated dr balance: " + drBalance);
            assertThat(drBalance).isEqualTo(initialDrBalance);
        } finally {
            updateAccount = graphQL.call(format(updateAccountActive, accountId, true), entityToken)
                    .print("  update account active");
            assertThat(updateAccount.getStr("accountStatus")).isEqualTo("ACTIVE");
        }
    }

    @Test
    public void testEcocashEpaygPayment() {
        BigDecimal initialDrBalance = getBalance("EcoCash Pool Account General", "Online Suspense", "ZWL");
        System.out.println("  initial dr balance: " + initialDrBalance);
        String vendorRef = UUID.randomUUID().toString();

        var payment = graphQL.call(format(makeInboundPayment, vendorRef, "EPAYG", "ecocash", "770000000",
                        "ZWL", 1.00, "2023-02-07 12:30:10", accountNumber))
                .print("  payment response");
        checkPaymentIsPROCESSING(payment, vendorRef);
        payment = pollForPaymentResponse(payment, vendorRef, 200, 60_000);
        checkPaymentIsSUCCESS(payment, vendorRef, null);                                                 // todo

        checkBalance(entityToken, accountNumber, 1.00);
        checkTransactions(entityToken, "Primary", "ZWL", "EPAYG", 1.0,
                format("Ecocash payment general received for %s %s ref(%s)", FIRST_NAME, LAST_NAME, vendorRef));
        BigDecimal updatedDrBalance = getBalance("EcoCash Pool Account General", "Online Suspense", "ZWL");
        System.out.println("  updated dr balance: " + updatedDrBalance);
        assertThat(updatedDrBalance).isEqualTo(initialDrBalance.subtract(BigDecimal.ONE));
    }

    @Test
    public void testEcocashZtpPayment() {
        BigDecimal initialDrBalance = getBalance("EcoCash Pool Account Zinara", "Online Suspense", "ZWL");
        System.out.println("  initial dr balance: " + initialDrBalance);
        BigDecimal initialCrBalance = getBalance("Ecocash", "Primary", "ZWL");
        System.out.println("  initial cr balance: " + initialCrBalance);
        String vendorRef = UUID.randomUUID().toString();

        var payment = graphQL.call(format(makeInboundPayment, vendorRef, "ZTP", "ecocash", "770000000", "ZWL", 1.00,
                        "2023-02-07 12:30:10", accountNumber))
                .print("  payment response");
        checkPaymentIsPROCESSING(payment, vendorRef);
        payment = pollForPaymentResponse(payment, vendorRef, 200, 60_000);
        checkPaymentIsSUCCESS(payment, vendorRef, null);                                                 // todo

        BigDecimal updatedDrBalance = getBalance("EcoCash Pool Account Zinara", "Online Suspense", "ZWL");
        System.out.println("  updated dr balance: " + updatedDrBalance);
        assertThat(updatedDrBalance).isEqualTo(initialDrBalance.subtract(BigDecimal.ONE));
        BigDecimal updatedCrBalance = getBalance("Ecocash", "Primary", "ZWL");
        System.out.println("  updated cr balance: " + updatedCrBalance);
        assertThat(updatedCrBalance).isEqualTo(initialCrBalance.add(BigDecimal.ONE));
    }

    @Test
    public void testEcocashEusdPayment() {
        BigDecimal initialDrBalance = getBalance("EcoCash Pool Account USD", "USD FCA", "USD");
        System.out.println("  initial dr balance: " + initialDrBalance);
        String vendorRef = UUID.randomUUID().toString();

        var payment = graphQL.call(format(makeInboundPayment, vendorRef, "EUSD", "ecocash", "770000000", "USD", 1.00,
                        "2023-02-07 12:30:10", usdAccountNumber))
                .print("  payment response");
        checkPaymentIsPROCESSING(payment, vendorRef);
        payment = pollForPaymentResponse(payment, vendorRef, 200, 60_000);
        checkPaymentIsSUCCESS(payment, vendorRef, null);                                                 // todo

        checkBalance(entityToken, usdAccountNumber, 1.00);
        checkTransactions(entityToken, "Primary", "USD", "EUSD", 1.0,
                format("EUSD received for %s %s ref(%s)", FIRST_NAME, LAST_NAME, vendorRef));
        BigDecimal updatedDrBalance = getBalance("EcoCash Pool Account USD", "USD FCA", "USD");
        System.out.println("  updated dr balance: " + updatedDrBalance);
        assertEquals(0, updatedDrBalance.compareTo(initialDrBalance.subtract(BigDecimal.ONE)));
    }

    @Test
    public void testOnemoneyPayment() {
        BigDecimal initialDrBalance = getBalance("OneMoney Pool Account", "Online Suspense", "ZWL");
        System.out.println("  initial dr balance: " + initialDrBalance);
        String vendorRef = UUID.randomUUID().toString();

        var payment = graphQL.call(format(makeInboundPayment, vendorRef, "OPAYG", "netone", "0710000000", "ZWL", 1.00,
                        "2023-02-07 12:30:10", accountNumber))
                .print("  payment response");
        checkPaymentIsPROCESSING(payment, vendorRef);
        payment = pollForPaymentResponse(payment, vendorRef, 1000, 60_000);
        checkPaymentIsSUCCESS(payment, vendorRef, null);                                                 // todo

        checkBalance(entityToken, accountNumber, 1.00);
        checkTransactions(entityToken, "Primary", "ZWL", "OPAYG", 1.0,
                format("OneMoney payment general received for %s %s ref(%s)", FIRST_NAME, LAST_NAME, vendorRef));
        BigDecimal updatedDrBalance = getBalance("OneMoney Pool Account", "Online Suspense", "ZWL");
        System.out.println("  updated dr balance: " + updatedDrBalance);
        assertThat(updatedDrBalance).isEqualTo(initialDrBalance.subtract(BigDecimal.ONE));
    }

    @Test
    public void testFlexcubePayment() {
        apiUtil.topupAccount(valueOf(accountId), "100.0");
        BigDecimal initialDrBalance = getBalance("RTGS Control Account", "Primary", "ZWL");
        System.out.println("  initial cr balance: " + initialDrBalance);
        String vendorRef = UUID.randomUUID().toString();

        var payment = graphQL.call(format(makeFlexcubePayment,
                        vendorRef, "TRN", "icecash", accountId, "ZWL", 1.00, "2023-02-07 12:30:10",
                        1584240, "588882", "10099209810", "001", "COBZZWH0", "Gordon Gangata", "129 Main Street, Harare", "Test RTGS transaction", "pay test"))
                .print("  payment response");
        checkPaymentIsPROCESSING(payment, vendorRef);
        payment = pollForPaymentResponse(payment, vendorRef, 200, 60_000);
        checkPaymentIsSUCCESS(payment, vendorRef, 99.0);

        checkBalance(entityToken, accountNumber, 99.00);
        checkTransactions(entityToken, "Primary", "ZWL", "TRN", -1.0,
                format("Flexcube payment done by %s %s ref(%s)", FIRST_NAME, LAST_NAME, vendorRef));
        BigDecimal updatedDrBalance = getBalance("RTGS Control Account", "Primary", "ZWL");
        System.out.println("  updated dr balance: " + updatedDrBalance);
        assertThat(updatedDrBalance).isEqualTo(initialDrBalance.add(BigDecimal.ONE));
    }

    @Test
    public void testMpesaMpiPayment() {
        BigDecimal initialDrBalance = getBalance("ICEcash MPESA Pool account", "Primary", "MZN");
        System.out.println("  initial dr balance: " + initialDrBalance);
        String vendorRef = UUID.randomUUID().toString();

        var payment = graphQL.call(format(makeMozInboundOutboundPayment, vendorRef, "MPI", "mpesa", "770000000", "MZN", 1.00,
                        "2023-02-07 12:30:10", mznAccountNumber, "SUCCESS"))
                .print("  payment response");
        checkPaymentIsPROCESSING(payment, vendorRef);
        payment = pollForPaymentResponse(payment, vendorRef, 200, 60_000);
        checkPaymentIsSUCCESS(payment, vendorRef, null);                                                 // todo

        checkBalance(entityToken, mznAccountNumber, 1.00);
        checkTransactions(entityToken, "Primary", "MZN", "MPI", 1.0,
                format("MPESA inbound payments received for %s %s ref(%s)", FIRST_NAME, LAST_NAME, vendorRef));
        BigDecimal updatedDrBalance = getBalance("ICEcash MPESA Pool account", "Primary", "MZN");
        System.out.println("  updated dr balance: " + updatedDrBalance);
        assertThat(updatedDrBalance).isEqualTo(initialDrBalance.subtract(BigDecimal.ONE));
    }

    @Test
    public void testMpesaMpoPayment() {
        BigDecimal initialDrBalance = getBalance("ICEcash MPESA Pool account", "Primary", "MZN");
        System.out.println("  initial dr balance: " + initialDrBalance);
        String vendorRef = UUID.randomUUID().toString();

        var payment = graphQL.call(format(makeMozInboundOutboundPayment, vendorRef, "MPO", "mpesa", "770000000", "MZN", 1.00,
                        "2023-02-07 12:30:10", mznAccountNumber, "SUCCESS"))
                .print("  payment response");
        checkPaymentIsPROCESSING(payment, vendorRef);
        payment = pollForPaymentResponse(payment, vendorRef, 200, 60_000);
        checkPaymentIsSUCCESS(payment, vendorRef, null);                                                 // todo

        checkBalance(entityToken, mznAccountNumber, -1.00);
        checkTransactions(entityToken, "Primary", "MZN", "MPO", -1.0,
                format("MPESA outbound payments done by %s %s ref(%s)", FIRST_NAME, LAST_NAME, vendorRef));
        BigDecimal updatedDrBalance = getBalance("ICEcash MPESA Pool account", "Primary", "MZN");
        System.out.println("  updated dr balance: " + updatedDrBalance);
        assertThat(updatedDrBalance).isEqualTo(initialDrBalance.add(BigDecimal.ONE));
    }

    private Wrapper pollForPaymentResponse(Wrapper initialResponse, String vendorRef, long delayBetweenRequestsMs, long maxWaitingTimeMs) {
        Wrapper response = initialResponse;
        long waitingTime = 0;
        while ("PROCESSING".equals(response.getStr("status")) || waitingTime > maxWaitingTimeMs) {
            Tool.sleep(delayBetweenRequestsMs);
            waitingTime += delayBetweenRequestsMs;
            response = graphQL.call(format(paymentResponse, vendorRef))
                    .print("  payment response");
        }
        return response;
    }

    private BigDecimal getBalance(String entityName, String accountTypeName, String currencyCode) {
        String balance = rest.sendSimplePostMultipartRequest("/user/account/balance", body -> {
            body.add("entityName", entityName);
            body.add("accountTypeName", accountTypeName);
            body.add("currencyCode", currencyCode);
        });
        return new BigDecimal(balance);
    }

    private String createAccount(Integer entityId, String accountTypeName, String currencyCode) {
        return rest.sendSimplePostMultipartRequest("/user/account/add", body -> {
            body.add("entityId", entityId);
            body.add("accountTypeName", accountTypeName);
            body.add("currencyCode", currencyCode);
        });
    }

    private void checkPaymentIsPROCESSING(Wrapper paymentWrapper, String vendorRef) {
        assertThat(paymentWrapper.getStr("status")).isEqualTo("PROCESSING");
        assertThat(paymentWrapper.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentWrapper.getStr("message")).isEqualTo("Operation is in progress");
        assertThat(paymentWrapper.getStr("errorCode")).isNull();
    }

    private void checkPaymentIsSUCCESS(Wrapper paymentWrapper, String vendorRef, Double expectedBalance) {
        assertThat(paymentWrapper.getStr("status")).isEqualTo("SUCCESS");
        assertThat(paymentWrapper.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentWrapper.getStr("transactionId")).isNotNull();
        assertThat(paymentWrapper.getStr("date")).isNotNull();
        if (expectedBalance != null) {
            assertThat(paymentWrapper.getDbl("balance")).isEqualTo(expectedBalance);
        }
    }

    private void checkBalance(String token, String accountNumber, Double expectedBalance) {
        var user = graphQL.call(mozUserDetailsRequestStr, token)
                .print("  user details");
        assertThat(user.getDbl("accounts[ accountNumber = {accountNumber} ].balance", accountNumber)).isEqualTo(expectedBalance);
    }

    private void checkTransactions(String token, String accountType, String currencyCode, String transactionCode, double amount, String description) {
        var statement = graphQL.call(format(getAccountStatement, accountType, currencyCode), token)
                .print(format("  %s statement response", accountType));
        assertThat(statement.getInt("total")).isGreaterThan(0);
        assertThat(statement.getStr("content[ last ].transactionCode.code")).isEqualTo(transactionCode);
        assertThat(statement.getDbl("content[ last ].amount")).isEqualTo(amount);
        assertThat(statement.getStr("content[ last ].statementDate")).isNotBlank();
        assertThat(statement.getListSize("content[ last ].lines")).isEqualTo(1);
        assertThat(statement.getStr("content[ last ].lines[ 0 ].transactionCode.code")).isEqualTo(transactionCode);
        assertThat(statement.getStr("content[ last ].lines[ 0 ].description")).isEqualTo(description);
        assertThat(statement.getDbl("content[ last ].lines[ 0 ].amount")).isEqualTo(amount);
    }
}

package cash.ice.e2e.moz;

import cash.ice.ApiUtil;
import cash.ice.GraphQLHelper;
import cash.ice.RestHelper;
import cash.ice.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.UUID;

import static cash.ice.GraphQlRequests.*;
import static cash.ice.sqldb.entity.Currency.MZN;
import static cash.ice.sqldb.entity.InitiatorType.TAG;
import static cash.ice.sqldb.entity.TransactionCode.TSF;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TransportTests {
    private static final String MOBILE = "000000000000";
    private static final String PASSWORD = "1234";
    private static final String DEVICE_SERIAL = "etest-device-serial";
    private static final String TEST_TAG = "testTag";
    private static final String ROUTE_NAME = "BAIXA-BOANE";

    private final RestHelper rest = new RestHelper();
    private final GraphQLHelper graphQL = new GraphQLHelper(rest);
    private final ApiUtil apiUtil = new ApiUtil(rest, graphQL);

    private Integer agentEntityId;
    private Integer ownerEntityId;
    private Integer driverEntityId;
    private Integer collectorEntityId;
    private Integer commuterEntityId;
    private Integer commuterSubsidyAccountId;
    private Integer commuterPrepaidAccountId;
    private String agentToken;
    private String ownerToken;
    private String commuterToken;
    private Integer vehicleId;
    private Wrapper routeWrapper;
    private Wrapper posWrapper;
    private Wrapper tagWrapper;
    private Wrapper ownerWrapper;
    private String posDeviceCode;

    @Test
    public void testTransportSolution() {
        try {
            Wrapper agentWrapper = makeFematroAgent();
            String agentAccountNumber = agentWrapper.getStr("accounts[0].accountNumber");
            agentToken = apiUtil.mozLogin(agentAccountNumber, PASSWORD);

            Wrapper ownerWrapper = makeOwner();
            String ownerAccountNumber = ownerWrapper.getStr("accounts[0].accountNumber");
            ownerToken = apiUtil.mozLogin(ownerAccountNumber, PASSWORD);

            Wrapper driverWrapper = makeDriver();
            String driverAccountNumber = driverWrapper.getStr("accounts[0].accountNumber");
            String driverToken = apiUtil.mozLogin(driverAccountNumber, PASSWORD);

            Wrapper collectorWrapper = makeCollector();
            String collectorAccountNumber = collectorWrapper.getStr("accounts[0].accountNumber");
            String collectorToken = apiUtil.mozLogin(collectorAccountNumber, PASSWORD);

            Wrapper commuterWrapper = makeCommuter();
            String commuterAccountNumber = commuterWrapper.getStr("accounts[0].accountNumber");
            commuterToken = apiUtil.mozLogin(commuterAccountNumber, PASSWORD);

            Wrapper vehicleWrapper = makeVehicle(ownerToken, ownerAccountNumber);

            posWrapper = makePosDevice();
            tagWrapper = makeTag(commuterWrapper);

            String vendorRef = UUID.randomUUID().toString();
            makePayments(vendorRef);

        } finally {
            destroyResourcesIfNeed();
        }
    }

    private String sendOtpMobile(String otpType) {
        var otpW = graphQL.call(format(sendOtpMobile, otpType, MOBILE, "false"))
                .print("  otp response(mobile)");
        assertThat(otpW.getStr()).isEqualTo("SUCCESS");
        String otp = rest.sendSimpleGetRequest("/moz/otp", format("otpType=%s&msisdn=%s", otpType, MOBILE));
        System.out.println("  otp: " + otp);
        assertThat(otp).isNotBlank();
        return otp;
    }

    private String sendOtpEntity(String otpType, int entityId) {
        var otpW = graphQL.call(format(sendOtpEntity, otpType, entityId))
                .print("  otp response(entity)");
        assertThat(otpW.getStr()).isEqualTo("SUCCESS");
        String otp = rest.sendSimpleGetRequest("/moz/otp", format("otpType=%s&entityId=%s", otpType, entityId));
        System.out.println("  otp: " + otp);
        assertThat(otp).isNotBlank();
        return otp;
    }

    private String sendOtpAccount(String otpType, String accountNumber) {
        var otpW = graphQL.call(format(sendOtpAccount, otpType, accountNumber))
                .print("  otp response(account)");
        assertThat(otpW.getStr()).isEqualTo("SUCCESS");
        String otp = rest.sendSimpleGetRequest("/moz/otp", format("otpType=%s&accountNumber=%s", otpType, accountNumber));
        System.out.println("  otp: " + otp);
        assertThat(otp).isNotBlank();
        return otp;
    }

    private Wrapper makeFematroAgent() {
        var agreement = graphQL.call(format(mozGetRegAgreement, "en", "AgentFematro"))
                .print("  agreement for AgentFematro");
        assertThat(agreement.getStr("value")).isNotNull();

        String agentRegOtp = sendOtpMobile("MOZ_REG_USER");
        var agent = graphQL.call(format(mozRegIndividualUser, agentRegOtp, "AgentFematro", "Test agent", "Test agent fematro",
                        "ID", "1213141515", null, "12341235", null, MOBILE, "test.agent.user@ice.cash", PASSWORD, "en"))
                .print("  agent");
        agentEntityId = agent.getInt("id");
        assertThat(agent.getInt("id")).isGreaterThan(0);
        assertThat(agent.getStr("firstName")).isEqualTo("Test agent");
        assertThat(agent.getStr("lastName")).isEqualTo("Test agent fematro");
        assertThat(agent.getStr("email")).isEqualTo("test.agent.user@ice.cash");
        assertThat(agent.getStr("msisdn[0].msisdn")).isEqualTo(MOBILE);
        assertThat(agent.getStr("idNumber")).isEqualTo("1213141515");
        assertThat(agent.getInt("idTypeId")).isGreaterThan(0);
        assertThat(agent.getInt("entityTypeId")).isGreaterThan(0);
        assertThat(agent.getStr("entityType.description")).isEqualTo("Private");
        assertThat(agent.getListSize("accounts")).isEqualTo(3);
        assertThat(agent.getStr("accounts[ 0 ].accountNumber")).isNotEmpty();

        assertThat(agent.getListSize("relationships")).isEqualTo(3);
        assertThat(agent.getListSize("relationships[ last ].securityGroupMoz")).isEqualTo(2);
        assertThat(agent.getInt("relationships[ last ].securityGroupMoz[ 0 ].id")).isEqualTo(1012);      // Moz Fematro Agent
        assertThat(agent.toBool("relationships[ last ].securityGroupMoz[ 0 ].active")).isTrue();
        assertThat(agent.getStrList("relationships[ last ].securityGroupMoz[ 0 ].rightsList")).isEqualTo(
                List.of("MOZ_TOPUP", "MOZ_LINK_TAG", "MOZ_KYC_UPLOAD", "MOZ_REG_USER", "MOZ_LINK_POS"));
        assertThat(agent.getInt("relationships[ last ].securityGroupMoz[ 1 ].id")).isEqualTo(1013);      // Moz Commuter
        assertThat(agent.toBool("relationships[ last ].securityGroupMoz[ 1 ].active")).isTrue();
        assertThat(agent.getStrList("relationships[ last ].securityGroupMoz[ 1 ].rightsList")).isEqualTo(
                List.of("MOZ_TOPUP", "MOZ_PROFILE", "MOZ_TAG_MANAGEMENT", "MOZ_STATEMENT"));
        return agent;
    }

    private Wrapper makeOwner() {
        var agreement = graphQL.call(format(mozGetRegAgreement, "en", "TransportOwnerPrivate"))
                .print("  agreement for TransportOwnerPrivate");
        assertThat(agreement.getStr("value")).isNotNull();

        String ownerRegOtp = sendOtpMobile("MOZ_REG_USER");
        ownerWrapper = graphQL.call(format(mozRegCorporateUser, ownerRegOtp,
                        "testCompany", "testNuel", null, "testNuit", null, MOBILE, "test.company@ice.cash", 205, "Maputo", "12345",
                        "testAddress1", "testAddress2", "some notes", "TransportOwnerPrivate", "Test owner", "Test private owner",
                        "DIRE", "1213141517", null, "12341236", null, MOBILE, "test.owner.user@ice.cash", PASSWORD, "en"), agentToken)
                .print("  owner");
        ownerEntityId = ownerWrapper.getInt("id");
        assertThat(ownerWrapper.getInt("id")).isGreaterThan(0);
        assertThat(ownerWrapper.getStr("firstName")).isEqualTo("testCompany");
        assertThat(ownerWrapper.getStr("lastName")).isEqualTo(null);
        assertThat(ownerWrapper.getStr("email")).isEqualTo("test.company@ice.cash");
        assertThat(ownerWrapper.getStr("msisdn[0].msisdn")).isEqualTo(MOBILE);
        assertThat(ownerWrapper.getStr("idNumber")).isEqualTo("testNuel");
        assertThat(ownerWrapper.getInt("idTypeId")).isGreaterThan(0);
        assertThat(ownerWrapper.getInt("entityTypeId")).isGreaterThan(0);
        assertThat(ownerWrapper.getStr("entityType.description")).isEqualTo("Business");
        assertThat(ownerWrapper.getListSize("accounts")).isEqualTo(3);
        assertThat(ownerWrapper.getStr("accounts[0].accountNumber")).isNotEmpty();

        assertThat(ownerWrapper.getListSize("relationships")).isEqualTo(3);
        assertThat(ownerWrapper.getListSize("relationships[ last ].securityGroupMoz")).isEqualTo(2);
        assertThat(ownerWrapper.getInt("relationships[ last ].securityGroupMoz[ 0 ].id")).isEqualTo(1013);      // Moz Commuter
        assertThat(ownerWrapper.toBool("relationships[ last ].securityGroupMoz[ 0 ].active")).isTrue();
        assertThat(ownerWrapper.getStrList("relationships[ last ].securityGroupMoz[ 0 ].rightsList")).isEqualTo(
                List.of("MOZ_TOPUP", "MOZ_PROFILE", "MOZ_TAG_MANAGEMENT", "MOZ_STATEMENT"));
        assertThat(ownerWrapper.getInt("relationships[ last ].securityGroupMoz[ 1 ].id")).isEqualTo(1014);      // Moz Transport Owner
        assertThat(ownerWrapper.toBool("relationships[ last ].securityGroupMoz[ 1 ].active")).isTrue();
        assertThat(ownerWrapper.getStrList("relationships[ last ].securityGroupMoz[ 1 ].rightsList")).isEqualTo(
                List.of("MOZ_TOPUP", "MOZ_POS_MANAGEMENT", "MOZ_PROFILE", "MOZ_VRN_MANAGEMENT", "MOZ_TAG_MANAGEMENT", "MOZ_STATEMENT"));
        return ownerWrapper;
    }

    private Wrapper makeDriver() {
        var agreement = graphQL.call(format(mozGetRegAgreement, "en", "TaxiDriver"))
                .print("  agreement for TaxiDriver");
        assertThat(agreement.getStr("value")).isNotNull();

        String regOtp = sendOtpMobile("MOZ_REG_USER");
        var user = graphQL.call(format(mozRegIndividualUser, regOtp, "TaxiDriver", "Test driver", "Test taxi driver", "Passport",
                        "1213141518", null, "12341237", null, MOBILE, "test.driver.user@ice.cash", PASSWORD, "en"), agentToken)
                .print("  driver");
        driverEntityId = user.getInt("id");
        assertThat(user.getInt("id")).isGreaterThan(0);
        assertThat(user.getStr("firstName")).isEqualTo("Test driver");
        assertThat(user.getStr("lastName")).isEqualTo("Test taxi driver");
        assertThat(user.getStr("email")).isEqualTo("test.driver.user@ice.cash");
        assertThat(user.getStr("msisdn[0].msisdn")).isEqualTo(MOBILE);
        assertThat(user.getStr("idNumber")).isEqualTo("1213141518");
        assertThat(user.getInt("idTypeId")).isGreaterThan(0);
        assertThat(user.getInt("entityTypeId")).isGreaterThan(0);
        assertThat(user.getStr("entityType.description")).isEqualTo("Private");
        assertThat(user.getListSize("accounts")).isEqualTo(3);
        assertThat(user.getStr("accounts[0].accountNumber")).isNotEmpty();

        assertThat(user.getListSize("relationships")).isEqualTo(3);
        assertThat(user.getListSize("relationships[ last ].securityGroupMoz")).isEqualTo(2);
        assertThat(user.getInt("relationships[ last ].securityGroupMoz[ 0 ].id")).isEqualTo(1013);      // Moz Commuter
        assertThat(user.toBool("relationships[ last ].securityGroupMoz[ 0 ].active")).isTrue();
        assertThat(user.getStrList("relationships[ last ].securityGroupMoz[ 0 ].rightsList")).isEqualTo(
                List.of("MOZ_TOPUP", "MOZ_PROFILE", "MOZ_TAG_MANAGEMENT", "MOZ_STATEMENT"));
        assertThat(user.getInt("relationships[ last ].securityGroupMoz[ 1 ].id")).isEqualTo(1015);      // Moz Taxi Driver
        assertThat(user.toBool("relationships[ last ].securityGroupMoz[ 1 ].active")).isTrue();
        assertThat(user.getStrList("relationships[ last ].securityGroupMoz[ 1 ].rightsList")).isEqualTo(
                List.of("MOZ_POS_LOGIN"));
        return user;
    }

    private Wrapper makeCollector() {
        var agreement = graphQL.call(format(mozGetRegAgreement, "en", "FareCollector"))
                .print("  agreement for FareCollector");
        assertThat(agreement.getStr("value")).isNotNull();

        String regOtp = sendOtpMobile("MOZ_REG_USER");
        var user = graphQL.call(format(mozRegIndividualUser, regOtp, "FareCollector", "Test collector", "Test fare collector",
                        "ID", "1213141519", null, "12341238", null, MOBILE, "test.collector.user@ice.cash", PASSWORD, "en"), agentToken)
                .print("  collector");
        collectorEntityId = user.getInt("id");
        assertThat(user.getInt("id")).isGreaterThan(0);
        assertThat(user.getStr("firstName")).isEqualTo("Test collector");
        assertThat(user.getStr("lastName")).isEqualTo("Test fare collector");
        assertThat(user.getStr("email")).isEqualTo("test.collector.user@ice.cash");
        assertThat(user.getStr("msisdn[0].msisdn")).isEqualTo(MOBILE);
        assertThat(user.getStr("idNumber")).isEqualTo("1213141519");
        assertThat(user.getInt("idTypeId")).isGreaterThan(0);
        assertThat(user.getInt("entityTypeId")).isGreaterThan(0);
        assertThat(user.getStr("entityType.description")).isEqualTo("Private");
        assertThat(user.getListSize("accounts")).isEqualTo(3);
        assertThat(user.getStr("accounts[0].accountNumber")).isNotEmpty();

        assertThat(user.getListSize("relationships")).isEqualTo(3);
        assertThat(user.getListSize("relationships[ last ].securityGroupMoz")).isEqualTo(2);
        assertThat(user.getInt("relationships[ last ].securityGroupMoz[ 0 ].id")).isEqualTo(1013);      // Moz Commuter
        assertThat(user.toBool("relationships[ last ].securityGroupMoz[ 0 ].active")).isTrue();
        assertThat(user.getStrList("relationships[ last ].securityGroupMoz[ 0 ].rightsList")).isEqualTo(
                List.of("MOZ_TOPUP", "MOZ_PROFILE", "MOZ_TAG_MANAGEMENT", "MOZ_STATEMENT"));
        assertThat(user.getInt("relationships[ last ].securityGroupMoz[ 1 ].id")).isEqualTo(1016);      // Moz Fare Collector
        assertThat(user.toBool("relationships[ last ].securityGroupMoz[ 1 ].active")).isTrue();
        assertThat(user.getStrList("relationships[ last ].securityGroupMoz[ 1 ].rightsList")).isEqualTo(
                List.of("MOZ_POS_LOGIN"));
        return user;
    }

    private Wrapper makeCommuter() {
        var agreementWrapper = graphQL.call(format(mozGetRegAgreement, "en", "CommuterRegular"))
                .print("  agreement for CommuterRegular");
        assertThat(agreementWrapper.getStr("value")).isNotNull();

        String regOtp = sendOtpMobile("MOZ_REG_USER");
        var user = graphQL.call(format(mozRegIndividualUser, regOtp, "CommuterRegular", "Test commuter", "Test regular commuter",
                        "ID", "1213141520", null, "12341239", null, MOBILE, "test.commuter.user@ice.cash", PASSWORD, "en"), agentToken)
                .print("  commuter");
        assertThat(user.getInt("id")).isGreaterThan(0);
        assertThat(user.getStr("firstName")).isEqualTo("Test commuter");
        assertThat(user.getStr("lastName")).isEqualTo("Test regular commuter");
        assertThat(user.getStr("email")).isEqualTo("test.commuter.user@ice.cash");
        assertThat(user.getStr("msisdn[0].msisdn")).isEqualTo(MOBILE);
        assertThat(user.getStr("idNumber")).isEqualTo("1213141520");
        assertThat(user.getInt("idTypeId")).isGreaterThan(0);
        assertThat(user.getInt("entityTypeId")).isGreaterThan(0);
        assertThat(user.getStr("entityType.description")).isEqualTo("Private");

        assertThat(user.getListSize("relationships")).isEqualTo(3);
        assertThat(user.getListSize("relationships[ last ].securityGroupMoz")).isEqualTo(1);
        assertThat(user.getInt("relationships[ last ].securityGroupMoz[ 0 ].id")).isEqualTo(1013);      // Moz Commuter
        assertThat(user.toBool("relationships[ last ].securityGroupMoz[ 0 ].active")).isTrue();
        assertThat(user.getStrList("relationships[ last ].securityGroupMoz[ 0 ].rightsList")).isEqualTo(
                List.of("MOZ_TOPUP", "MOZ_PROFILE", "MOZ_TAG_MANAGEMENT", "MOZ_STATEMENT"));

        assertThat(user.getListSize("accounts")).isEqualTo(3);
        assertThat(user.getStr("accounts[ 0 ].accountNumber")).isNotEmpty();
        assertThat(user.getStr("accounts[ 1 ].accountType.name")).isEqualTo("Subsidy");
        assertThat(user.getStr("accounts[ 2 ].accountType.name")).isEqualTo("Prepaid");
        commuterEntityId = user.getInt("id");
        commuterSubsidyAccountId = user.getInt("accounts[ 1 ].id");
        commuterPrepaidAccountId = user.getInt("accounts[ 2 ].id");
        apiUtil.topupAccount(String.valueOf(commuterPrepaidAccountId), "10000.0");
        apiUtil.topupAccount(String.valueOf(commuterSubsidyAccountId), "1000.0");
        return user;
    }

    private Wrapper makeVehicle(String userToken, String ownerAccountNumber) {
        var routes = graphQL.call(mozGetRoutes, userToken)
                .print("  routes");
        assertThat(routes.getListSize("")).isGreaterThan(0);
        routeWrapper = routes.toWrapper("[ name = {ROUTE_NAME} ]", ROUTE_NAME);
        assertThat(routeWrapper.toBool("active")).isTrue();
        assertThat(routeWrapper.getListSize("details")).isEqualTo(2);
        assertThat(routeWrapper.getStr("details[ 0 ].operatorType")).isEqualTo("PRIVATE");
        assertThat(routeWrapper.getStr("details[ 1 ].operatorType")).isEqualTo("PUBLIC");

        var newVehicle = graphQL.call(format(mozCreateVehicle, "testVrn1", "testMake1", "testModel1", "TAXI",
                        routeWrapper.getInt("id"), driverEntityId, collectorEntityId), userToken)
                .print("  vehicle");
        vehicleId = newVehicle.getInt("id");
        var vehicles = graphQL.call(mozGetVehicles, ownerToken)
                .print("  vehicles");
        assertThat(vehicles.getInt("total")).isEqualTo(1);
        assertThat(vehicles.getInt("content[ 0 ].id")).isGreaterThan(0);
        assertThat(vehicles.getStr("content[ 0 ].vrn")).isEqualTo("testVrn1");
        assertThat(vehicles.getStr("content[ 0 ].make")).isEqualTo("testMake1");
        assertThat(vehicles.getStr("content[ 0 ].vehicleType")).isEqualTo("TAXI");
        assertThat(vehicles.getInt("content[ 0 ].routeId")).isEqualTo(routeWrapper.getInt("id"));
        assertThat(vehicles.getStr("content[ 0 ].route.name")).isEqualTo(ROUTE_NAME);
        assertThat(vehicles.getStr("content[ 0 ].status")).isEqualTo("ACTIVE");
        assertThat(vehicles.getInt("content[ 0 ].driverEntityId")).isEqualTo(driverEntityId);
        assertThat(vehicles.getInt("content[ 0 ].collectorEntityId")).isEqualTo(collectorEntityId);
        assertThat(vehicles.getStr("content[ 0 ].account.accountNumber")).isEqualTo(ownerAccountNumber);
        return newVehicle;
    }

    private Wrapper makePosDevice() {
        var posWrapper = graphQL.call(format(mozCreatePosDevice, DEVICE_SERIAL, "etestProduct1", "etestModel1",
                        "etestBootVer1", "etestCpu1", "etestRfidVer1", "etestOs1", "etestImei1", "etestImsi1"))
                .print("  pos");
        assertThat(posWrapper.getStr()).isNotBlank();
        posDeviceCode = posWrapper.getStr();
        String posLinkOtp = sendOtpEntity("MOZ_POS_LINK", ownerEntityId);

        var linkPos1 = graphQL.call(format(mozLinkPosDeviceByAgent, DEVICE_SERIAL, ownerEntityId, posLinkOtp), agentToken)
                .print("  link pos1");
        assertThat(linkPos1.getInt("id")).isGreaterThan(0);
        assertThat(linkPos1.getStr("serial")).isEqualTo(DEVICE_SERIAL);
        assertThat(linkPos1.getStr("status")).isEqualTo("INACTIVE");
        assertThat(linkPos1.getInt("account.entity.id")).isEqualTo(ownerEntityId);
        assertThat(linkPos1.getObject("vehicle")).isNull();

        var linkPos2 = graphQL.call(format(mozLinkVehicleToPosDeviceByOwner, DEVICE_SERIAL, vehicleId), ownerToken)
                .print("  link pos2");
        assertThat(linkPos2.getStr("serial")).isEqualTo(DEVICE_SERIAL);
        assertThat(linkPos2.getStr("status")).isEqualTo("ACTIVE");
        assertThat(linkPos2.getInt("account.entity.id")).isEqualTo(ownerEntityId);
        assertThat(linkPos2.getInt("vehicle.id")).isEqualTo(vehicleId);

        var simpleInfo = graphQL.call(format(mozGetSimpleAccountInfo, posDeviceCode))
                .print("  simple account info");
        assertThat(simpleInfo.getInt("accountId")).isEqualTo(ownerWrapper.getInt("accounts[0].id"));
        assertThat(simpleInfo.getStr("accountNumber")).isEqualTo(ownerWrapper.getStr("accounts[0].accountNumber"));
        assertThat(simpleInfo.getStr("accountType")).isEqualTo(ownerWrapper.getStr("accounts[0].accountType.description"));
        assertThat(simpleInfo.getStr("firstName")).isEqualTo(ownerWrapper.getStr("firstName"));
        assertThat(simpleInfo.getStr("lastName")).isEqualTo(ownerWrapper.getStr("lastName"));
        assertThat(simpleInfo.getStr("deviceCode")).isEqualTo(posDeviceCode);
        assertThat(simpleInfo.getStr("deviceStatus")).isEqualTo("ACTIVE");
        assertThat(simpleInfo.getInt("route.id")).isEqualTo(routeWrapper.getInt("id"));
        assertThat(simpleInfo.getStr("route.name")).isEqualTo(ROUTE_NAME);
        assertThat(simpleInfo.getListSize("route.details")).isEqualTo(2);
        assertThat(simpleInfo.getStr("route.details[0].operatorType")).isEqualTo("PRIVATE");
        assertThat(simpleInfo.getStr("route.details[1].operatorType")).isEqualTo("PUBLIC");
        return posWrapper;
    }

    private Wrapper makeTag(Wrapper commuterWrapper) {
        var tag = rest.sendPostMultipartRequest("/moz/me60/tag/link/clear", body -> body.add(TAG, TEST_TAG))
                .print("  create tag");
        String commuterPrepaidAccountNumber = commuterWrapper.getStr("accounts[2].accountNumber");
        String linkTagOtp = sendOtpAccount("MOZ_TAG_LINK", commuterPrepaidAccountNumber);

        var linkTag = graphQL.call(format(mozLinkTag, linkTagOtp, DEVICE_SERIAL, commuterPrepaidAccountNumber, TEST_TAG, "2023-02-07 12:30:10"))
                .print("  link tag");
        assertThat(linkTag.getStr("status")).isEqualTo("Active");
        assertThat(linkTag.getStr("firstName")).isEqualTo("Test commuter");
        assertThat(linkTag.getStr("lastName")).isEqualTo("Test regular commuter");
        assertThat(linkTag.getStr("accountNumber")).isEqualTo(commuterPrepaidAccountNumber);
        assertThat(linkTag.getStr("accountNumber")).isEqualTo(commuterPrepaidAccountNumber);
        assertThat(linkTag.getDbl("prepaidBalance")).isEqualTo(10000.0);
        assertThat(linkTag.getDbl("subsidyBalance")).isEqualTo(1000.0);
        return tag;
    }

    private void makePayments(String vendorRef) {
        var payment = graphQL.call(format(mozMakePayment, vendorRef, TSF, TAG, TEST_TAG, MZN, 10, "2023-02-07 12:30:10", posDeviceCode))
                .print("  payment response");
        assertThat(payment.getStr("status")).isEqualTo("SUCCESS");
        assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment.getStr("transactionId")).isNotNull();
        assertThat(payment.getStr("date")).isNotNull();
        assertThat(payment.getDbl("balance")).isEqualTo(9992.0);
        checkAccounts(commuterToken, 0.0, 998.0, 9992.0);
        checkAccounts(ownerToken, 10.0, 0.0, 0.0);

        var payment2 = graphQL.call(format(mozMakePayment, vendorRef, TSF, TAG, TEST_TAG, MZN, 8, "2023-02-07 12:30:10", posDeviceCode))
                .print("  payment response");
        assertThat(payment2.getStr("status")).isEqualTo("SUCCESS");
        assertThat(payment2.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment2.getStr("transactionId")).isNotNull();
        assertThat(payment2.getStr("date")).isNotNull();
        assertThat(payment2.getDbl("balance")).isEqualTo(9993.6);
        checkAccounts(commuterToken, 0.0, 998.4, 9993.6);
        checkAccounts(ownerToken, 8.0, 0.0, 0.0);
    }

    private void checkAccounts(String token, Double expectedPrimaryBalance, Double expectedSubsidyBalance, Double expectedPrepaidBalance) {
        var userDetails = graphQL.call(mozUserDetailsRequestStr, token)
                .print("  user details");
        assertThat(userDetails.getDbl("accounts[ accountType.name=Primary ].balance")).isEqualTo(expectedPrimaryBalance);
        assertThat(userDetails.getDbl("accounts[ accountType.name=Subsidy ].balance")).isEqualTo(expectedSubsidyBalance);
        assertThat(userDetails.getDbl("accounts[ accountType.name=Prepaid ].balance")).isEqualTo(expectedPrepaidBalance);
    }

    private void destroyResourcesIfNeed() {
        if (tagWrapper != null) {
            rest.sendSimpleDeleteRequest("/moz/me60/tag/remove", "tag=" + TEST_TAG);
        }
        if (posWrapper != null) {
            rest.sendSimpleDeleteRequest("/moz/me60/device/remove", "serialOrCode=" + DEVICE_SERIAL);
        }
        if (vehicleId != null) {
            graphQL.call(format(mozDeleteVehicle, vehicleId), ownerToken);
        }
        if (commuterEntityId != null) {
            rest.deleteMozUser(commuterEntityId);
        }
        if (collectorEntityId != null) {
            rest.deleteMozUser(collectorEntityId);
        }
        if (driverEntityId != null) {
            rest.deleteMozUser(driverEntityId);
        }
        if (ownerEntityId != null) {
            rest.deleteMozUser(ownerEntityId);
        }
        if (agentEntityId != null) {
            rest.deleteMozUser(agentEntityId);
        }
    }
}

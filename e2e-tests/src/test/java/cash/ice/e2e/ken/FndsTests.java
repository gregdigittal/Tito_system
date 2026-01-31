package cash.ice.e2e.ken;

import cash.ice.ApiUtil;
import cash.ice.GraphQLHelper;
import cash.ice.RestHelper;
import cash.ice.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;

import static cash.ice.GraphQlRequests.*;
import static cash.ice.sqldb.entity.InitiatorType.ACCOUNT_NUMBER;
import static cash.ice.sqldb.entity.TransactionCode.KIT;
import static cash.ice.sqldb.entity.Currency.KES;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FndsTests {
    private static final String MOBILE = "000000000000";
    private static final String PASSWORD = "1234";
    private static final String DEVICE_SERIAL = "etest-device-serial";

    private final RestHelper rest = new RestHelper();
    private final GraphQLHelper graphQL = new GraphQLHelper(rest);
    private final ApiUtil apiUtil = new ApiUtil(rest, graphQL);

    private Integer dealerEntityId;
    private Integer farmerEntityId;
    private Integer productId;
    private Wrapper deviceWrapper;
    private String posDeviceCode;

    @Test
    public void testFndsFlow() {
        try {
            Wrapper dealerWrapper = makeDealer();
            String dealerAccountNumber = dealerWrapper.getStr("accounts[1].accountNumber");
            var dealerLogin = graphQL.call(format(fndsLoginUser, dealerAccountNumber, PASSWORD));
            String dealerToken = dealerLogin.getStr("accessToken.token");

            Wrapper farmerWrapper = makeFarmer();
            String farmerAccountNumber = farmerWrapper.getStr("accounts[1].accountNumber");
            var farmerLogin = graphQL.call(format(fndsLoginUser, dealerAccountNumber, PASSWORD));
            String farmerToken = farmerLogin.getStr("accessToken.token");

            deviceWrapper = makePosDevice(dealerAccountNumber);
            makeProducts();

            apiUtil.topupAccount(farmerAccountNumber, "7000.0");
            testSearch(0.0, 7000.0);
            makeSinglePayment(farmerAccountNumber);
            makeBulkPayment(farmerAccountNumber);

        } finally {
            destroyResourcesIfNeed();
        }
    }

    private void testSearch(double dealerBalance, double farmerBalance) {
        Wrapper dealerWrapper = graphQL.call(format(fndsEntitiesSearch, "AgriDealer", format("idType: %s", "NationalID"), format("idNumber: \"%s\"", "1213151111"), format("mobile: \"%s\"", MOBILE))).print("  search dealer");
        assertThat(dealerWrapper.getInt("total")).isEqualTo(1);
        assertThat(dealerWrapper.getInt("content[0].id")).isEqualTo(dealerEntityId);
        assertThat(dealerWrapper.getDbl("content[0].fndsKesAccountBalance")).isEqualTo(dealerBalance);
        assertThat(dealerWrapper.getStr("content[0].entityType.description")).isEqualTo("FNDS Agri Dealer");
        assertThat(dealerWrapper.getStr("content[0].firstName")).isEqualTo("Fnds Agri Dealer");
        assertThat(dealerWrapper.getStr("content[0].lastName")).isEqualTo("Test Fnds agri dealer");
        assertThat(dealerWrapper.getStr("content[0].idType.description")).isEqualTo("Kenya National ID");
        assertThat(dealerWrapper.getStr("content[0].idNumber")).isEqualTo("1213151111");
        assertThat(dealerWrapper.getStr("content[0].msisdn[0].msisdn")).isEqualTo(MOBILE);
        assertThat(dealerWrapper.getStr("content[0].status")).isEqualTo("ACTIVE");

        Wrapper farmerWrapper = graphQL.call(format(fndsEntitiesSearch, "Farmer", format("idType: %s", "Passport"), format("idNumber: \"%s\"", "1213151112"), format("mobile: \"%s\"", MOBILE))).print("  search dealer");
        assertThat(farmerWrapper.getInt("total")).isEqualTo(1);
        assertThat(farmerWrapper.getInt("content[0].id")).isEqualTo(farmerEntityId);
        assertThat(farmerWrapper.getDbl("content[0].fndsKesAccountBalance")).isEqualTo(farmerBalance);
        assertThat(farmerWrapper.getStr("content[0].entityType.description")).isEqualTo("FNDS Farmer");
        assertThat(farmerWrapper.getStr("content[0].firstName")).isEqualTo("Fnds Farmer");
        assertThat(farmerWrapper.getStr("content[0].lastName")).isEqualTo("Test Fnds farmer");
        assertThat(farmerWrapper.getStr("content[0].idType.description")).isEqualTo("Passport");
        assertThat(farmerWrapper.getStr("content[0].idNumber")).isEqualTo("1213151112");
        assertThat(farmerWrapper.getStr("content[0].msisdn[0].msisdn")).isEqualTo(MOBILE);
        assertThat(farmerWrapper.getStr("content[0].status")).isEqualTo("ACTIVE");
    }

    private Wrapper makeDealer() {
        String regOtp = sendOtpMobile("FNDS_REG_USER");
        Wrapper dealerWrapper = graphQL.call(format(fndsRegUser, regOtp, "AgriDealer", "Fnds Agri Dealer", "Test Fnds agri dealer", "NationalID", "1213151111", MOBILE, PASSWORD)).print("  dealer");
        dealerEntityId = dealerWrapper.getInt("id");
        assertThat(dealerWrapper.getInt("id")).isGreaterThan(0);
        assertThat(dealerWrapper.getStr("entityType.description")).isEqualTo("FNDS Agri Dealer");
        assertThat(dealerWrapper.getStr("firstName")).isEqualTo("Fnds Agri Dealer");
        assertThat(dealerWrapper.getStr("lastName")).isEqualTo("Test Fnds agri dealer");
        assertThat(dealerWrapper.getStr("idType.description")).isEqualTo("Kenya National ID");
        assertThat(dealerWrapper.getStr("idNumber")).isEqualTo("1213151111");
        assertThat(dealerWrapper.getStr("msisdn[0].msisdn")).isEqualTo(MOBILE);
        assertThat(dealerWrapper.getStr("email")).isEqualTo(null);
        assertThat(dealerWrapper.getDbl("fndsKesAccountBalance")).isEqualTo(0.0);
        assertThat(dealerWrapper.getStr("status")).isEqualTo("ACTIVE");
        assertThat(dealerWrapper.getStr("loginStatus")).isEqualTo("ACTIVE");
        assertThat(dealerWrapper.getStr("locale")).isEqualTo("en");
        assertThat(dealerWrapper.getStr("createdDate")).isNotBlank();

        assertThat(dealerWrapper.getListSize("accounts")).isEqualTo(2);
        assertThat(dealerWrapper.getStr("accounts[0].accountNumber")).isNotEmpty();
        assertThat(dealerWrapper.getStr("accounts[0].accountType.name")).isEqualTo("Primary");
        assertThat(dealerWrapper.getStr("accounts[0].accountType.currency.isoCode")).isEqualTo("KES");
        assertThat(dealerWrapper.getDbl("accounts[0].balance")).isEqualTo(0.0);
        assertThat(dealerWrapper.getStr("accounts[0].createdDate")).isNotBlank();
        assertThat(dealerWrapper.getStr("accounts[1].accountNumber")).isNotEmpty();
        assertThat(dealerWrapper.getStr("accounts[1].accountType.name")).isEqualTo("FNDS");
        assertThat(dealerWrapper.getStr("accounts[1].accountType.currency.isoCode")).isEqualTo("KES");
        assertThat(dealerWrapper.getDbl("accounts[1].balance")).isEqualTo(0.0);
        assertThat(dealerWrapper.getStr("accounts[1].createdDate")).isNotBlank();

        assertThat(dealerWrapper.getListSize("relationships")).isEqualTo(2);
        assertThat(dealerWrapper.getStr("relationships[ last ].partnerAccountId")).isEqualTo(dealerWrapper.getStr("accounts[ last ].id"));
        assertThat(dealerWrapper.getInt("relationships[ last ].securityGroups.FNDS[0]")).isEqualTo(2002);
        return dealerWrapper;
    }

    private Wrapper makeFarmer() {
        String regOtp = sendOtpMobile("FNDS_REG_USER");
        Wrapper farmerWrapper = graphQL.call(format(fndsRegUser, regOtp, "Farmer", "Fnds Farmer", "Test Fnds farmer", "Passport", "1213151112", MOBILE, PASSWORD)).print("  dealer");
        farmerEntityId = farmerWrapper.getInt("id");
        assertThat(farmerWrapper.getInt("id")).isGreaterThan(0);
        assertThat(farmerWrapper.getStr("entityType.description")).isEqualTo("FNDS Farmer");
        assertThat(farmerWrapper.getStr("firstName")).isEqualTo("Fnds Farmer");
        assertThat(farmerWrapper.getStr("lastName")).isEqualTo("Test Fnds farmer");
        assertThat(farmerWrapper.getStr("idType.description")).isEqualTo("Passport");
        assertThat(farmerWrapper.getStr("idNumber")).isEqualTo("1213151112");
        assertThat(farmerWrapper.getStr("msisdn[0].msisdn")).isEqualTo(MOBILE);
        assertThat(farmerWrapper.getStr("email")).isEqualTo(null);
        assertThat(farmerWrapper.getDbl("fndsKesAccountBalance")).isEqualTo(0.0);
        assertThat(farmerWrapper.getStr("status")).isEqualTo("ACTIVE");
        assertThat(farmerWrapper.getStr("loginStatus")).isEqualTo("ACTIVE");
        assertThat(farmerWrapper.getStr("locale")).isEqualTo("en");
        assertThat(farmerWrapper.getStr("createdDate")).isNotBlank();

        assertThat(farmerWrapper.getListSize("accounts")).isEqualTo(2);
        assertThat(farmerWrapper.getStr("accounts[0].accountNumber")).isNotEmpty();
        assertThat(farmerWrapper.getStr("accounts[0].accountType.name")).isEqualTo("Primary");
        assertThat(farmerWrapper.getStr("accounts[0].accountType.currency.isoCode")).isEqualTo("KES");
        assertThat(farmerWrapper.getDbl("accounts[0].balance")).isEqualTo(0.0);
        assertThat(farmerWrapper.getStr("accounts[0].createdDate")).isNotBlank();
        assertThat(farmerWrapper.getStr("accounts[1].accountNumber")).isNotEmpty();
        assertThat(farmerWrapper.getStr("accounts[1].accountType.name")).isEqualTo("FNDS");
        assertThat(farmerWrapper.getStr("accounts[1].accountType.currency.isoCode")).isEqualTo("KES");
        assertThat(farmerWrapper.getDbl("accounts[1].balance")).isEqualTo(0.0);
        assertThat(farmerWrapper.getStr("accounts[1].createdDate")).isNotBlank();

        assertThat(farmerWrapper.getListSize("relationships")).isEqualTo(2);
        assertThat(farmerWrapper.getStr("relationships[ last ].partnerAccountId")).isEqualTo(farmerWrapper.getStr("accounts[ last ].id"));
        assertThat(farmerWrapper.getInt("relationships[ last ].securityGroups.FNDS[0]")).isEqualTo(2001);
        return farmerWrapper;
    }

    private Wrapper makePosDevice(String dealerAccountNumber) {
        var deviceWrapper = graphQL.call(format(mozCreatePosDevice, DEVICE_SERIAL, "etestProduct1", "etestModel1", "etestBootVer1", "etestCpu1", "etestRfidVer1", "etestOs1", "etestImei1", "etestImsi1")).print("  device");
        assertThat(deviceWrapper.getStr()).isNotBlank();
        posDeviceCode = deviceWrapper.getStr();

        rest.sendPostMultipartRequest("/moz/me60/device/activate", body -> {
            body.add("serialOrCode", posDeviceCode);
            body.add("accountNumber", dealerAccountNumber);
        });
        return deviceWrapper;
    }

    private void makeProducts() {
        Integer kesId = apiUtil.getCurrencyId("KES");
        Wrapper product = rest.sendPostMultipartRequest("/ken/product", body -> {
            body.add("productType", "KIT");
            body.add("name", "Test Kit");
            body.add("description", "Test Kit description");
            body.add("currencyId", kesId);
            body.add("price", "1000.00");
        });
        System.out.printf("%s: %s%n", "  product", product);
        productId = product.getInt("id");
        Wrapper deviceProduct = rest.sendPostMultipartRequest("/ken/entity/product", body -> {
            body.add("entityId", dealerEntityId);
            body.add("productId", productId);
            body.add("relationshipType", "DealerStock");
            body.add("active", true);
        });
        System.out.printf("%s: %s%n", "  device product", deviceProduct);
        var productsWrapper = graphQL.call(format(fndsGetDeviceProducts, posDeviceCode)).print("  get device products");
        assertThat(productsWrapper.getInt("[0].entityId")).isEqualTo(dealerEntityId);
        assertThat(productsWrapper.getStr("[0].entity.firstName")).isEqualTo("Fnds Agri Dealer");
        assertThat(productsWrapper.getInt("[0].productId")).isEqualTo(productId);
        assertThat(productsWrapper.getStr("[0].relationship")).isEqualTo("DealerStock");
        assertThat(productsWrapper.toBool("[0].active")).isEqualTo(true);
        assertThat(productsWrapper.getStr("[0].createdDate")).isNotBlank();
        assertThat(productsWrapper.getStr("[0].modifiedDate")).isNotBlank();
    }

    private void makeSinglePayment(String farmerAccountNumber) {
        String vendorRef = UUID.randomUUID().toString();
        var payment = graphQL.call(format(fndsMakePayment, vendorRef, KIT, ACCOUNT_NUMBER, farmerAccountNumber, KES, 1000, "2024-03-20 12:30:10", posDeviceCode, 1))        // todo productId
                .print("  payment response");
        assertThat(payment.getStr("status")).isEqualTo("SUCCESS");
        assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment.getStr("transactionId")).isNotNull();
        assertThat(payment.getStr("date")).isNotNull();
        assertThat(payment.getDbl("balance")).isEqualTo(6000.0);
        testSearch(1000.0, 6000.0);
    }

    private void makeBulkPayment(String farmerAccountNumber) {
        String vendorRef1 = UUID.randomUUID().toString();
        String vendorRef2 = UUID.randomUUID().toString();
        var payment = graphQL.call(format(fndsMakeBulkPayment, vendorRef1, KIT, ACCOUNT_NUMBER, farmerAccountNumber, KES, 1000, "2024-03-20 12:30:10", posDeviceCode, 1, vendorRef2, KIT, ACCOUNT_NUMBER, farmerAccountNumber, KES, 1000, "2024-03-20 12:30:10", posDeviceCode, 1))        // todo productId
                .print("  bulk payment response");
        assertThat(payment.getStr("status")).isEqualTo("SUCCESS");
        assertThat(payment.getStr("vendorRef")).isEqualTo(null);
        assertThat(payment.getStr("message")).isEqualTo("Transaction offload completed");
        assertThat(payment.getDbl("balance")).isEqualTo(4000.0);
        testSearch(3000.0, 4000.0);
    }

    private String sendOtpMobile(String otpType) {
        var otpW = graphQL.call(format(sendOtpMobile, otpType, MOBILE, "false")).print("  otp response(mobile)");
        assertThat(otpW.getStr()).isEqualTo("SUCCESS");
        String otp = rest.sendSimpleGetRequest("/moz/otp", format("otpType=%s&msisdn=%s", otpType, MOBILE));
        System.out.println("  otp: " + otp);
        assertThat(otp).isNotBlank();
        return otp;
    }

    private void destroyResourcesIfNeed() {
        if (productId != null) {
            rest.deleteProduct(productId);
        }
        if (deviceWrapper != null) {
            rest.sendSimpleDeleteRequest("/moz/me60/device/remove", "serialOrCode=" + DEVICE_SERIAL);
        }
        if (farmerEntityId != null) {
            rest.deleteMozUser(farmerEntityId);
        }
        if (dealerEntityId != null) {
            rest.deleteMozUser(dealerEntityId);
        }
    }
}

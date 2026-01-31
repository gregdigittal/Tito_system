package cash.ice;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import static cash.ice.BackofficeRequests.loginMfaStaffMember;
import static cash.ice.BackofficeRequests.loginStaffMember;
import static cash.ice.GraphQlRequests.getCurrencies;
import static cash.ice.GraphQlRequests.loginRequestStr;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class ApiUtil {
    private final RestHelper rest;
    private final GraphQLHelper graphQL;

    public String mozLogin(String username, String password) {
        var login = graphQL.call(format(loginRequestStr, username, password));
        return login.getStr("accessToken.token");
    }

    public String otpLoginBackofficeUser(String email, String password) {
        graphQL.call(String.format(loginStaffMember, email, password))
                .print("  login");
        String otp = rest.sendSimpleGetRequest("/users/pin/staff/otp", "email=" + email);
        System.out.println("  otp: " + otp);
        assertThat(otp).isNotBlank();
        var loginMfa = graphQL.call(String.format(loginMfaStaffMember, email, otp))
                .print("  mfa");
        return loginMfa.getStr("accessToken.token");
    }

    public void topupAccount(String accountId, String amount) {
        rest.sendSimplePostMultipartRequest("/moz/account/topup", body -> {
            body.add("account", accountId);
            body.add("amount", amount);
            body.add("reference", "Test deposit");
        });
    }

    public String mozRegisterPosDevice(String serialNumber) {
        return rest.sendSimplePostRequest("/moz/me60/device/register", Map.of(
                "serialNumber", serialNumber,
                "productNumber", "test-prod",
                "model", "test-model",
                "bootVersion", "test-boot",
                "cpuType", "test-cpu",
                "rfidVersion", "test-rfid",
                "osVersion", "test-os",
                "imei", "test-imei",
                "imsi", "test-imsi"
        ));
    }

    public Integer getCurrencyId(String isoCode) {
        var currencies = graphQL.call(getCurrencies)
                .print("  currencies");
        return currencies.getInt(String.format("[isoCode = %s].id", isoCode));
    }
}

package cash.ice.e2e.zim;

import cash.ice.RestHelper;
import cash.ice.Wrapper;
import cash.ice.common.utils.Tool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ZimPosbTests {

    public static final int PAYMENT_ID = 101;
    public static final String MOBILE = "263788786951";
    public static final String ACCOUNT_NUMBER = "500005998256";
    private final RestHelper rest = new RestHelper();

    @Test
    public void testPosbSuccessfulPayment() {
        String vendorRef = "e2e-" + UUID.randomUUID().toString().replace("-", "");
        Wrapper payment = rest.sendPostRequest(RestHelper.zimHost + "/api/v1/zim/payment/sync", (headers, body) -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
            body.put("vendorRef", vendorRef);
            body.put("bankName", "POSB");
            body.put("accountNumber", ACCOUNT_NUMBER);
            body.put("amount", 0.20);
            body.put("metaData", Tool.newMetaMap()
                    .put("paymentId", PAYMENT_ID)
                    .put("approvePaymentFlat", true)
                    .put("simulateDb", "all")
                    .build());
        });
        System.out.println("  payment response: " + payment);
        assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment.getStr("status")).isEqualTo("OTP_WAITING");
        assertThat(payment.getStr("mobile")).isEqualTo(MOBILE);

        Wrapper paymentDebug = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/posb/%s/debug", vendorRef), null, headers -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
        });
        System.out.println("  posb payment debug: " + paymentDebug);
        assertThat(paymentDebug.getStr("_vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getObject("_requestTime")).isNotNull();
        assertThat(paymentDebug.getObject("_responseTime")).isNotNull();
        assertThat(paymentDebug.getObject("_expiration")).isNotNull();
        assertThat(paymentDebug.getObject("_request")).isNotNull();
        assertThat(paymentDebug.getObject("_handler")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.traceId")).isNotEmpty();
        assertThat(paymentDebug.getStr("_handler.status")).isEqualTo("otp");
        assertThat(paymentDebug.getStr("_handler.reason")).isNull();
        assertThat(paymentDebug.getObject("_handler.createdTime")).isNotNull();
        assertThat(paymentDebug.getObject("_handler.updatedTime")).isNull();
        assertThat(paymentDebug.getObject("_handler.refundedTime")).isNull();
        assertThat(paymentDebug.getStr("_handler.paymentRequest.vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.paymentRequest.partnerId")).isNull();
        assertThat(paymentDebug.getStr("_handler.paymentRequest.bankName")).isEqualTo("POSB");
        assertThat(paymentDebug.getStr("_handler.paymentRequest.accountNumber")).isEqualTo(ACCOUNT_NUMBER);
        assertThat(paymentDebug.getDbl("_handler.paymentRequest.amount")).isEqualTo(0.2);
        assertThat(paymentDebug.getStr("_handler.paymentRequest.currencyCode")).isEqualTo("ZWL");
        assertThat(paymentDebug.getStr("_handler.paymentRequest.paymentDescription")).isEqualTo("Simulated payment description");
        assertThat(paymentDebug.getInt("_handler.paymentRequest.paymentId")).isEqualTo(PAYMENT_ID);
        assertThat(paymentDebug.getStr("_handler.instructionRequest.paymentReference")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.instructionRequest.customerAccountNumber")).isEqualTo(ACCOUNT_NUMBER);
        assertThat(paymentDebug.getDbl("_handler.instructionRequest.amount")).isEqualTo(0.2);
        assertThat(paymentDebug.getStr("_handler.instructionRequest.description")).isEqualTo("Simulated payment description");
        assertThat(paymentDebug.getStr("_handler.instructionRequest.currency")).isEqualTo("ZWL");
        assertThat(paymentDebug.getStr("_handler.instructionResponse.paymentReference")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.instructionResponse.icecashPoolAccountNumber")).isEqualTo(ACCOUNT_NUMBER);
        assertThat(paymentDebug.getStr("_handler.instructionResponse.currency")).isEqualTo("ZWL");
        assertThat(paymentDebug.getStr("_handler.instructionResponse.customerName")).isEqualTo("EMMANUEL MANESWA");
        assertThat(paymentDebug.getStr("_handler.instructionResponse.customerMobileNumber")).isEqualTo(MOBILE);
        assertThat(paymentDebug.getStr("_handler.instructionResponse.status")).isEqualTo("PENDING");
        assertThat(paymentDebug.getStr("_handler.instructionResponse.narrative")).isEqualTo("OTP sent to customer mobile number");
        assertThat(paymentDebug.getStr("_handler.instructionResponse.otpExpiringTime")).isNotEmpty();
        assertThat(paymentDebug.getStr("_handler.instructionResponse.code")).isNull();
        assertThat(paymentDebug.getStr("_handler.instructionResponse.message")).isNull();
        assertThat(paymentDebug.getObject("_handler.confirmationRequest")).isNull();
        assertThat(paymentDebug.getObject("_handler.confirmationResponse")).isNull();
        assertThat(paymentDebug.getObject("_handler.statusResponse")).isNull();
        assertThat(paymentDebug.getObject("_handler.reversalRequest")).isNull();
        assertThat(paymentDebug.getObject("_handler.reversalResponse")).isNull();
        assertThat(paymentDebug.getObject("_response")).isNotNull();
        assertThat(paymentDebug.getStr("_response.vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_response.status")).isEqualTo("OTP_WAITING");
        assertThat(paymentDebug.getStr("_response.mobile")).startsWith(MOBILE);

        // OTP
        Wrapper paymentOtp = rest.sendPostRequest(RestHelper.zimHost + "/api/v1/zim/payment/otp/sync", (headers, body) -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
            body.put("vendorRef", vendorRef);
            body.put("otp", "1234");
        });
        System.out.println("  payment otp response: " + paymentOtp);
        assertThat(paymentOtp.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentOtp.getStr("status")).isEqualTo("SUCCESS");
        assertThat(paymentOtp.getStr("mobile")).startsWith(MOBILE);
        assertThat(paymentOtp.getStr("externalTransactionId")).startsWith(vendorRef);
        assertThat(paymentOtp.getStr("spResult.spName")).isEqualTo("p_Payment_Approval");
        assertThat(paymentOtp.getInt("spResult.transactionId")).isGreaterThan(0);
        assertThat(paymentOtp.getInt("spResult.result")).isEqualTo(1);
        assertThat(paymentOtp.getStr("spResult.message")).isEqualTo("Approved");
        assertThat(paymentOtp.getStr("spResult.error")).isEqualTo(null);

        paymentDebug = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/posb/%s/debug", vendorRef), null, headers -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
        });
        System.out.println("  posb payment debug: " + paymentDebug);
        assertThat(paymentDebug.getStr("_vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.traceId")).isNotEmpty();
        assertThat(paymentDebug.getStr("_handler.status")).isEqualTo("success");
        assertThat(paymentDebug.getObject("_handler.createdTime")).isNotNull();
        assertThat(paymentDebug.getObject("_handler.updatedTime")).isNotNull();
        assertThat(paymentDebug.getObject("_handler.refundedTime")).isNull();
        assertThat(paymentDebug.getObject("_handler.paymentRequest")).isNotNull();
        assertThat(paymentDebug.getObject("_handler.instructionRequest")).isNotNull();
        assertThat(paymentDebug.getObject("_handler.instructionResponse")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.confirmationRequest.paymentReference")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.confirmationRequest.otp")).isEqualTo("1234");
        assertThat(paymentDebug.getStr("_handler.confirmationResponse.paymentReference")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.confirmationResponse.status")).isEqualTo("SUCCESSFUL");
        assertThat(paymentDebug.getInt("_handler.confirmationResponse.responseCode")).isEqualTo(0);
        assertThat(paymentDebug.getStr("_handler.confirmationResponse.narrative")).isEqualTo("Success");
        assertThat(paymentDebug.getStr("_handler.confirmationResponse.code")).isNull();
        assertThat(paymentDebug.getStr("_handler.confirmationResponse.message")).isNull();
        assertThat(paymentDebug.getStr("_handler.statusResponse.paymentReference")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.statusResponse.status")).isEqualTo("SUCCESSFUL");
        assertThat(paymentDebug.getInt("_handler.statusResponse.responseCode")).isEqualTo(0);
        assertThat(paymentDebug.getStr("_handler.statusResponse.narrative")).isEqualTo("Success");
        assertThat(paymentDebug.getStr("_handler.confirmationResponse.code")).isNull();
        assertThat(paymentDebug.getStr("_handler.confirmationResponse.message")).isNull();
        assertThat(paymentDebug.getObject("_handler.reversalRequest")).isNull();
        assertThat(paymentDebug.getObject("_handler.reversalResponse")).isNull();
        assertThat(paymentDebug.getStr("_response.vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_response.status")).isEqualTo("SUCCESS");
        assertThat(paymentDebug.getStr("_response.mobile")).startsWith(MOBILE);
        assertThat(paymentDebug.getStr("_response.externalTransactionId")).startsWith(vendorRef);
        assertThat(paymentDebug.getStr("_response.spResult.spName")).isEqualTo("p_Payment_Approval");
    }
}

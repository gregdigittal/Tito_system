package cash.ice.e2e.zim;

import cash.ice.RestHelper;
import cash.ice.Wrapper;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.utils.Tool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ZimMpesaTests {

    private final RestHelper rest = new RestHelper();

    @Test
    public void testMpesaSuccessfulPayment() {
        String vendorRef = "e2e-" + UUID.randomUUID().toString().replace("-", "");
        Wrapper payment = rest.sendPostRequest(RestHelper.zimHost + "/api/v1/zim/payment/sync", (headers, body) -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
            body.put("vendorRef", vendorRef);
            body.put("bankName", "mpesa");
            body.put("accountNumber", "258843120879");
            body.put("amount", 10);
            body.put("metaData", Tool.newMetaMap()
                    .put("walletId", 99)
                    .put("transactionCode", "MTP")
                    .put("sessionId", 912984)
                    .put("channel", "WEB")
                    .put("accountId", 20528257)
                    .put("partnerId", 0)
                    .put("accountFundId", 20078466)
                    .put("cardNumber", "8811060000010302")
                    .put("paymentDescription", "Fund card")
                    .put("organisation", "Revimo")
                    .put("simulate", "SUCCESS")
                    .build());
        });
        System.out.println("  payment response: " + payment);
        assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment.getStr("status")).isEqualTo("SUCCESS");
        assertThat(payment.getStr("externalTransactionId")).startsWith("sim_");
        assertThat(payment.getStr("date")).isNotEmpty();
        assertThat(payment.getStr("spResult.spName")).isEqualTo("p_Create_Transaction_Card");
        assertThat(payment.getInt("spResult.transactionId")).isGreaterThan(0);
        assertThat(payment.getInt("spResult.drAccountId")).isEqualTo(20528257);
        assertThat(payment.getInt("spResult.crAccountId")).isGreaterThan(0);
        assertThat(payment.getDbl("spResult.balance")).isGreaterThan(0.0);
        assertThat(payment.getDbl("spResult.drFees")).isEqualTo(0.0);
        assertThat(payment.getInt("spResult.result")).isEqualTo(1);
        assertThat(payment.getStr("spResult.message")).isEqualTo("Funds successfully loaded. ");
        assertThat(payment.getStr("spResult.error")).isEqualTo(null);

        Wrapper paymentDebug = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/mpesa/%s/debug", vendorRef), null, headers -> {
            headers.set("Api-Key", "nttzhxmyjarfegtvznkarjxvhgfmxhgsizdvtvegiviceieddfdkfekvwyqtaqdf");
        });
        System.out.println("  payment debug: " + paymentDebug);
        assertThat(paymentDebug.getStr("_vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getObject("_requestTime")).isNotNull();
        assertThat(paymentDebug.getObject("_responseTime")).isNotNull();
        assertThat(paymentDebug.getObject("_expiration")).isNotNull();
        assertThat(paymentDebug.getObject("_request")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.payment.vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.payment.paymentType")).isEqualTo("Inbound");
        assertThat(paymentDebug.getStr("_handler.status")).isEqualTo("success");
        assertThat(paymentDebug.getStr("_handler.responseStatus")).isEqualTo("201 Created (simulated)");
        assertThat(paymentDebug.getStr("_handler.responseCode")).isEqualTo("INS-0");
        assertThat(paymentDebug.getStr("_handler.responseDesc")).isEqualTo("Request processed successfully (simulated)");
        assertThat(paymentDebug.getStr("_handler.transactionStatus")).isEqualTo("Completed");
        assertThat(paymentDebug.getStr("_handler.transactionStatusLine")).isEqualTo("HTTP/1.1 201 Created");
        assertThat(paymentDebug.getStr("_handler.transactionStatusResponse")).isEqualTo("INS-0 Request processed successfully (simulated)");
        assertThat(paymentDebug.getStr("_handler.errorCode")).isNull();
        assertThat(paymentDebug.getStr("_handler.errorMessage")).isNull();
        assertThat(paymentDebug.getObject("_handler.refunded")).isEqualTo(false);
        assertThat(paymentDebug.getStr("_handler.createdTime")).isNotEmpty();
        assertThat(paymentDebug.getStr("_handler.updatedTime")).isNotEmpty();
        assertThat(paymentDebug.getStr("_handler.refundTime")).isNull();
        assertThat(paymentDebug.getObject("_handler.paymentSuccessful")).isEqualTo(true);
        assertThat(paymentDebug.getObject("_response")).isNotNull();
        assertThat(paymentDebug.getStr("_response.vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_response.status")).isEqualTo("SUCCESS");
        assertThat(paymentDebug.getStr("_response.externalTransactionId")).startsWith("sim_");

        // refund
        Wrapper refund = rest.sendPostRequest(String.format("%s/api/v1/zim/payment/mpesa/%s/reversal", RestHelper.zimHost, vendorRef), (headers, body) -> {
//        Wrapper refund = rest.sendPostRequest(String.format("%s/api/v1/zim/payment/mpesa/%s/reversal?useExternalTransactionId=true", RestHelper.zimHost, vendorRef), (headers, body) -> {
            headers.set("Api-Key", "nttzhxmyjarfegtvznkarjxvhgfmxhgsizdvtvegiviceieddfdkfekvwyqtaqdf");
        });
        System.out.println("  refund response: " + refund);
        assertThat(refund.toBool("refunded")).isTrue();
        assertThat(refund.getStr("originalTransactionId")).isEqualTo(vendorRef);
        assertThat(refund.getStr("responseStatus")).isEqualTo("201 Created (simulated)");
        assertThat(refund.getStr("response")).isEqualTo("INS-0 Request processed successfully (simulated)");
        assertThat(refund.getStr("transactionStatus")).isEqualTo("Completed");
        assertThat(refund.getStr("transactionStatusResponse")).isEqualTo("INS-0 Request processed successfully (simulated)");
        assertThat(refund.getStr("reversalTransactionId")).isNotEmpty();
        assertThat(refund.getStr("reversalConversationId")).isNotEmpty();

        Wrapper paymentResponse = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/%s/response", vendorRef), null, headers -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
        });
        System.out.println("  final payment response: " + paymentResponse);
        assertThat(paymentResponse.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentResponse.getStr("status")).isEqualTo("ERROR");
        assertThat(paymentResponse.getStr("message")).startsWith("Manual refund");
        assertThat(paymentResponse.getStr("errorCode")).startsWith("111-IC1552-0006");
        assertThat(paymentResponse.getStr("date")).isNotEmpty();
        assertThat(paymentResponse.getStr("spResult.error")).isEqualTo("Transaction cancelled");

        Wrapper paymentDebug2 = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/mpesa/%s/debug", vendorRef), null, headers -> {
            headers.set("Api-Key", "nttzhxmyjarfegtvznkarjxvhgfmxhgsizdvtvegiviceieddfdkfekvwyqtaqdf");
        });
        System.out.println("  payment debug: " + paymentDebug2);
        assertThat(paymentDebug2.getStr("_vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug2.getObject("_handler.refunded")).isEqualTo(true);
        assertThat(paymentDebug2.getStr("_handler.refundResponseStatus")).isEqualTo("201 Created (simulated)");
        assertThat(paymentDebug2.getStr("_handler.refundResponse")).isEqualTo("INS-0 Request processed successfully (simulated)");
        assertThat(paymentDebug2.getStr("_handler.updatedTime")).isNotEmpty();
        assertThat(paymentDebug2.getStr("_handler.refundTransactionId")).startsWith("sim_");
        assertThat(paymentDebug2.getStr("_handler.refundConversationId")).isNotEmpty();
        assertThat(paymentDebug2.getStr("_handler.refundTransactionStatus")).isEqualTo("Completed");
        assertThat(paymentDebug2.getStr("_handler.refundTransactionStatusResponse")).isEqualTo("INS-0 Request processed successfully (simulated)");
        assertThat(paymentDebug2.getStr("_handler.refundTime")).isNotNull();
        assertThat(paymentDebug2.getObject("_response")).isNotNull();
        assertThat(paymentDebug2.getStr("_response.status")).isEqualTo("ERROR");
        assertThat(paymentDebug2.getStr("_response.message")).isEqualTo("Manual refund");
        assertThat(paymentDebug2.getStr("_response.errorCode")).isEqualTo("111-IC1552-0006");
        assertThat(paymentDebug2.getStr("_response.externalTransactionId")).startsWith("sim_");
        assertThat(paymentDebug2.getStr("_response.spResult.error")).startsWith("Transaction cancelled");
    }

    @Test
    public void testMpesaSpError() {
        String vendorRef = "e2e-" + UUID.randomUUID().toString().replace("-", "");
        Wrapper payment = rest.sendPostRequest(RestHelper.zimHost + "/api/v1/zim/payment/sync", (headers, body) -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
            body.put("vendorRef", vendorRef);
            body.put("bankName", "mpesa");
            body.put("accountNumber", "258843120879");
            body.put("amount", 10);
            body.put("metaData", Tool.newMetaMap()
                    .put("walletId", 99)
                    .put("transactionCode", "MTP")
                    .put("sessionId", 912984)
                    .put("channel", "WEB")
                    .put("accountId", 20528257)
                    .put("partnerId", 0)
                    .put("accountFundId", 20078466)
                    .put("cardNumber", "8811060000010302")
                    .put("paymentDescription", "Fund card")
                    .put("organisation", "Revimo")
                    .put("simulate", "SUCCESS")
                    .put("simulateErrorStep", 41)
                    .build());
        });
        System.out.println("  payment response: " + payment);
        assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment.getStr("status")).isEqualTo("ERROR");
        assertThat(payment.getStr("message")).isEqualTo("'p_Create_Transaction_Card' SP returned Result=2, Error: Simulated SP error");
        assertThat(payment.getStr("errorCode")).isEqualTo(ErrorCodes.EC1110);
        assertThat(payment.getStr("externalTransactionId")).startsWith("sim_");
        assertThat(payment.getInt("spTries")).isEqualTo(1);
        assertThat(payment.getStr("date")).isNotEmpty();
        assertThat(payment.getStr("spResult.spName")).isEqualTo("p_Create_Transaction_Card");
        assertThat(payment.getInt("spResult.result")).isEqualTo(2);

        Wrapper paymentDebug = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/mpesa/%s/debug", vendorRef), null, headers -> {
            headers.set("Api-Key", "nttzhxmyjarfegtvznkarjxvhgfmxhgsizdvtvegiviceieddfdkfekvwyqtaqdf");
        });
        System.out.println("  payment debug: " + paymentDebug);
        assertThat(paymentDebug.getStr("_vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.status")).isEqualTo("refunded");
        assertThat(paymentDebug.getObject("_handler.paymentSuccessful")).isEqualTo(true);
        assertThat(paymentDebug.getObject("_handler.refunded")).isEqualTo(true);
        assertThat(paymentDebug.getStr("_handler.refundResponseStatus")).isEqualTo("201 Created (simulated)");
        assertThat(paymentDebug.getStr("_handler.refundTime")).isNotNull();
    }

    @Test
    public void testMpesaSpErrorAndRefundError() {
        String vendorRef = "e2e-" + UUID.randomUUID().toString().replace("-", "");
        Wrapper payment = rest.sendPostRequest(RestHelper.zimHost + "/api/v1/zim/payment/sync", (headers, body) -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
            body.put("vendorRef", vendorRef);
            body.put("bankName", "mpesa");
            body.put("accountNumber", "258843120879");
            body.put("amount", 10);
            body.put("metaData", Tool.newMetaMap()
                    .put("walletId", 99)
                    .put("transactionCode", "MTP")
                    .put("sessionId", 912984)
                    .put("channel", "WEB")
                    .put("accountId", 20528257)
                    .put("partnerId", 0)
                    .put("accountFundId", 20078466)
                    .put("cardNumber", "8811060000010302")
                    .put("paymentDescription", "Fund card")
                    .put("organisation", "Revimo")
                    .put("simulate", "REFUND_ERROR")
                    .put("simulateErrorStep", 41)
                    .build());
        });
        System.out.println("  payment response: " + payment);
        assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment.getStr("status")).isEqualTo("ERROR_PARTIAL_PAYMENT");
        assertThat(payment.getStr("message")).isEqualTo("'p_Create_Transaction_Card' SP returned Result=2, Error: Simulated SP error");
        assertThat(payment.getStr("errorCode")).isEqualTo(ErrorCodes.EC1110);
        assertThat(payment.getStr("externalTransactionId")).startsWith("sim_");
        assertThat(payment.getInt("spTries")).isEqualTo(1);
        assertThat(payment.getStr("date")).isNotEmpty();
        assertThat(payment.getStr("spResult.spName")).isEqualTo("p_Create_Transaction_Card");
        assertThat(payment.getInt("spResult.result")).isEqualTo(2);

        Wrapper paymentDebug = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/mpesa/%s/debug", vendorRef), null, headers -> {
            headers.set("Api-Key", "nttzhxmyjarfegtvznkarjxvhgfmxhgsizdvtvegiviceieddfdkfekvwyqtaqdf");
        });
        System.out.println("  payment debug: " + paymentDebug);
        assertThat(paymentDebug.getStr("_vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.status")).isEqualTo("refundError");
        assertThat(paymentDebug.getObject("_handler.paymentSuccessful")).isEqualTo(true);
        assertThat(paymentDebug.getObject("_handler.refunded")).isEqualTo(false);
        assertThat(paymentDebug.getStr("_handler.refundTime")).isNotNull();
    }

    @Test
    public void testMpesaSpRetry() {
        String vendorRef = "e2e-" + UUID.randomUUID().toString().replace("-", "");
        Wrapper payment = rest.sendPostRequest(RestHelper.zimHost + "/api/v1/zim/payment/sync", (headers, body) -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
            body.put("vendorRef", vendorRef);
            body.put("bankName", "mpesa");
            body.put("accountNumber", "258843120879");
            body.put("amount", 10);
            body.put("metaData", Tool.newMetaMap()
                    .put("walletId", 99)
                    .put("transactionCode", "MTP")
                    .put("sessionId", 912984)
                    .put("channel", "WEB")
                    .put("accountId", 20528257)
                    .put("partnerId", 0)
                    .put("accountFundId", 20078466)
                    .put("cardNumber", "8811060000010302")
                    .put("paymentDescription", "Fund card")
                    .put("organisation", "Revimo")
                    .put("simulate", "SUCCESS")
                    .put("simulateErrorStep", 42)
                    .build());
        });
        System.out.println("  payment response: " + payment);
        assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment.getStr("status")).isEqualTo("SUCCESS");
        assertThat(payment.getStr("externalTransactionId")).startsWith("sim_");
        assertThat(payment.getStr("date")).isNotEmpty();
        assertThat(payment.getStr("spResult.spName")).isEqualTo("p_Create_Transaction_Card");
        assertThat(payment.getInt("spResult.transactionId")).isGreaterThan(0);
        assertThat(payment.getInt("spResult.drAccountId")).isEqualTo(20528257);
        assertThat(payment.getInt("spResult.crAccountId")).isGreaterThan(0);
        assertThat(payment.getDbl("spResult.balance")).isGreaterThan(0.0);
        assertThat(payment.getDbl("spResult.drFees")).isEqualTo(0.0);
        assertThat(payment.getInt("spResult.result")).isEqualTo(1);
        assertThat(payment.getStr("spResult.message")).isEqualTo("Funds successfully loaded. ");
        assertThat(payment.getStr("spResult.error")).isEqualTo(null);

        Wrapper paymentDebug = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/mpesa/%s/debug", vendorRef), null, headers -> {
            headers.set("Api-Key", "nttzhxmyjarfegtvznkarjxvhgfmxhgsizdvtvegiviceieddfdkfekvwyqtaqdf");
        });
        System.out.println("  payment debug: " + paymentDebug);
        assertThat(paymentDebug.getStr("_vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.status")).isEqualTo("success");
        assertThat(paymentDebug.getObject("_handler.paymentSuccessful")).isEqualTo(true);
        assertThat(paymentDebug.getObject("_handler.refunded")).isEqualTo(false);
    }

    @Test
    public void testMpesaSpAllRetriesFail() {
        String vendorRef = "e2e-" + UUID.randomUUID().toString().replace("-", "");
        Wrapper payment = rest.sendPostRequest(RestHelper.zimHost + "/api/v1/zim/payment/sync", (headers, body) -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
            body.put("vendorRef", vendorRef);
            body.put("bankName", "mpesa");
            body.put("accountNumber", "258843120879");
            body.put("amount", 10);
            body.put("metaData", Tool.newMetaMap()
                    .put("walletId", 99)
                    .put("transactionCode", "MTP")
                    .put("sessionId", 912984)
                    .put("channel", "WEB")
                    .put("accountId", 20528257)
                    .put("partnerId", 0)
                    .put("accountFundId", 20078466)
                    .put("cardNumber", "8811060000010302")
                    .put("paymentDescription", "Fund card")
                    .put("organisation", "Revimo")
                    .put("simulate", "SUCCESS")
                    .put("simulateErrorStep", 43)
                    .build());
        });
        System.out.println("  payment response: " + payment);
        assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment.getStr("status")).isEqualTo("ERROR");
        assertThat(payment.getStr("message")).isEqualTo("Approving SP polling attempts exceeded");
        assertThat(payment.getStr("errorCode")).isEqualTo(ErrorCodes.EC1111);
        assertThat(payment.getStr("externalTransactionId")).startsWith("sim_");
        assertThat(payment.getInt("spTries")).isEqualTo(3);
        assertThat(payment.getStr("date")).isNotEmpty();
        assertThat(payment.getStr("spResult.spName")).isEqualTo("p_Create_Transaction_Card");
        assertThat(payment.getObject("spResult.result")).isEqualTo(null);

        Wrapper paymentDebug = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/mpesa/%s/debug", vendorRef), null, headers -> {
            headers.set("Api-Key", "nttzhxmyjarfegtvznkarjxvhgfmxhgsizdvtvegiviceieddfdkfekvwyqtaqdf");
        });
        System.out.println("  payment debug: " + paymentDebug);
        assertThat(paymentDebug.getStr("_vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.status")).isEqualTo("refunded");
        assertThat(paymentDebug.getObject("_handler.paymentSuccessful")).isEqualTo(true);
        assertThat(paymentDebug.getObject("_handler.refunded")).isEqualTo(true);
        assertThat(paymentDebug.getStr("_handler.refundTime")).isNotNull();
    }

    @Test
    public void testMpesaSpRetryRecheck() {
        String vendorRef = "e2e-" + UUID.randomUUID().toString().replace("-", "");
        Wrapper payment = rest.sendPostRequest(RestHelper.zimHost + "/api/v1/zim/payment/sync", (headers, body) -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
            body.put("vendorRef", vendorRef);
            body.put("bankName", "mpesa");
            body.put("accountNumber", "258843120879");
            body.put("amount", 10);
            body.put("metaData", Tool.newMetaMap()
                    .put("walletId", 99)
                    .put("transactionCode", "MTP")
                    .put("sessionId", 912984)
                    .put("channel", "WEB")
                    .put("accountId", 20528257)
                    .put("partnerId", 0)
                    .put("accountFundId", 20078466)
                    .put("cardNumber", "8811060000010302")
                    .put("paymentDescription", "Fund card")
                    .put("organisation", "Revimo")
                    .put("simulate", "SUCCESS")
                    .put("simulateErrorStep", 44)
                    .build());
        });
        System.out.println("  payment response: " + payment);
        assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment.getStr("status")).isEqualTo("ERROR");
        assertThat(payment.getStr("message")).isEqualTo("Approving SP polling attempts exceeded");
        assertThat(payment.getStr("errorCode")).isEqualTo(ErrorCodes.EC1111);
        assertThat(payment.getStr("externalTransactionId")).startsWith("sim_");
        assertThat(payment.getInt("spTries")).isEqualTo(3);
        assertThat(payment.getStr("date")).isNotEmpty();
        assertThat(payment.getStr("spResult.spName")).isEqualTo("p_Create_Transaction_Card");
        assertThat(payment.getObject("spResult.result")).isEqualTo(null);

        Wrapper paymentDebug = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/mpesa/%s/debug", vendorRef), null, headers -> {
            headers.set("Api-Key", "nttzhxmyjarfegtvznkarjxvhgfmxhgsizdvtvegiviceieddfdkfekvwyqtaqdf");
        });
        System.out.println("  payment debug: " + paymentDebug);
        assertThat(paymentDebug.getStr("_vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.status")).isEqualTo("refunded");
        assertThat(paymentDebug.getObject("_handler.paymentSuccessful")).isEqualTo(true);
        assertThat(paymentDebug.getObject("_handler.refunded")).isEqualTo(true);
        assertThat(paymentDebug.getStr("_handler.refundTime")).isNotNull();
    }


    @Test
    public void testMpesaStatusQueryDelay() {
        String vendorRef = "e2e-" + UUID.randomUUID().toString().replace("-", "");
        Wrapper payment = rest.sendPostRequest(RestHelper.zimHost + "/api/v1/zim/payment/sync", (headers, body) -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
            body.put("vendorRef", vendorRef);
            body.put("bankName", "mpesa");
            body.put("accountNumber", "258843120879");
            body.put("amount", 10);
            body.put("metaData", Tool.newMetaMap()
                    .put("walletId", 99)
                    .put("transactionCode", "MTP")
                    .put("sessionId", 912984)
                    .put("channel", "WEB")
                    .put("accountId", 20528257)
                    .put("partnerId", 0)
                    .put("accountFundId", 20078466)
                    .put("cardNumber", "8811060000010302")
                    .put("paymentDescription", "Fund card")
                    .put("organisation", "Revimo")
                    .put("simulate", "INBOUND_UNANSWERED")
                    .build());
        });
        System.out.println("  payment response: " + payment);
        assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment.getStr("status")).isEqualTo("ERROR");
        assertThat(payment.getStr("message")).isEqualTo("Received error from external mpesa server: no response");
        assertThat(payment.getStr("errorCode")).isEqualTo(ErrorCodes.EC9003);
        assertThat(payment.getStr("externalTransactionId")).isNull();
        assertThat(payment.getInt("spTries")).isEqualTo(0);
        assertThat(payment.getStr("date")).isNotEmpty();
        assertThat(payment.getStr("spResult")).isNull();

        Wrapper paymentDebug = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/mpesa/%s/debug", vendorRef), null, headers -> {
            headers.set("Api-Key", "nttzhxmyjarfegtvznkarjxvhgfmxhgsizdvtvegiviceieddfdkfekvwyqtaqdf");
        });
        System.out.println("  payment debug: " + paymentDebug);
        assertThat(paymentDebug.getStr("_vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.status")).isEqualTo("error");
        assertThat(paymentDebug.getStr("_handler.responseStatus")).isEqualTo("no response");
        assertThat(paymentDebug.getStr("_handler.transactionStatusResponse")).isEqualTo("INS-6 Transaction Failed (simulated)");
        assertThat(paymentDebug.getObject("_handler.paymentSuccessful")).isEqualTo(false);
        assertThat(paymentDebug.getObject("_handler.refunded")).isEqualTo(false);
        assertThat(paymentDebug.getStr("_handler.createdTime")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.updatedTime")).isNotNull();
    }

    @Test
    public void testMpesaErrorPaymentBeforeApprove() {
        String vendorRef = "e2e-" + UUID.randomUUID().toString().replace("-", "");
        Wrapper payment = rest.sendPostRequest(RestHelper.zimHost + "/api/v1/zim/payment/sync", (headers, body) -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
            body.put("vendorRef", vendorRef);
            body.put("bankName", "mpesa");
            body.put("accountNumber", "258843120879");
            body.put("amount", 10);
            body.put("metaData", Tool.newMetaMap()
                    .put("walletId", 99)
                    .put("transactionCode", "MTP")
                    .put("sessionId", 912984)
                    .put("channel", "WEB")
                    .put("accountId", 20528257)
                    .put("partnerId", 0)
                    .put("accountFundId", 20078466)
                    .put("cardNumber", "8811060000010302")
                    .put("paymentDescription", "Fund card")
                    .put("organisation", "Revimo")
                    .put("simulate", "SUCCESS")
                    .put("simulateErrorStep", 11)
                    .build());
        });
        System.out.println("  payment response: " + payment);
        assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment.getStr("status")).isEqualTo("ERROR");
        assertThat(payment.getStr("message")).isEqualTo("Simulated error on step: 11");
        assertThat(payment.getStr("errorCode")).isEqualTo("111-IC1552-0009");
        assertThat(payment.getStr("externalTransactionId")).isNull();
        assertThat(payment.getStr("date")).isNotEmpty();
        assertThat(payment.getObject("spResult")).isNull();

        Wrapper paymentDebug = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/mpesa/%s/debug", vendorRef), null, headers -> {
            headers.set("Api-Key", "nttzhxmyjarfegtvznkarjxvhgfmxhgsizdvtvegiviceieddfdkfekvwyqtaqdf");
        });
        System.out.println("  payment debug: " + paymentDebug);
        assertThat(paymentDebug.getStr("_vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getObject("_requestTime")).isNotNull();
        assertThat(paymentDebug.getObject("_responseTime")).isNotNull();
        assertThat(paymentDebug.getObject("_expiration")).isNotNull();
        assertThat(paymentDebug.getObject("_request")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.status")).isEqualTo("refunded");
        assertThat(paymentDebug.getStr("_handler.responseStatus")).isEqualTo("201 Created (simulated)");
        assertThat(paymentDebug.getStr("_handler.responseCode")).isEqualTo("INS-0");
        assertThat(paymentDebug.getStr("_handler.responseDesc")).isEqualTo("Request processed successfully (simulated)");
        assertThat(paymentDebug.getStr("_handler.transactionStatus")).isEqualTo("Completed");
        assertThat(paymentDebug.getStr("_handler.transactionStatusLine")).isEqualTo("HTTP/1.1 201 Created");
        assertThat(paymentDebug.getStr("_handler.transactionStatusResponse")).isEqualTo("INS-0 Request processed successfully (simulated)");
        assertThat(paymentDebug.getStr("_handler.errorCode")).isNull();
        assertThat(paymentDebug.getStr("_handler.errorMessage")).isNull();
        assertThat(paymentDebug.getObject("_handler.refunded")).isEqualTo(true);
        assertThat(paymentDebug.getStr("_handler.refundResponseStatus")).isEqualTo("201 Created (simulated)");
        assertThat(paymentDebug.getStr("_handler.refundResponse")).isEqualTo("INS-0 Request processed successfully (simulated)");
        assertThat(paymentDebug.getStr("_handler.refundTransactionId")).startsWith("sim_");
        assertThat(paymentDebug.getStr("_handler.refundConversationId")).isNotEmpty();
        assertThat(paymentDebug.getStr("_handler.refundTransactionStatus")).isEqualTo("Completed");
        assertThat(paymentDebug.getStr("_handler.refundTransactionStatusResponse")).isEqualTo("INS-0 Request processed successfully (simulated)");
        assertThat(paymentDebug.getStr("_handler.refundTime")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.updatedTime")).isNotEmpty();
        assertThat(paymentDebug.getObject("_response")).isNotNull();
        assertThat(paymentDebug.getStr("_response.status")).isEqualTo("ERROR");
        assertThat(paymentDebug.getStr("_response.message")).isEqualTo("Simulated error on step: 11");
        assertThat(paymentDebug.getStr("_response.errorCode")).isEqualTo("111-IC1552-0009");
        assertThat(paymentDebug.getStr("_response.spResult")).isNull();
    }

    @Test
    public void testMpesaErrorPaymentAfterApprove() {
        String vendorRef = "e2e-" + UUID.randomUUID().toString().replace("-", "");
        Wrapper payment = rest.sendPostRequest(RestHelper.zimHost + "/api/v1/zim/payment/sync", (headers, body) -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
            body.put("vendorRef", vendorRef);
            body.put("bankName", "mpesa");
            body.put("accountNumber", "258843120879");
            body.put("amount", 10);
            body.put("metaData", Tool.newMetaMap()
                    .put("walletId", 99)
                    .put("transactionCode", "MTP")
                    .put("sessionId", 912984)
                    .put("channel", "WEB")
                    .put("accountId", 20528257)
                    .put("partnerId", 0)
                    .put("accountFundId", 20078466)
                    .put("cardNumber", "8811060000010302")
                    .put("paymentDescription", "Fund card")
                    .put("organisation", "Revimo")
                    .put("simulate", "SUCCESS")
                    .put("simulateErrorStep", 14)
                    .build());
        });
        System.out.println("  payment response: " + payment);
        assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment.getStr("status")).isEqualTo("ERROR");
        assertThat(payment.getStr("message")).isEqualTo("Simulated error on step: 14");
        assertThat(payment.getStr("errorCode")).isEqualTo("111-IC1552-0009");
        assertThat(payment.getStr("externalTransactionId")).isNull();
        assertThat(payment.getStr("date")).isNotEmpty();
        assertThat(payment.getStr("spResult.spName")).isEqualTo("p_Create_Transaction_Card");
        assertThat(payment.getInt("spResult.transactionId")).isGreaterThan(0);
        assertThat(payment.getInt("spResult.result")).isEqualTo(1);
        assertThat(payment.getStr("spResult.message")).isEqualTo("Funds successfully loaded. ");
        assertThat(payment.getStr("spResult.error")).isEqualTo("Transaction cancelled");

        Wrapper paymentDebug = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/mpesa/%s/debug", vendorRef), null, headers -> {
            headers.set("Api-Key", "nttzhxmyjarfegtvznkarjxvhgfmxhgsizdvtvegiviceieddfdkfekvwyqtaqdf");
        });
        System.out.println("  payment debug: " + paymentDebug);
        assertThat(paymentDebug.getStr("_vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getObject("_requestTime")).isNotNull();
        assertThat(paymentDebug.getObject("_responseTime")).isNotNull();
        assertThat(paymentDebug.getObject("_expiration")).isNotNull();
        assertThat(paymentDebug.getObject("_request")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.status")).isEqualTo("refunded");
        assertThat(paymentDebug.getStr("_handler.responseStatus")).isEqualTo("201 Created (simulated)");
        assertThat(paymentDebug.getStr("_handler.responseCode")).isEqualTo("INS-0");
        assertThat(paymentDebug.getStr("_handler.responseDesc")).isEqualTo("Request processed successfully (simulated)");
        assertThat(paymentDebug.getStr("_handler.transactionStatus")).isEqualTo("Completed");
        assertThat(paymentDebug.getStr("_handler.transactionStatusLine")).isEqualTo("HTTP/1.1 201 Created");
        assertThat(paymentDebug.getStr("_handler.transactionStatusResponse")).isEqualTo("INS-0 Request processed successfully (simulated)");
        assertThat(paymentDebug.getStr("_handler.errorCode")).isNull();
        assertThat(paymentDebug.getStr("_handler.errorMessage")).isNull();
        assertThat(paymentDebug.getObject("_handler.refunded")).isEqualTo(true);
        assertThat(paymentDebug.getStr("_handler.refundResponseStatus")).isEqualTo("201 Created (simulated)");
        assertThat(paymentDebug.getStr("_handler.refundResponse")).isEqualTo("INS-0 Request processed successfully (simulated)");
        assertThat(paymentDebug.getStr("_handler.refundTransactionId")).startsWith("sim_");
        assertThat(paymentDebug.getStr("_handler.refundConversationId")).isNotEmpty();
        assertThat(paymentDebug.getStr("_handler.refundTransactionStatus")).isEqualTo("Completed");
        assertThat(paymentDebug.getStr("_handler.refundTransactionStatusResponse")).isEqualTo("INS-0 Request processed successfully (simulated)");
        assertThat(paymentDebug.getStr("_handler.refundTime")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.updatedTime")).isNotEmpty();
        assertThat(paymentDebug.getObject("_response")).isNotNull();
        assertThat(paymentDebug.getStr("_response.status")).isEqualTo("ERROR");
        assertThat(paymentDebug.getStr("_response.message")).isEqualTo("Simulated error on step: 14");
        assertThat(paymentDebug.getStr("_response.errorCode")).isEqualTo("111-IC1552-0009");
        assertThat(paymentDebug.getObject("_response.spResult")).isNotNull();
    }
}

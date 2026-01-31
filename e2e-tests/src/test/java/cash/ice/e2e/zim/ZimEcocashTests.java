package cash.ice.e2e.zim;

import cash.ice.RestHelper;
import cash.ice.Wrapper;
import cash.ice.common.utils.Tool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ZimEcocashTests {

    private static final int PAYMENT_ID = 101;
    private static final String MOBILE = "263788786951";
    private static final String PAYMENT_DESCRIPTION = "some description";
    private static final double AMOUNT = 0.20;
    private static final String ACCOUNT_NUMBER = "36870063298";
    private final RestHelper rest = new RestHelper();

    @Test
    public void testEcocashSuccessfulPayment() {
        String vendorRef = "e2e-" + UUID.randomUUID().toString().replace("-", "");
        Wrapper payment = rest.sendPostRequest(RestHelper.zimHost + "/api/v1/zim/payment/sync", (headers, body) -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
            body.put("vendorRef", vendorRef);
            body.put("bankName", "ecocash");
            body.put("accountNumber", MOBILE);
            body.put("amount", AMOUNT);
            body.put("metaData", Tool.newMetaMap()
                    .put("paymentId", PAYMENT_ID)
                    .put("approvePaymentFlat", true)
                    .put("transactionCode", "EPAYG")
                    .put("accountNumber", ACCOUNT_NUMBER)
                    .put("description", PAYMENT_DESCRIPTION)
                    .put("simulate", "SUCCESS")
                    .put("simulateDb", "all")
                    .build());
        });
        System.out.println("  payment response: " + payment);
        assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment.getStr("status")).isEqualTo("SUCCESS");
        assertThat(payment.getStr("externalTransactionId")).startsWith("SIMULATED.");
        assertThat(payment.getStr("date")).isNotEmpty();
        assertThat(payment.getStr("spResult.spName")).isEqualTo("p_Payment_Approval");
        assertThat(payment.getInt("spResult.transactionId")).isGreaterThan(0);
        assertThat(payment.getInt("spResult.result")).isEqualTo(1);
        assertThat(payment.getStr("spResult.message")).isEqualTo("Approved (simulated)");
        assertThat(payment.getStr("spResult.error")).isEqualTo(null);

        Wrapper paymentDebug = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/ecocash/%s/debug", vendorRef), null, headers -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
        });
        System.out.println("  payment debug: " + paymentDebug);
        assertThat(paymentDebug.getStr("_vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getObject("_requestTime")).isNotNull();
        assertThat(paymentDebug.getObject("_responseTime")).isNotNull();
        assertThat(paymentDebug.getObject("_expiration")).isNotNull();
        assertThat(paymentDebug.getObject("_request")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getObject("_handler.pendingPayment")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.status")).isEqualTo("success");
        assertThat(paymentDebug.getStr("_handler.ecocashReference")).startsWith("SIMULATED.");
        assertThat(paymentDebug.getStr("_handler.endUserId")).isEqualTo(MOBILE.substring(3));
        assertThat(paymentDebug.getStr("_handler.clientCorrelator")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.transactionOperationStatus")).isEqualTo("COMPLETED");
        assertThat(paymentDebug.getStr("_handler.reason")).isNull();
        assertThat(paymentDebug.getStr("_handler.errorCode")).isNull();
        assertThat(paymentDebug.toBool("_handler.finishedPayment")).isTrue();
        assertThat(paymentDebug.getStr("_handler.recheck")).isNull();
        assertThat(paymentDebug.getStr("_handler.refundFailed")).isNull();
        assertThat(paymentDebug.getStr("_handler.createdTime")).isNotBlank();
        assertThat(paymentDebug.getStr("_handler.updatedTime")).isNotBlank();
        assertThat(paymentDebug.getStr("_handler.refundedTime")).isNull();
        assertThat(paymentDebug.getObject("_handler.request")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.request.clientCorrelator")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.request.notifyUrl")).isEqualTo(RestHelper.ecocashHost + "/ecocash/api/callback");
        assertThat(paymentDebug.getStr("_handler.request.referenceCode")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.request.tranType")).isEqualTo("MER");
        assertThat(paymentDebug.getStr("_handler.request.endUserId")).isEqualTo(MOBILE.substring(3));
        assertThat(paymentDebug.getStr("_handler.request.transactionOperationStatus")).isEqualTo("Charged");
        assertThat(paymentDebug.getDbl("_handler.request.paymentAmount.charginginformation.amount")).isEqualTo(AMOUNT);
        assertThat(paymentDebug.getStr("_handler.request.paymentAmount.charginginformation.currency")).isEqualTo("ZWL");
        assertThat(paymentDebug.getStr("_handler.request.paymentAmount.charginginformation.description")).isEqualTo(PAYMENT_DESCRIPTION);
        assertThat(paymentDebug.getStr("_handler.request.paymentAmount.chargeMetaData.channel")).isEqualTo("WEB");
        assertThat(paymentDebug.getStr("_handler.request.paymentAmount.chargeMetaData.purchaseCategoryCode")).isEqualTo("Online Payment");
        assertThat(paymentDebug.getStr("_handler.request.paymentAmount.chargeMetaData.onBeHalfOf")).isEqualTo("Paynow Topup");
        assertThat(paymentDebug.getStr("_handler.request.currencyCode")).isEqualTo("ZWL");
        assertThat(paymentDebug.getStr("_handler.request.countryCode")).isEqualTo("ZW");
        assertThat(paymentDebug.getStr("_handler.request.terminalId")).isEqualTo("202");
        assertThat(paymentDebug.getStr("_handler.request.location")).isEqualTo("77 Coventry Road Harare");
        assertThat(paymentDebug.getStr("_handler.request.superMerchantName")).isEqualTo("ICEcash");
        assertThat(paymentDebug.getStr("_handler.request.merchantName")).isEqualTo("ICEcash");
        assertThat(paymentDebug.getStr("_handler.request.merchantCode")).isEqualTo("02273");
        assertThat(paymentDebug.getStr("_handler.request.merchantPin")).isEqualTo("1357");
        assertThat(paymentDebug.getStr("_handler.request.merchantNumber")).isEqualTo("771998182");
        assertThat(paymentDebug.getStr("_handler.request.remarks")).isEqualTo("ICECash");
        assertThat(paymentDebug.getObject("_handler.initialResponse")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.initialResponse.transactionOperationStatus")).isEqualTo("COMPLETED");
        assertThat(paymentDebug.getStr("_handler.initialResponse.ecocashReference")).startsWith("SIMULATED.");
        assertThat(paymentDebug.getObject("_handler.finalResponse")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.finalResponse.transactionOperationStatus")).isEqualTo("COMPLETED");
        assertThat(paymentDebug.getObject("_response")).isNotNull();
        assertThat(paymentDebug.getStr("_response.vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_response.status")).isEqualTo("SUCCESS");
        assertThat(paymentDebug.getStr("_response.externalTransactionId")).startsWith("SIMULATED.");

        // refund
        Wrapper refund = rest.sendPostRequest(String.format("%s/api/v1/zim/payment/ecocash/%s/reversal", RestHelper.zimHost, vendorRef), (headers, body) -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
        });
        System.out.println("  refund response: " + refund);
        assertThat(refund.toBool("refunded")).isTrue();
        assertThat(refund.getStr("originalEcocashReference")).startsWith("SIMULATED.");
        assertThat(refund.getStr("transactionOperationStatus")).isEqualTo("COMPLETED");
        assertThat(refund.getStr("clientCorrelator")).isNotBlank();
        assertThat(refund.getStr("time")).isNotBlank();

        Wrapper paymentResponse = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/%s/response", vendorRef), null, headers -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
        });
        System.out.println("  final payment response: " + paymentResponse);
        assertThat(paymentResponse.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentResponse.getStr("status")).isEqualTo("ERROR");
        assertThat(paymentResponse.getStr("message")).startsWith("Manual refund");
        assertThat(paymentResponse.getStr("errorCode")).startsWith("111-IC1552-0006");
        assertThat(paymentResponse.getStr("date")).isNotEmpty();
        assertThat(paymentResponse.getStr("externalTransactionId")).startsWith("SIMULATED.");
        assertThat(paymentResponse.getStr("spResult.spName")).isEqualTo("p_Payment_Reversal");
        assertThat(paymentResponse.getInt("spResult.result")).isEqualTo(1);
        assertThat(paymentResponse.getStr("spResult.message")).isEqualTo("Simulated transaction cancel");

        Wrapper paymentDebug2 = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/ecocash/%s/debug", vendorRef), null, headers -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
        });
        System.out.println("  payment debug: " + paymentDebug2);
        assertThat(paymentDebug2.getStr("_vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug2.getStr("_handler.status")).isEqualTo("refunded");
        assertThat(paymentDebug2.getObject("_handler.refundFailed")).isNull();
        assertThat(paymentDebug2.getObject("_handler.refundRequest")).isNotNull();
        assertThat(paymentDebug2.getStr("_handler.refundRequest.clientCorrelator")).isNotBlank();
        assertThat(paymentDebug2.getStr("_handler.refundRequest.referenceCode")).isEqualTo(vendorRef);
        assertThat(paymentDebug2.getStr("_handler.refundRequest.tranType")).isEqualTo("REF");
        assertThat(paymentDebug2.getStr("_handler.refundRequest.endUserId")).isEqualTo(MOBILE.substring(3));
        assertThat(paymentDebug2.getDbl("_handler.refundRequest.paymentAmount.charginginformation.amount")).isEqualTo(AMOUNT);
        assertThat(paymentDebug2.getObject("_handler.refundResponse")).isNotNull();
        assertThat(paymentDebug2.getStr("_handler.refundResponse.referenceCode")).isEqualTo(vendorRef);
        assertThat(paymentDebug2.getStr("_handler.refundResponse.transactionOperationStatus")).isEqualTo("COMPLETED");
        assertThat(paymentDebug2.getStr("_handler.refundedTime")).isNotBlank();
        assertThat(paymentDebug2.getObject("_response")).isNotNull();
        assertThat(paymentDebug2.getStr("_response.status")).isEqualTo("ERROR");
        assertThat(paymentDebug2.getStr("_response.message")).isEqualTo("Manual refund");
        assertThat(paymentDebug2.getStr("_response.errorCode")).isEqualTo("111-IC1552-0006");
        assertThat(paymentDebug2.getStr("_response.externalTransactionId")).startsWith("SIMULATED.");
        assertThat(paymentDebug2.getStr("_response.spResult.message")).startsWith("Simulated transaction cancel");
    }

    @Test
    public void testEcocashFailWithRefund() {
        String vendorRef = "e2e-" + UUID.randomUUID().toString().replace("-", "");
        Wrapper payment = rest.sendPostRequest(RestHelper.zimHost + "/api/v1/zim/payment/sync", (headers, body) -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
            body.put("vendorRef", vendorRef);
            body.put("bankName", "ecocash");
            body.put("accountNumber", MOBILE);
            body.put("amount", AMOUNT);
            body.put("metaData", Tool.newMetaMap()
                    .put("paymentId", PAYMENT_ID)
                    .put("approvePaymentFlat", true)
                    .put("transactionCode", "EPAYG")
                    .put("accountNumber", ACCOUNT_NUMBER)
                    .put("description", PAYMENT_DESCRIPTION)
                    .put("simulate", "SUCCESS")
                    .put("simulateEcocashFirstResponse", "COMPLETED")
                    .put("simulateErrorStep", 11)
                    .put("simulateDb", "all")
                    .build());
        });
        System.out.println("  payment response: " + payment);
        assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment.getStr("status")).isEqualTo("ERROR");
        assertThat(payment.getStr("message")).startsWith("Simulated error on step: 11");
        assertThat(payment.getStr("errorCode")).startsWith("111-IC1552-0009");
        assertThat(payment.getStr("date")).isNotEmpty();
        assertThat(payment.getStr("spResult.spName")).isEqualTo("p_Payment_Approval");
        assertThat(payment.getStr("spResult.error")).isEqualTo("Simulated transaction fail");

        Wrapper paymentDebug = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/ecocash/%s/debug", vendorRef), null, headers -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
        });
        System.out.println("  payment debug: " + paymentDebug);
        assertThat(paymentDebug.getStr("_vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getObject("_handler.pendingPayment")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.status")).isEqualTo("refunded");
        assertThat(paymentDebug.getObject("_handler.refundFailed")).isNull();
        assertThat(paymentDebug.getObject("_handler.refundRequest")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.refundRequest.clientCorrelator")).isNotBlank();
        assertThat(paymentDebug.getStr("_handler.refundRequest.referenceCode")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.refundRequest.tranType")).isEqualTo("REF");
        assertThat(paymentDebug.getStr("_handler.refundRequest.endUserId")).isEqualTo(MOBILE.substring(3));
        assertThat(paymentDebug.getDbl("_handler.refundRequest.paymentAmount.charginginformation.amount")).isEqualTo(AMOUNT);
        assertThat(paymentDebug.getObject("_handler.refundResponse")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.refundResponse.referenceCode")).isEqualTo(vendorRef);
        assertThat(paymentDebug.getStr("_handler.refundResponse.transactionOperationStatus")).isEqualTo("COMPLETED");
        assertThat(paymentDebug.getStr("_handler.refundedTime")).isNotBlank();
    }

    @Test
    public void testEcocashCheckStatus() {
        String vendorRef = "e2e-" + UUID.randomUUID().toString().replace("-", "");
        Wrapper payment = rest.sendPostRequest(RestHelper.zimHost + "/api/v1/zim/payment/sync", (headers, body) -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
            body.put("vendorRef", vendorRef);
            body.put("bankName", "ecocash");
            body.put("accountNumber", MOBILE);
            body.put("amount", AMOUNT);
            body.put("metaData", Tool.newMetaMap()
                    .put("paymentId", PAYMENT_ID)
                    .put("approvePaymentFlat", true)
                    .put("transactionCode", "EPAYG")
                    .put("accountNumber", ACCOUNT_NUMBER)
                    .put("description", PAYMENT_DESCRIPTION)
                    .put("simulate", "SUCCESS")
                    .put("simulateEcocashFirstResponse", "PENDING SUBSCRIBER VALIDATION")
                    .put("simulateEcocashCheckStatus", "COMPLETED")
                    .put("simulateDb", "all")
                    .build());
        });
        System.out.println("  payment response: " + payment);
        assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment.getStr("status")).isEqualTo("SUCCESS");
        assertThat(payment.getStr("externalTransactionId")).startsWith("SIMULATED.");
        assertThat(payment.getStr("date")).isNotEmpty();
        assertThat(payment.getStr("spResult.spName")).isEqualTo("p_Payment_Approval");
        assertThat(payment.getInt("spResult.transactionId")).isGreaterThan(0);
        assertThat(payment.getInt("spResult.result")).isEqualTo(1);
        assertThat(payment.getStr("spResult.message")).isEqualTo("Approved (simulated)");
        assertThat(payment.getStr("spResult.error")).isEqualTo(null);
    }

    @Test
    public void testEcocashCallback() {
        String vendorRef = "e2e-" + UUID.randomUUID().toString().replace("-", "");
        Wrapper payment = rest.sendPostRequest(RestHelper.zimHost + "/api/v1/zim/payment", (headers, body) -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
            body.put("vendorRef", vendorRef);
            body.put("bankName", "ecocash");
            body.put("accountNumber", MOBILE);
            body.put("amount", AMOUNT);
            body.put("metaData", Tool.newMetaMap()
                    .put("paymentId", PAYMENT_ID)
                    .put("approvePaymentFlat", true)
                    .put("transactionCode", "EPAYG")
                    .put("accountNumber", ACCOUNT_NUMBER)
                    .put("description", PAYMENT_DESCRIPTION)
                    .put("simulate", "SUCCESS")
                    .put("simulateEcocashFirstResponse", "PENDING SUBSCRIBER VALIDATION")
                    .put("simulateDb", "all")
                    .build());
        });
        System.out.println("  payment response: " + payment);
        assertThat(payment.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(payment.getStr("status")).isEqualTo("PROCESSING");
        assertThat(payment.getStr("date")).isNotEmpty();


        Wrapper paymentDbg = null;
        for (int i = 0; i < 10; i++) {
            Tool.sleep(1000);
            paymentDbg = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/ecocash/%s/debug", vendorRef), null, headers -> {
                headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
            });
            System.out.println("  payment debug: " + paymentDbg);
            if (paymentDbg.getObject("_handler.initialResponse") != null) {
                break;
            }
        }
        Wrapper paymentDebug = paymentDbg;
        assertThat(paymentDebug.getObject("_handler.request")).isNotNull();
        assertThat(paymentDebug.getObject("_handler.initialResponse")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.status")).isEqualTo("sent");
        assertThat(paymentDebug.getStr("_handler.request.notifyUrl")).isEqualTo(RestHelper.ecocashHost + "/ecocash/api/callback");
        assertThat(paymentDebug.getStr("_handler.request.clientCorrelator")).isNotNull();
        assertThat(paymentDebug.getStr("_handler.initialResponse.transactionOperationStatus")).isEqualTo("PENDING SUBSCRIBER VALIDATION");
        assertThat(paymentDebug.getStr("_handler.initialResponse.ecocashReference")).startsWith("SIMULATED.");

        // callback
        rest.sendPostRequest(paymentDebug.getStr("_handler.request.notifyUrl"), (headers, body) -> {
            body.put("tranType", "MER");
            body.put("clientCorrelator", paymentDebug.getStr("_handler.request.clientCorrelator"));
            body.put("transactionOperationStatus", "COMPLETED");
            body.put("ecocashReference", paymentDebug.getStr("_handler.initialResponse.ecocashReference"));
            body.put("endUserId", paymentDebug.getStr("_handler.request.endUserId"));
            body.put("orginalMerchantReference", "SomeOriginalMerchantReference");
            body.put("paymentAmount", Map.of("totalAmountCharged", AMOUNT, "charginginformation", Map.of("amount", AMOUNT)));
        });

        Wrapper paymentDebug2 = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/ecocash/%s/debug", vendorRef), null, headers -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
        });
        System.out.println("  payment debug2: " + paymentDebug);
        assertThat(paymentDebug2.getObject("_handler.request")).isNotNull();
        assertThat(paymentDebug2.getStr("_handler.request.transactionOperationStatus")).isEqualTo("Charged");

        Wrapper paymentResponse = rest.sendGetRequest(RestHelper.zimHost + String.format("/api/v1/zim/payment/%s/response", vendorRef), null, headers -> {
            headers.set("Api-Key", "fd836ed0521558b8c18d1bff976b41d3985e12b51078b53e4ddcaac1e16547cc");
        });
        System.out.println("  final payment response: " + paymentResponse);
        assertThat(paymentResponse.getStr("vendorRef")).isEqualTo(vendorRef);
        assertThat(paymentResponse.getStr("status")).isEqualTo("SUCCESS");
        assertThat(paymentResponse.getStr("externalTransactionId")).startsWith("SIMULATED.");
        assertThat(paymentResponse.getStr("date")).isNotEmpty();
        assertThat(paymentResponse.getStr("spResult.spName")).isEqualTo("p_Payment_Approval");
        assertThat(paymentResponse.getInt("spResult.result")).isEqualTo(1);
        assertThat(paymentResponse.getStr("spResult.message")).isEqualTo("Approved (simulated)");
        assertThat(paymentResponse.getStr("spResult.error")).isNull();
    }
}

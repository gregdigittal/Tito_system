package cash.ice.zim.api.controller;

import cash.ice.common.dto.zim.PaymentOtpRequestZim;
import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.common.utils.Tool;
import cash.ice.zim.api.config.ZimApiProperties;
import cash.ice.zim.api.dto.PaymentResponseZim;
import cash.ice.zim.api.error.ZimApiExceptionHandler;
import cash.ice.zim.api.service.ZimLoggerService;
import cash.ice.zim.api.service.ZimPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static cash.ice.zim.api.documentation.IcecashEndpointDocumentation.endpoint;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ZimPaymentRestController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {ZimPaymentRestController.class, ZimApiExceptionHandler.class})
@AutoConfigureRestDocs(outputDir = "target/generated-snippets", uriScheme = "https", uriHost = "uat-zw-api.icecash.mobi", uriPort = 80)
class ZimPaymentControllerTest {
    private static final String VENDOR_REF = "vendorRefTest1";

    @MockBean
    private ZimPaymentService paymentService;
    @MockBean
    private ZimLoggerService loggerService;
    @MockBean
    private ZimApiProperties zimApiProperties;
    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testMpesaSyncPayment() throws Exception {
        String jsonRequest = Tool.readResourceAsString("data/json/mpesaPaymentRequest.json");
        String jsonResponse = Tool.readResourceAsString("data/json/mpesaPaymentResponse.json");
        PaymentRequestZim paymentRequest = createObjectMapper().readValue(jsonRequest, PaymentRequestZim.class);
        PaymentResponseZim paymentResponse = createObjectMapper().readValue(jsonResponse, PaymentResponseZim.class);
        when(paymentService.makePaymentSync(paymentRequest)).thenReturn(paymentResponse);

        mockMvc.perform(post("/api/v1/zim/payment/sync").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("mpesa-payment/sync-post", endpoint(),
                        requestFields(
                                fieldWithPath("vendorRef").description("Unique payment identifier, Required"),
                                fieldWithPath("bankName").description("Bank or External provider name (supported by API), Currently supported names: `mpesa`, Required"),
                                fieldWithPath("accountNumber").description("Account number withing bank (or external provider), Required"),
                                fieldWithPath("amount").description("Amount of payment, Required"),
                                fieldWithPath("metaData").description("Additional payment meta data"),
                                fieldWithPath("metaData.walletId").description("Payment Wallet ID, Required"),
                                fieldWithPath("metaData.transactionCode").description("Payment Transaction Code, Required"),
                                fieldWithPath("metaData.sessionId").description("Session ID, Required"),
                                fieldWithPath("metaData.channel").description("Channel, Required"),
                                fieldWithPath("metaData.accountId").description("Crediting Account ID, Required"),
                                fieldWithPath("metaData.partnerId").description("Partner ID, Required"),
                                fieldWithPath("metaData.accountFundId").description("Account Fund ID, Required"),
                                fieldWithPath("metaData.cardNumber").description("Card Number, Required"),
                                fieldWithPath("metaData.paymentDescription").description("Payment Description, Required"),
                                fieldWithPath("metaData.organisation").description("Organisation, Required")
                        ),
                        responseFields(
                                fieldWithPath("vendorRef").description("Unique payment identifier"),
                                fieldWithPath("status").description("Payment status, may be one of: PROCESSING, OTP_WAITING, SUCCESS, ERROR"),
                                fieldWithPath("externalTransactionId").description("Transaction ID returned from external provider (or bank). Non-null only on successful payment"),
                                fieldWithPath("date").description("Date of the transaction"),
                                fieldWithPath("spTries").description("Approve SP attempts amount"),
                                fieldWithPath("spResult").description("Result of stored procedure call"),
                                fieldWithPath("spResult.spName").description("Name of SP ('p_Create_Transactions_Card' value for mpesa MTP payment)"),
                                fieldWithPath("spResult.transactionId").description("Transaction ID returned from SP"),
                                fieldWithPath("spResult.drAccountId").description("Debit Account ID returned from SP"),
                                fieldWithPath("spResult.crAccountId").description("Credit Account ID returned from SP"),
                                fieldWithPath("spResult.balance").description("Current balance returned from SP"),
                                fieldWithPath("spResult.drFees").description("Debit Fees returned from SP"),
                                fieldWithPath("spResult.result").description("Result returned from SP (1 - success, 0 - error)"),
                                fieldWithPath("spResult.message").description("Message returned from SP ('Funds successfully loaded. ' - for successful payment)"),
                                fieldWithPath("spResult.error").description("Error message returned from SP")
                        )
                ));
    }

    @Test
    void testMpesaAsyncPayment() throws Exception {
        String jsonRequest = Tool.readResourceAsString("data/json/mpesaPaymentRequest.json");
        String jsonResponse = Tool.readResourceAsString("data/json/mpesaPaymentResponse.json");
        PaymentRequestZim paymentRequest = createObjectMapper().readValue(jsonRequest, PaymentRequestZim.class);
        PaymentResponseZim paymentResponse = createObjectMapper().readValue(jsonResponse, PaymentResponseZim.class);
        when(paymentService.makePaymentSync(paymentRequest)).thenReturn(paymentResponse);

        mockMvc.perform(post("/api/v1/zim/payment").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("mpesa-payment/async-post", endpoint(),
                        requestFields(
                                fieldWithPath("vendorRef").description("Unique payment identifier, Required"),
                                fieldWithPath("bankName").description("Bank or External provider name (supported by API), Currently supported names: `mpesa`, Required"),
                                fieldWithPath("accountNumber").description("Account number withing bank (or external provider), Required"),
                                fieldWithPath("amount").description("Amount of payment, Required"),
                                fieldWithPath("metaData").description("Additional payment meta data"),
                                fieldWithPath("metaData.walletId").description("Payment Wallet ID, Required"),
                                fieldWithPath("metaData.transactionCode").description("Payment Transaction Code, Required"),
                                fieldWithPath("metaData.sessionId").description("Session ID, Required"),
                                fieldWithPath("metaData.channel").description("Channel, Required"),
                                fieldWithPath("metaData.accountId").description("Crediting Account ID, Required"),
                                fieldWithPath("metaData.partnerId").description("Partner ID, Required"),
                                fieldWithPath("metaData.accountFundId").description("Account Fund ID, Required"),
                                fieldWithPath("metaData.cardNumber").description("Card Number, Required"),
                                fieldWithPath("metaData.paymentDescription").description("Payment Description, Required"),
                                fieldWithPath("metaData.organisation").description("Organisation, Required")
                        ),
                        responseFields(
                                fieldWithPath("vendorRef").description("Unique payment identifier"),
                                fieldWithPath("status").description("'PROCESSING' payment status"),
                                fieldWithPath("spTries").description("Approve SP attempts amount"),
                                fieldWithPath("date").description("Date of the transaction")
                        )
                ));
    }

    @Test
    void testGetMpesaPaymentRequest() throws Exception {
        String jsonRequest = Tool.readResourceAsString("data/json/mpesaPaymentRequest.json");
        PaymentRequestZim paymentRequest = createObjectMapper().readValue(jsonRequest, PaymentRequestZim.class);
        when(paymentService.getPaymentRequest(VENDOR_REF)).thenReturn(paymentRequest);

        mockMvc.perform(get("/api/v1/zim/payment/{vendorRef}/request", VENDOR_REF).contentType(APPLICATION_JSON))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andDo(document("mpesa-payment/get-request", endpoint(),
                        pathParameters(parameterWithName("vendorRef").description("Identifier of payment")),
                        responseFields(
                                fieldWithPath("vendorRef").description("Unique payment identifier, Required"),
                                fieldWithPath("bankName").description("Bank or External provider name (supported by API), Currently supported names: `mpesa`, Required"),
                                fieldWithPath("accountNumber").description("Account number withing bank (or external provider), Required"),
                                fieldWithPath("amount").description("Amount of payment, Required"),
                                fieldWithPath("metaData").description("Additional payment meta data"),
                                fieldWithPath("metaData.walletId").description("Payment Wallet ID, Required"),
                                fieldWithPath("metaData.transactionCode").description("Payment Transaction Code, Required"),
                                fieldWithPath("metaData.sessionId").description("Session ID, Required"),
                                fieldWithPath("metaData.channel").description("Channel, Required"),
                                fieldWithPath("metaData.accountId").description("Crediting Account ID, Required"),
                                fieldWithPath("metaData.partnerId").description("Partner ID, Required"),
                                fieldWithPath("metaData.accountFundId").description("Account Fund ID, Required"),
                                fieldWithPath("metaData.cardNumber").description("Card Number, Required"),
                                fieldWithPath("metaData.paymentDescription").description("Payment Description, Required"),
                                fieldWithPath("metaData.organisation").description("Organisation, Required")
                        )
                ));
    }

    @Test
    void testGetMpesaPaymentResponse() throws Exception {
        String jsonResponse = Tool.readResourceAsString("data/json/mpesaPaymentResponse.json");
        PaymentResponseZim paymentResponse = createObjectMapper().readValue(jsonResponse, PaymentResponseZim.class);
        when(paymentService.getPaymentResponse(VENDOR_REF)).thenReturn(paymentResponse);

        mockMvc.perform(get("/api/v1/zim/payment/{vendorRef}/response", VENDOR_REF).contentType(APPLICATION_JSON))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andDo(document("mpesa-payment/get-response", endpoint(),
                        pathParameters(parameterWithName("vendorRef").description("Identifier of payment")),
                        responseFields(
                                fieldWithPath("vendorRef").description("Unique payment identifier"),
                                fieldWithPath("status").description("Payment status, may be one of: PROCESSING, OTP_WAITING, SUCCESS, ERROR"),
                                fieldWithPath("externalTransactionId").description("Transaction ID returned from external provider (or bank). Non-null only on successful payment"),
                                fieldWithPath("spTries").description("Approve SP attempts amount"),
                                fieldWithPath("date").description("Date of the transaction"),
                                fieldWithPath("spResult").description("Result of stored procedure call"),
                                fieldWithPath("spResult.spName").description("Name of SP ('p_Create_Transactions_Card' value for mpesa MTP payment)"),
                                fieldWithPath("spResult.transactionId").description("Transaction ID returned from SP"),
                                fieldWithPath("spResult.drAccountId").description("Debit Account ID returned from SP"),
                                fieldWithPath("spResult.crAccountId").description("Credit Account ID returned from SP"),
                                fieldWithPath("spResult.balance").description("Current balance returned from SP"),
                                fieldWithPath("spResult.drFees").description("Debit Fees returned from SP"),
                                fieldWithPath("spResult.result").description("Result returned from SP (1 - success, 0 - error)"),
                                fieldWithPath("spResult.message").description("Message returned from SP ('Funds successfully loaded. ' - for successful payment)"),
                                fieldWithPath("spResult.error").description("Error message returned from SP")
                        )
                ));
    }

    @Test
    void testMpesaManualRefund() throws Exception {
        String jsonResponse = Tool.readResourceAsString("data/json/mpesaPaymentResponse.json");
        PaymentResponseZim paymentResponse = createObjectMapper().readValue(jsonResponse, PaymentResponseZim.class);
        when(paymentService.getPaymentResponse(VENDOR_REF)).thenReturn(paymentResponse);

        mockMvc.perform(get("/api/v1/zim/payment/{vendorRef}/response", VENDOR_REF).contentType(APPLICATION_JSON))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andDo(document("mpesa-payment/get-response", endpoint(),
                        pathParameters(parameterWithName("vendorRef").description("Identifier of payment")),
                        responseFields(
                                fieldWithPath("vendorRef").description("Unique payment identifier"),
                                fieldWithPath("status").description("Payment status, may be one of: PROCESSING, OTP_WAITING, SUCCESS, ERROR"),
                                fieldWithPath("externalTransactionId").description("Transaction ID returned from external provider (or bank). Non-null only on successful payment"),
                                fieldWithPath("spTries").description("Approve SP attempts amount"),
                                fieldWithPath("date").description("Date of the transaction"),
                                fieldWithPath("spResult").description("Result of stored procedure call"),
                                fieldWithPath("spResult.spName").description("Name of SP ('p_Create_Transactions_Card' value for mpesa MTP payment)"),
                                fieldWithPath("spResult.transactionId").description("Transaction ID returned from SP"),
                                fieldWithPath("spResult.drAccountId").description("Debit Account ID returned from SP"),
                                fieldWithPath("spResult.crAccountId").description("Credit Account ID returned from SP"),
                                fieldWithPath("spResult.balance").description("Current balance returned from SP"),
                                fieldWithPath("spResult.drFees").description("Debit Fees returned from SP"),
                                fieldWithPath("spResult.result").description("Result returned from SP (1 - success, 0 - error)"),
                                fieldWithPath("spResult.message").description("Message returned from SP ('Funds successfully loaded. ' - for successful payment)"),
                                fieldWithPath("spResult.error").description("Error message returned from SP")
                        )
                ));
    }

    @Test
    void testPosbSyncPaymentRequest() throws Exception {
        String jsonRequest = Tool.readResourceAsString("data/json/posbPaymentRequest.json");
        String jsonResponse = Tool.readResourceAsString("data/json/posbPaymentResponse.json");
        PaymentRequestZim paymentRequest = createObjectMapper().readValue(jsonRequest, PaymentRequestZim.class);
        PaymentResponseZim paymentResponse = createObjectMapper().readValue(jsonResponse, PaymentResponseZim.class);
        when(paymentService.makePaymentSync(paymentRequest)).thenReturn(paymentResponse);

        mockMvc.perform(post("/api/v1/zim/payment/sync").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("zim-posb-payment/sync-post", endpoint(),
                        requestFields(
                                fieldWithPath("vendorRef").description("Unique payment identifier, Required"),
                                fieldWithPath("bankName").description("Bank or External provider name (supported by API), Currently supported names: `mpesa`, Required"),
                                fieldWithPath("accountNumber").description("Account number withing bank (or external provider), Required"),
                                fieldWithPath("amount").description("Amount of payment, Required"),
                                fieldWithPath("metaData").description("Additional payment meta data"),
                                fieldWithPath("metaData.walletId").description("Payment Wallet ID, Optional"),
                                fieldWithPath("metaData.paymentId").description("Payment ID, Required"),
                                fieldWithPath("metaData.approvePaymentFlat").description("Approve payment flat, Required")
                        ),
                        responseFields(
                                fieldWithPath("vendorRef").description("Unique payment identifier"),
                                fieldWithPath("status").description("Payment status, may be one of: PROCESSING, OTP_WAITING, SUCCESS, ERROR"),
                                fieldWithPath("mobile").description("Mobile number that was used to send OTP (One-time password)"),
                                fieldWithPath("spTries").description("Approve SP attempts amount"),
                                fieldWithPath("date").description("Date of the transaction")
                        )
                ));
    }

    @Test
    void testPosbSyncPaymentOtpRequest() throws Exception {
        String jsonRequest = Tool.readResourceAsString("data/json/posbOtpPaymentRequest.json");
        String jsonResponse = Tool.readResourceAsString("data/json/posbOtpPaymentResponse.json");
        PaymentOtpRequestZim paymentRequest = createObjectMapper().readValue(jsonRequest, PaymentOtpRequestZim.class);
        PaymentResponseZim paymentResponse = createObjectMapper().readValue(jsonResponse, PaymentResponseZim.class);
        when(paymentService.handlePaymentOtpSync(paymentRequest)).thenReturn(paymentResponse);

        mockMvc.perform(post("/api/v1/zim/payment/otp/sync").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("zim-posb-payment-otp/sync-post", endpoint(),
                        requestFields(
                                fieldWithPath("vendorRef").description("Unique payment identifier, Required"),
                                fieldWithPath("otp").description("OTP (one-time password), Required")
                        ),
                        responseFields(
                                fieldWithPath("vendorRef").description("Unique payment identifier"),
                                fieldWithPath("status").description("Payment status, may be one of: PROCESSING, OTP_WAITING, SUCCESS, ERROR"),
                                fieldWithPath("mobile").description("Mobile number that was used to send OTP (One-time password)"),
                                fieldWithPath("externalTransactionId").description("Transaction ID returned from external provider (or bank). Non-null only on successful payment"),
                                fieldWithPath("spTries").description("Approve SP attempts amount"),
                                fieldWithPath("date").description("Date of the transaction"),
                                fieldWithPath("spResult").description("Result of stored procedure call"),
                                fieldWithPath("spResult.spName").description("Name of SP ('p_Payment_Approval' value for ecocash payment)"),
                                fieldWithPath("spResult.result").description("Result returned from SP (1 - success, 0 - error)"),
                                fieldWithPath("spResult.message").description("Message returned from SP ('Funds successfully loaded. ' - for successful payment)"),
                                fieldWithPath("spResult.transactionId").description("Transaction ID returned from SP"),
                                fieldWithPath("spResult.error").description("Error message returned from SP")
                        )
                ));
    }

    @Test
    void testEcocashSyncPayment() throws Exception {
        String jsonRequest = Tool.readResourceAsString("data/json/ecocashPaymentRequest.json");
        String jsonResponse = Tool.readResourceAsString("data/json/ecocashPaymentResponse.json");
        PaymentRequestZim paymentRequest = createObjectMapper().readValue(jsonRequest, PaymentRequestZim.class);
        PaymentResponseZim paymentResponse = createObjectMapper().readValue(jsonResponse, PaymentResponseZim.class);
        when(paymentService.makePaymentSync(paymentRequest)).thenReturn(paymentResponse);

        mockMvc.perform(post("/api/v1/zim/payment/sync").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("zim-ecocash-payment/sync-post", endpoint(),
                        requestFields(
                                fieldWithPath("vendorRef").description("Unique payment identifier, Required"),
                                fieldWithPath("bankName").description("Bank or External provider name (supported by API), Currently supported names: `mpesa`, Required"),
                                fieldWithPath("accountNumber").description("Account number withing bank (or external provider), Required"),
                                fieldWithPath("amount").description("Amount of payment, Required"),
                                fieldWithPath("metaData").description("Additional payment meta data"),
                                fieldWithPath("metaData.walletId").description("Payment Wallet ID, Optional"),
                                fieldWithPath("metaData.paymentId").description("Payment ID, Required"),
                                fieldWithPath("metaData.approvePaymentFlat").description("Approve payment flat, Required"),
                                fieldWithPath("metaData.transactionCode").description("Payment Transaction Code, Required"),
                                fieldWithPath("metaData.accountNumber").description("Account Number, Required"),
                                fieldWithPath("metaData.description").description("Payment Description, Required")
                        ),
                        responseFields(
                                fieldWithPath("vendorRef").description("Unique payment identifier"),
                                fieldWithPath("status").description("Payment status, may be one of: PROCESSING, OTP_WAITING, SUCCESS, ERROR"),
                                fieldWithPath("externalTransactionId").description("Transaction ID returned from external provider (or bank). Non-null only on successful payment"),
                                fieldWithPath("date").description("Date of the transaction"),
                                fieldWithPath("spTries").description("Approve SP attempts amount"),
                                fieldWithPath("spResult").description("Result of stored procedure call"),
                                fieldWithPath("spResult.spName").description("Name of SP ('p_Payment_Approval' value for ecocash payment)"),
                                fieldWithPath("spResult.result").description("Result returned from SP (1 - success, 0 - error)"),
                                fieldWithPath("spResult.message").description("Message returned from SP ('Funds successfully loaded. ' - for successful payment)"),
                                fieldWithPath("spResult.transactionId").description("Transaction ID returned from SP"),
                                fieldWithPath("spResult.error").description("Error message returned from SP")
                        )
                ));
    }

    private ObjectMapper createObjectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
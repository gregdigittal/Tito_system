package cash.ice.api.controller;

import cash.ice.api.documentation.IcecashErrorCodesDocumentation;
import cash.ice.api.errors.ApiExceptionHandler;
import cash.ice.api.service.PaymentService;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;
import cash.ice.common.dto.ResponseStatus;
import cash.ice.common.utils.Tool;
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

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static cash.ice.api.documentation.IcecashEndpointDocumentation.endpoint;
import static cash.ice.common.error.ErrorCodes.EC4002;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = PaymentRestController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {PaymentRestController.class, ApiExceptionHandler.class})
@AutoConfigureRestDocs(outputDir = "target/generated-snippets", uriScheme = "https", uriHost = "uat-gateway.icecash.mobi", uriPort = 80)
class PaymentControllerTest {
    private static final String VENDOR_REF = "vendorRefTest1";
    private static final String TRANSACTION_ID = "1001";

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testAddPayment() throws Exception {
        String jsonRequest = Tool.readResourceAsString("data/json/paymentRequest.json");

        mockMvc.perform(post("/api/v1/payments").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("payment/post", endpoint(),
                        relaxedRequestFields(
                                fieldWithPath("vendorRef").description("Unique payment identifier, Required"),
                                fieldWithPath("tx").description("Transaction code, may be different for different payment services, Required"),
                                fieldWithPath("initiatorType").description("Initiator type, is usually used to select a payment service, the values of which are stored in the `initiator_type` database table, it can take eg. the following values: `card`, `paygo`, `ecocash`, `onemoney`, ..., Required"),
                                fieldWithPath("initiator").description("Initiator, as a rule is an MSISDN of the user which performs the payment. Some payment services require this value, Required"),
                                fieldWithPath("currency").description("Currency of payment, eg. ZWL, USD, ..."),
                                fieldWithPath("amount").description("Amount of payment, Required"),
                                fieldWithPath("partnerId").description("Identifier of a partner, if payment is requested by partner, Required"),
                                fieldWithPath("apiVersion").description("Required API version, Required"),
                                fieldWithPath("date").description("Date of payment, Required"),
                                fieldWithPath("deviceId").description("Some varchar identifier particular to the integration"),
                                fieldWithPath("meta").description("Additional info object, as a rule contains payment specific fields structure")
                        )
                ));
        PaymentRequest expectedPaymentRequest = createObjectMapper().readValue(jsonRequest, PaymentRequest.class);
        verify(paymentService).addPayment(expectedPaymentRequest);
    }

    @Test
    void testAddPaygoPayment() throws Exception {
        String jsonRequest = Tool.readResourceAsString("data/json/paymentPaygoRequest.json");

        mockMvc.perform(post("/api/v1/payments").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("payment/post-paygo", endpoint()));
        PaymentRequest expectedPaymentRequest = createObjectMapper().readValue(jsonRequest, PaymentRequest.class);
        verify(paymentService).addPayment(expectedPaymentRequest);
    }

    @Test
    void testAddEcocashPayment() throws Exception {
        String jsonRequest = Tool.readResourceAsString("data/json/paymentEcocashRequest.json");

        mockMvc.perform(post("/api/v1/payments").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("payment/post-ecocash", endpoint()));
        PaymentRequest expectedPaymentRequest = createObjectMapper().readValue(jsonRequest, PaymentRequest.class);
        verify(paymentService).addPayment(expectedPaymentRequest);
    }

    @Test
    void testAddOnemoneyPayment() throws Exception {
        String jsonRequest = Tool.readResourceAsString("data/json/paymentOnemoneyRequest.json");

        mockMvc.perform(post("/api/v1/payments").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("payment/post-onemoney", endpoint()));
        PaymentRequest expectedPaymentRequest = createObjectMapper().readValue(jsonRequest, PaymentRequest.class);
        verify(paymentService).addPayment(expectedPaymentRequest);
    }

    @Test
    void testSuccessGetPaymentResponse() throws Exception {
        when(paymentService.getPaymentResponse(VENDOR_REF))
                .thenReturn(PaymentResponse.success(VENDOR_REF, TRANSACTION_ID, new BigDecimal(100), null, null));

        mockMvc.perform(get("/api/v1/payments/{vendorRef}/response", VENDOR_REF).contentType(APPLICATION_JSON))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.vendorRef", is(VENDOR_REF)))
                .andExpect(jsonPath("$.date", notNullValue()))
                .andExpect(jsonPath("$.status", is(ResponseStatus.SUCCESS.toString())))
                .andExpect(jsonPath("$.errorCode").doesNotExist())
                .andExpect(jsonPath("$.message", is("Transaction processed successfully")))
                .andExpect(jsonPath("$.transactionId", is(TRANSACTION_ID)))
                .andExpect(jsonPath("$.balance", is(100)))
                .andDo(document("payment/response", endpoint(),
                        pathParameters(parameterWithName("vendorRef").description("Identifier of payment")),
                        relaxedResponseFields(
                                fieldWithPath("vendorRef").description("Unique message identifier"),
                                fieldWithPath("status").description("SUCCESS status of the request"),
                                fieldWithPath("message").description("\"Transaction processed successfully\" message"),
                                fieldWithPath("transactionId").description("Transaction id if status is success"),
                                fieldWithPath("balance").description("Account balance if status is success"),
                                fieldWithPath("date").description("Date of payment")
                        )));
    }

    @Test
    void testErrorGetPaymentResponse() throws Exception {
        when(paymentService.getPaymentResponse(VENDOR_REF))
                .thenReturn(PaymentResponse.error(VENDOR_REF, EC4002, "Insufficient balance for the transaction. Current balance: ZWL $1.71"));

        mockMvc.perform(get("/api/v1/payments/{vendorRef}/response", VENDOR_REF).contentType(APPLICATION_JSON))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("payment/error-response", endpoint(),
                        pathParameters(parameterWithName("vendorRef").description("Identifier of payment")),
                        relaxedResponseFields(
                                fieldWithPath("vendorRef").description("Unique message identifier"),
                                fieldWithPath("status").description("ERROR status of the request"),
                                fieldWithPath("errorCode").description("Error code"),
                                fieldWithPath("message").description("Error reason message"),
                                fieldWithPath("date").description("Date of payment")
                        )));
    }

    @Test
    void testProcessingGetPaymentResponse() throws Exception {
        when(paymentService.getPaymentResponse(VENDOR_REF)).thenReturn(PaymentResponse.processing(VENDOR_REF));

        mockMvc.perform(get("/api/v1/payments/{vendorRef}/response", VENDOR_REF).contentType(APPLICATION_JSON))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("payment/processing-response", endpoint(),
                        pathParameters(parameterWithName("vendorRef").description("Identifier of payment")),
                        relaxedResponseFields(
                                fieldWithPath("vendorRef").description("Unique message identifier"),
                                fieldWithPath("status").description("PROCESSING status of the request"),
                                fieldWithPath("message").description("\"Operation is in progress\" message"),
                                fieldWithPath("date").description("Date of payment")
                        )));
    }

    @Test
    void testPaygoSubResultGetPaymentResponse() throws Exception {
        when(paymentService.getPaymentResponse(VENDOR_REF)).thenReturn(PaymentResponse.subResult(VENDOR_REF,
                Map.of("payGoId", 40044, "deviceReference", UUID.randomUUID().toString(), "qr64", "data:image:...")));

        mockMvc.perform(get("/api/v1/payments/{vendorRef}/response", VENDOR_REF).contentType(APPLICATION_JSON))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("payment/paygo-response", endpoint(),
                        pathParameters(parameterWithName("vendorRef").description("Identifier of payment")),
                        relaxedResponseFields(
                                fieldWithPath("vendorRef").description("Unique message identifier"),
                                fieldWithPath("status").description("PROCESSING status of the request"),
                                fieldWithPath("payload.payGoId").description("Unique PayGO Identifier of payment"),
                                fieldWithPath("payload.deviceReference").description("Device reference"),
                                fieldWithPath("payload.qr64").description("Base64 image"),
                                fieldWithPath("message").description("\"Operation is in progress\" message"),
                                fieldWithPath("date").description("Date of payment")
                        )));
    }

    @Test
    void testGetPaymentResponseIfProcessing() throws Exception {
        when(paymentService.getPaymentResponse(VENDOR_REF)).thenReturn(PaymentResponse.processing(VENDOR_REF));

        mockMvc.perform(get("/api/v1/payments/%s/response".formatted(VENDOR_REF)).contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.vendorRef", is(VENDOR_REF)))
                .andExpect(jsonPath("$.date", notNullValue()))
                .andExpect(jsonPath("$.status", is(ResponseStatus.PROCESSING.toString())))
                .andExpect(jsonPath("$.message", is("Operation is in progress")))
                .andExpect(jsonPath("$.errorCode").doesNotExist())
                .andExpect(jsonPath("$.transactionId").doesNotExist())
                .andExpect(jsonPath("$.balance").doesNotExist());
    }

    @Test
    void testErrorCodes() throws Exception {
        mockMvc.perform(get("/api/v1/error"))
                .andDo(document("error-codes", IcecashErrorCodesDocumentation.errorCodes()));
    }

    private ObjectMapper createObjectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
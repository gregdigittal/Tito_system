package cash.ice.paygo.controller;

import cash.ice.common.utils.Tool;
import cash.ice.paygo.dto.admin.*;
import cash.ice.paygo.error.PaygoExceptionHandler;
import cash.ice.paygo.service.PaygoAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = PaygoAdminController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {PaygoAdminController.class, PaygoExceptionHandler.class})
@AutoConfigureRestDocs(outputDir = "target/generated-snippets", uriScheme = "https", uriHost = "uat-gateway.icecash.mobi", uriPort = 80)
class PaygoAdminControllerTest {

    @MockBean
    private PaygoAdminService paygoAdminService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getFinancialInstitutions() throws Exception {
        when(paygoAdminService.getFinancialInstitutions()).thenReturn(List.of(
                new FinancialInstitution().setId("47788a3c-dcf4-4606-8be8-aff8902c7bf4").setName("CBZ").setActive(true)
                        .setInstitutionId("601237").setCreated("2022-02-14T14:23:22.577789+02:00").setUpdated("2022-02-14T14:23:22.577883+02:00")));

        mockMvc.perform(get("/api/v1/paygo/financial-institutions").contentType(APPLICATION_JSON))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("paygo/get-financial-institutions",
                        responseFields(
                                fieldWithPath("[].id").description("ID reference of institution"),
                                fieldWithPath("[].institutionId").description("Financial Institution ID"),
                                fieldWithPath("[].name").description("The human friendly name of the institutions"),
                                fieldWithPath("[].active").description("Indicates whether this FI is available for use"),
                                fieldWithPath("[].created").description("Time at which the entity was created"),
                                fieldWithPath("[].updated").description("Time at which the entity was updated")
                        )));
    }

    @Test
    void getMerchants() throws Exception {
        when(paygoAdminService.getMerchants()).thenReturn(List.of(
                (Merchant) new Merchant().setId("6c464e00-1b49-4051-b222-5786ea60efba").setName("ICEcash").setActive(true)
                        .setTransactionCode("PGCBZ").setAddressLine1("shop 1").setAddressLine2("shop 2").setCity("Harare")
                        .setCountryCode("ZWE").setDescription("merchant desc").setEmailAddress("info@icecash.org")
                        .setRegion("Mashonaland").setMspReference("626105b844fc193b8e5560cd").setPhoneNumber("+263773123124")
                        .setUrl("http://icecash.test.org")));

        mockMvc.perform(get("/api/v1/paygo/merchant").contentType(APPLICATION_JSON))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("paygo/get-merchant",
                        responseFields(
                                fieldWithPath("[].id").description("ID of merchant"),
                                fieldWithPath("[].active").description("Whether merchant is active or not"),
                                fieldWithPath("[].transactionCode").description("Transaction Code assigned to this merchant"),
                                fieldWithPath("[].countryCode").description("Country Code of merchant"),
                                fieldWithPath("[].city").description("City of merchant"),
                                fieldWithPath("[].name").description("Name of merchant"),
                                fieldWithPath("[].region").description("Region of merchant"),
                                fieldWithPath("[].addressLine1").description("First address line of merchant"),
                                fieldWithPath("[].addressLine2").description("Second address line of merchant"),
                                fieldWithPath("[].description").description("Merchant description"),
                                fieldWithPath("[].emailAddress").description("E-mail address of merchant"),
                                fieldWithPath("[].phoneNumber").description("Phone number of merchant"),
                                fieldWithPath("[].url").description("URL of merchant"),
                                fieldWithPath("[].mspReference").description("an MSP held reference that can be used to retrieve the merchant")
                        )));
    }

    @Test
    void addMerchant() throws Exception {
        String jsonRequest = Tool.readResourceAsString("data/json/merchant.json");
        Merchant merchantResponse = new ObjectMapper().readValue(jsonRequest, Merchant.class);
        when(paygoAdminService.addMerchant(any())).thenReturn(merchantResponse);

        mockMvc.perform(post("/api/v1/paygo/merchant").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("paygo/post-merchant",
                        relaxedRequestFields(
                                fieldWithPath("name").description("Indicates the merchant Name"),
                                fieldWithPath("transactionCode").description("Transaction Code assigned to this merchant"),
                                fieldWithPath("active").description("Indicates whether this credential is available for use"),
                                fieldWithPath("addressLine1").description("address Line 1"),
                                fieldWithPath("addressLine2").description("address Line 2"),
                                fieldWithPath("city").description("City"),
                                fieldWithPath("countryCode").description("Country Code eg ZWE"),
                                fieldWithPath("description").description("Description"),
                                fieldWithPath("emailAddress").description("Email Address"),
                                fieldWithPath("mspReference").description("an MSP held reference that can be used to retrieve the merchant"),
                                fieldWithPath("phoneNumber").description("Phone Number"),
                                fieldWithPath("region").description("Region location of the merchant"),
                                fieldWithPath("url").description("Merchant website URL (Optional)")
                        )
                ));
    }

    @Test
    void updateMerchant() throws Exception {
        String jsonRequest = Tool.readResourceAsString("data/json/merchant.json");
        Merchant merchantResponse = new ObjectMapper().readValue(jsonRequest, Merchant.class);
        when(paygoAdminService.addMerchant(any())).thenReturn(merchantResponse);

        mockMvc.perform(put("/api/v1/paygo/merchant").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("paygo/put-merchant",
                        relaxedRequestFields(
                                fieldWithPath("id").description("The entity id"),
                                fieldWithPath("name").description("Indicates the merchant Name"),
                                fieldWithPath("active").description("Indicates whether this credential is available for use"),
                                fieldWithPath("addressLine1").description("address Line 1"),
                                fieldWithPath("addressLine2").description("address Line 2"),
                                fieldWithPath("city").description("City"),
                                fieldWithPath("countryCode").description("Country Code eg ZWE"),
                                fieldWithPath("description").description("Description"),
                                fieldWithPath("emailAddress").description("Email Address"),
                                fieldWithPath("mspReference").description("an MSP held reference that can be used to retrieve the merchant"),
                                fieldWithPath("phoneNumber").description("Phone Number"),
                                fieldWithPath("region").description("Region location of the merchant"),
                                fieldWithPath("url").description("Merchant website URL (Optional)")
                        )
                ));
    }

    @Test
    void deleteMerchant() throws Exception {
        mockMvc.perform(delete("/api/v1/paygo/merchant/{id}", "6c464e00-1b49-4051-b222-5786ea60efba").contentType(APPLICATION_JSON))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("paygo/delete-merchant",
                        pathParameters(parameterWithName("id").description("Merchant ID"))
                ));
    }

    @Test
    void getCredentials() throws Exception {
        String merchantId = "6c464e00-1b49-4051-b222-5786ea60efba";
        when(paygoAdminService.getCredentials(merchantId)).thenReturn(List.of(
                (Credential) new Credential().setId("6c464e00-1b49-4051-b222-5786ea60efba").setType("ACCEPTOR").setActive(true)
                        .setCurrencyCode("ZWL").setCredential("123456").setCredentialReference("83d33750-553f-490c-94ec-647d39ee6b22")
                        .setTerminalId("12345678").setCardAcceptorId("123456789012345").setFiId("6737eb16-e86d-4457-ac5d-1f26f0265a58")));

        mockMvc.perform(get("/api/v1/paygo/merchant/credential/" + merchantId).contentType(APPLICATION_JSON))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("paygo/get-merchant-credential",
                        responseFields(
                                fieldWithPath("[].id").description("Credential ID"),
                                fieldWithPath("[].active").description("Whether the credential is active or not"),
                                fieldWithPath("[].type").description("Indicates the type of credential, e.g. ACCEPTOR, BANK_ACCOUNT, etc"),
                                fieldWithPath("[].currencyCode").description("The currency code that the merchant can be settled in. (this may be null for multi-currency conversions)"),
                                fieldWithPath("[].credential").description("The primary identifier for a credential, e.g. Account Id"),
                                fieldWithPath("[].credentialReference").description("A reference provided by the calling system, e.g. a UUID"),
                                fieldWithPath("[].terminalId").description("The terminal identifier at the acquiring bank (identifying the merchant account)"),
                                fieldWithPath("[].cardAcceptorId").description("The card acceptor identified at the acquiring bank (identifying the merchant account)"),
                                fieldWithPath("[].fiId").description("The financial institution ID correlated to this credential")
                        )));
    }

    @Test
    void addCredential() throws Exception {
        String jsonRequest = Tool.readResourceAsString("data/json/credential.json");
        Credential credentialResponse = new ObjectMapper().readValue(jsonRequest, Credential.class);
        when(paygoAdminService.addCredential(any())).thenReturn(credentialResponse);

        mockMvc.perform(post("/api/v1/paygo/merchant/credential").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("paygo/post-credential",
                        relaxedRequestFields(
                                fieldWithPath("merchantId").description("Merchant ID to assigned the credential to"),
                                fieldWithPath("type").description("Indicates the type of credential, e.g. ACCEPTOR, BANK_ACCOUNT, etc"),
                                fieldWithPath("currencyCode").description("The currency code that the merchant can be settled in. (this may be null for multi-currency conversions)"),
                                fieldWithPath("credential").description("The primary identifier for a credential, e.g. Account Id"),
                                fieldWithPath("credentialReference").description("A reference provided by the calling system, e.g. a UUID"),
                                fieldWithPath("terminalId").description("The terminal identifier at the acquiring bank (identifying the merchant account)"),
                                fieldWithPath("cardAcceptorId").description("The card acceptor identified at the acquiring bank (identifying the merchant account)"),
                                fieldWithPath("fiId").description("The financial institution correlated to this credential"),
                                fieldWithPath("active").description("Indicates whether this credential is available for use")
                        )
                ));
    }

    @Test
    void updateCredential() throws Exception {
        String jsonRequest = Tool.readResourceAsString("data/json/credential.json");
        Credential credentialResponse = new ObjectMapper().readValue(jsonRequest, Credential.class);
        when(paygoAdminService.addCredential(any())).thenReturn(credentialResponse);

        mockMvc.perform(put("/api/v1/paygo/merchant/credential").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("paygo/put-credential",
                        relaxedRequestFields(
                                fieldWithPath("id").description("Credential ID"),
                                fieldWithPath("merchantCredentialId").description("MerchantCredentialID previously assigned by PayGo DS"),
                                fieldWithPath("merchantId").description("Merchant ID to which this credential belongs"),
                                fieldWithPath("type").description("Indicates the type of credential, e.g. ACCEPTOR, BANK_ACCOUNT, etc"),
                                fieldWithPath("currencyCode").description("The currency code that the merchant can be settled in. (this may be null for multi-currency conversions)"),
                                fieldWithPath("credential").description("The primary identifier for a credential, e.g. Account Id"),
                                fieldWithPath("credentialReference").description("A reference provided by the calling system, e.g. a UUID"),
                                fieldWithPath("terminalId").description("The terminal identifier at the acquiring bank (identifying the merchant account)"),
                                fieldWithPath("cardAcceptorId").description("The card acceptor identified at the acquiring bank (identifying the merchant account)"),
                                fieldWithPath("fiId").description("The financial institution correlated to this credential"),
                                fieldWithPath("active").description("Indicates whether this credential is available for use")
                        )
                ));
    }

    @Test
    void deleteCredential() throws Exception {
        mockMvc.perform(delete("/api/v1/paygo/merchant/credential/{id}", "6c464e00-1b49-4051-b222-5786ea60efba").contentType(APPLICATION_JSON))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("paygo/delete-credential",
                        pathParameters(parameterWithName("id").description("Credential ID"))
                ));
    }

    @Test
    void getPaymentExpiration() throws Exception {
        String deviceReference = "someDeviceReference";
        when(paygoAdminService.getPaymentExpiration(deviceReference)).thenReturn(new Expiration()
                .setCreated("2022-04-29T13:43:37.677978+02:00").setResponseCode("025").setResponseDescription("NOT FOUND"));

        mockMvc.perform(get("/api/v1/paygo/payment/expire/{deviceReference}", deviceReference).contentType(APPLICATION_JSON))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("paygo/get-expiration",
                        pathParameters(parameterWithName("deviceReference").description("Device Reference")),
                        responseFields(
                                fieldWithPath("created").description("Time at which the entity was created"),
                                fieldWithPath("responseDescription").description("Textual description of the response"),
                                fieldWithPath("responseCode").description("Response code: " +
                                        "`'000'` - Payment Expired. " +
                                        "`'005'` - Do not honour (Transaction Already Processed - Request received too late). perform query on transaction state if no advice received. " +
                                        "`'025'` - Not Found. " +
                                        "`'091'` - Transaction possibly in flight, unknown result. try again after a few seconds. ")
                        )));
    }

    @Test
    void getPaymentStatus() throws Exception {
        String jsonRequest = Tool.readResourceAsString("data/json/paymentStatus.json");
        PaymentStatus paymentStatus = new ObjectMapper().readValue(jsonRequest, PaymentStatus.class);
        String deviceReference = "someDeviceReference";
        when(paygoAdminService.getPaymentStatus(deviceReference)).thenReturn(paymentStatus);

        mockMvc.perform(get("/api/v1/paygo/payment/status/{deviceReference}", deviceReference).contentType(APPLICATION_JSON))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("paygo/get-status",
                        pathParameters(parameterWithName("deviceReference").description("Device Reference")),
                        relaxedResponseFields(
                                fieldWithPath("merchantPaymentId").description("Device Reference as specified in the first auth response"),
                                fieldWithPath("amount").description("The amount in decimal (2 decimal place precision)"),
                                fieldWithPath("complete").description("true/false"),
                                fieldWithPath("success").description("null/true/false. null= payment state not final"),
                                fieldWithPath("created").description("Time at which the entity was created"),
                                fieldWithPath("updated").description("Time at which the entity was updated"),
                                fieldWithPath("currencyCode").description("currency code as specified"),
                                fieldWithPath("expiresAt").description("Time at which this payment expires based on first auth response expires seconds"),
                                fieldWithPath("initiator").description("initiator as specified in first auth response"),
                                fieldWithPath("narration").description("narration as specified in first auth response"),
                                fieldWithPath("reference").description("reference as specified in first auth response"),
                                fieldWithPath("token").description("token as specified in first auth response"),
                                fieldWithPath("advices").description("refer to the bridge api, these are the same advices as would be received through the async push").optional()
                        )));
    }
}
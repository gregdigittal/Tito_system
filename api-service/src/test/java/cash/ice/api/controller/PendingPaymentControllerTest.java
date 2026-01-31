package cash.ice.api.controller;

import cash.ice.api.controller.zim.PendingPaymentRestController;
import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.PaymentCollectionView;
import cash.ice.api.dto.PaymentLineView;
import cash.ice.api.dto.PaymentView;
import cash.ice.api.errors.ApiExceptionHandler;
import cash.ice.api.service.AuthUserService;
import cash.ice.api.service.PendingPaymentService;
import cash.ice.common.constant.IceCashProfile;
import cash.ice.api.dto.PaymentDocument;
import cash.ice.common.utils.Tool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cash.ice.api.documentation.IcecashEndpointDocumentation.endpoint;
import static cash.ice.api.entity.zim.PaymentStatus.PENDING;
import static cash.ice.api.parser.impl.FbcTemplateParser.RTGS_TEMPLATE;
import static cash.ice.api.service.impl.PendingPaymentServiceImpl.*;
import static cash.ice.sqldb.entity.AccountSide.DR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = PendingPaymentRestController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {PendingPaymentRestController.class, ApiExceptionHandler.class})
@AutoConfigureRestDocs(outputDir = "target/generated-snippets", uriScheme = "https", uriHost = "uat-gateway.icecash.mobi", uriPort = 80)
@ActiveProfiles(IceCashProfile.PROD)
class PendingPaymentControllerTest {
    private static final int PAYMENT_ID = 1;
    private static final int COLLECTION_ID = 2;

    @MockBean
    private PendingPaymentService pendingPaymentService;
    @MockBean
    private AuthUserService authUserService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getPayment() throws Exception {
        AuthUser authUser = mockAuthUser();
        when(pendingPaymentService.getPayment(PAYMENT_ID, authUser)).thenReturn(createMockPayment());

        mockMvc.perform(get("/api/v1/payments/pending/{id}", PAYMENT_ID))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("pending-payment/get", endpoint(),
                        pathParameters(parameterWithName("id").description("Payment ID")),
                        relaxedResponseFields(
                                fieldWithPath("id").description("ID of the payment"),
                                fieldWithPath("status").description("Status of the payment"),
                                fieldWithPath("createdDate").description("Created date of the payment"),
                                fieldWithPath("count").description("Count of payment lines"),
                                fieldWithPath("total").description("Total amount of all payment lines"),
                                fieldWithPath("accountId").description("Account ID"),
                                fieldWithPath("accountSide").description("Side of Account, either DR or CR (debit or credit), required"),
                                fieldWithPath("description").description("Payment description"),
                                fieldWithPath("taxDeclarationId").description("Tax declaration ID"),
                                fieldWithPath("taxReasonId").description("Tax Reason ID"),
                                fieldWithPath("meta").description("Metadata of the payment"),
                                fieldWithPath("meta." + CREATED_ID).description("Entity ID of the payment creator"),
                                fieldWithPath("meta." + MODIFIED_ID).description("Entity ID of the last payment updater"),
                                fieldWithPath("meta." + MODIFIED_DATE).description("Date of the last payment update"),
                                fieldWithPath("meta." + APPROVED_ID).description("Entity ID of the first approver"),
                                fieldWithPath("meta." + APPROVED_DATE).description("Date of the first payment approvement"),
                                fieldWithPath("meta." + APPROVED2_ID).description("Entity ID of the second approver"),
                                fieldWithPath("meta." + APPROVED2_DATE).description("Date of the second payment approvement"),
                                fieldWithPath("meta." + REJECTED_ID).description("Entity ID of payment canceler"),
                                fieldWithPath("meta." + REJECTED_DATE).description("Date of the payment rejection"),
                                fieldWithPath("meta." + IMPORT_TEMPLATE).description("Template used to import payment lines data"),
                                fieldWithPath("meta.notifyTo").description("MSISDN or email for payment notification"),
                                fieldWithPath("meta.notifySubject").description("Subject if the above is an email"),
                                fieldWithPath("meta.notifyMessage").description("Body of the notification if the email"),
                                fieldWithPath("meta.locationId").description("Location ID"),
                                fieldWithPath("meta.channel").description("Channel"),
                                fieldWithPath("meta.notes").description("Any additional notes for the payment"),
                                fieldWithPath("documents[].documentId").description("Supporting document ID"),
                                fieldWithPath("documents[].filename").description("Supporting document file name"),
                                fieldWithPath("paymentLines[].id").description("Payment line ID"),
                                fieldWithPath("paymentLines[].accountId").description("Payment line Account ID"),
                                fieldWithPath("paymentLines[].details").description("Payment line details description"),
                                fieldWithPath("paymentLines[].amount").description("Payment line amount"),
                                fieldWithPath("paymentLines[].currency").description("Payment line currency"),
                                fieldWithPath("paymentLines[].transactionCode").description("Payment line transaction code"),
                                fieldWithPath("paymentLines[].transactionId").description("Payment line transaction ID"),
                                fieldWithPath("paymentLines[].meta.paymentMethod").description("Payment line payment method"),
                                fieldWithPath("paymentLines[].meta.bankName").description("Payment line bank name"),
                                fieldWithPath("paymentLines[].meta.bankBin").description("Payment line bank bin"),
                                fieldWithPath("paymentLines[].meta.branchCode").description("Payment line branch code"),
                                fieldWithPath("paymentLines[].meta.swiftCode").description("Payment line swift code"),
                                fieldWithPath("paymentLines[].meta.bankAccountNo").description("Payment line account number"),
                                fieldWithPath("paymentLines[].meta.beneficiaryName").description("Payment line beneficiary name"),
                                fieldWithPath("paymentLines[].meta.beneficiaryReference").description("Payment line beneficiary reference")
                        )
                ));
    }

    @Test
    void getPaymentList() throws Exception {
        AuthUser authUser = mockAuthUser();
        when(pendingPaymentService.getPayments(PageRequest.of(0, 10), authUser)).thenReturn(new PageImpl<>(List.of(
                createMockPayment(),
                createMockPayment()
        )));

        mockMvc.perform(get("/api/v1/payments/pending")
                        .param("page", "0")
                        .param("size", "10"))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("pending-payment/get-list", endpoint(),
                        queryParameters(
                                parameterWithName("page").description("If provided, payments list page to return, by default page=0"),
                                parameterWithName("size").description("If provided, amount of payments to return on provided page, by default size=10")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("content[].id").description("ID of the payment"),
                                fieldWithPath("content[].status").description("Status of the payment")
                        )
                ));
    }

    @Test
    void createPayment() throws Exception {
        AuthUser authUser = mockAuthUser();
        String jsonRequest = Tool.readResourceAsString("data/json/createPaymentRequest.json");
        when(pendingPaymentService.createPayment(any(PaymentView.class), eq(authUser))).thenReturn(
                new PaymentView());

        mockMvc.perform(post("/api/v1/payments/pending", PAYMENT_ID).contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andDo(document("pending-payment/create", endpoint(),
                        requestFields(
                                fieldWithPath("accountId").description("Account ID, required"),
                                fieldWithPath("accountSide").description("Side of Account, either DR or CR (debit or credit), required"),
                                fieldWithPath("description").description("Payment description"),
                                fieldWithPath("taxDeclarationId").description("Tax declaration ID"),
                                fieldWithPath("taxReasonId").description("Tax Reason ID"),
                                fieldWithPath("meta").description("Metadata of the payment"),
                                fieldWithPath("meta.notifyTo").description("MSISDN or email for payment notification"),
                                fieldWithPath("meta.notifySubject").description("Subject if the above is an email"),
                                fieldWithPath("meta.notifyMessage").description("Body of the notification if the email"),
                                fieldWithPath("meta.locationId").description("Location ID"),
                                fieldWithPath("meta.channel").description("Channel"),
                                fieldWithPath("meta.notes").description("Any additional notes for the payment"),
                                fieldWithPath("paymentLines[].accountId").description("Payment line Account ID"),
                                fieldWithPath("paymentLines[].details").description("Payment line details description"),
                                fieldWithPath("paymentLines[].amount").description("Payment line amount"),
                                fieldWithPath("paymentLines[].currency").description("Payment line currency"),
                                fieldWithPath("paymentLines[].transactionCode").description("Payment line transaction code"),
                                fieldWithPath("paymentLines[].meta.paymentMethod").description("Payment line payment method"),
                                fieldWithPath("paymentLines[].meta.bankName").description("Payment line bank name"),
                                fieldWithPath("paymentLines[].meta.bankBin").description("Payment line bank bin"),
                                fieldWithPath("paymentLines[].meta.branchCode").description("Payment line branch code"),
                                fieldWithPath("paymentLines[].meta.swiftCode").description("Payment line swift code"),
                                fieldWithPath("paymentLines[].meta.bankAccountNo").description("Payment line account number"),
                                fieldWithPath("paymentLines[].meta.beneficiaryName").description("Payment line beneficiary name"),
                                fieldWithPath("paymentLines[].meta.beneficiaryReference").description("Payment line beneficiary reference")
                        )
                ));
    }

    @Test
    void updatePayment() throws Exception {
        AuthUser authUser = mockAuthUser();
        String jsonRequest = Tool.readResourceAsString("data/json/updatePaymentRequest.json");
        when(pendingPaymentService.createPayment(any(PaymentView.class), eq(authUser))).thenReturn(
                new PaymentView());

        mockMvc.perform(put("/api/v1/payments/pending", PAYMENT_ID).contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("pending-payment/update", endpoint(),
                        requestFields(
                                fieldWithPath("id").description("Payment ID, required"),
                                fieldWithPath("accountId").description("Account ID, required"),
                                fieldWithPath("accountSide").description("Side of Account, either DR or CR (debit or credit), required"),
                                fieldWithPath("description").description("Payment description"),
                                fieldWithPath("taxDeclarationId").description("Tax declaration ID"),
                                fieldWithPath("taxReasonId").description("Tax Reason ID"),
                                fieldWithPath("meta").description("Metadata of the payment"),
                                fieldWithPath("meta.notifyTo").description("MSISDN or email for payment notification"),
                                fieldWithPath("meta.notifySubject").description("Subject if the above is an email"),
                                fieldWithPath("meta.notifyMessage").description("Body of the notification if the email"),
                                fieldWithPath("meta.locationId").description("Location ID"),
                                fieldWithPath("meta.channel").description("Channel"),
                                fieldWithPath("meta.notes").description("Any additional notes for the payment"),
                                fieldWithPath("paymentLines[].id").description("Payment line ID, required"),
                                fieldWithPath("paymentLines[].accountId").description("Payment line Account ID"),
                                fieldWithPath("paymentLines[].details").description("Payment line details description"),
                                fieldWithPath("paymentLines[].amount").description("Payment line amount, required"),
                                fieldWithPath("paymentLines[].currency").description("Payment line currency"),
                                fieldWithPath("paymentLines[].transactionCode").description("Payment line transaction code"),
                                fieldWithPath("paymentLines[].meta.paymentMethod").description("Payment line payment method"),
                                fieldWithPath("paymentLines[].meta.bankName").description("Payment line bank name"),
                                fieldWithPath("paymentLines[].meta.bankBin").description("Payment line bank bin"),
                                fieldWithPath("paymentLines[].meta.branchCode").description("Payment line branch code"),
                                fieldWithPath("paymentLines[].meta.swiftCode").description("Payment line swift code"),
                                fieldWithPath("paymentLines[].meta.bankAccountNo").description("Payment line account number"),
                                fieldWithPath("paymentLines[].meta.beneficiaryName").description("Payment line beneficiary name"),
                                fieldWithPath("paymentLines[].meta.beneficiaryReference").description("Payment line beneficiary reference")
                        )
                ));
    }

    @Test
    void uploadPaymentLines() throws Exception {
        AuthUser authUser = mockAuthUser();
        MockMultipartFile file = new MockMultipartFile("file", "template.xlsx",
                APPLICATION_OCTET_STREAM_VALUE, "content".getBytes());
        when(pendingPaymentService.uploadPaymentLines(PAYMENT_ID, RTGS_TEMPLATE, file.getInputStream(),
                authUser)).thenReturn(new PaymentView());

        mockMvc.perform(multipart("/api/v1/payments/pending/{paymentId}/upload", PAYMENT_ID)
                        .file(file)
                        .file("template", RTGS_TEMPLATE.getBytes()))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("pending-payment/upload", endpoint(),
                        pathParameters(parameterWithName("paymentId").description("Payment ID")),
                        requestParts(
                                partWithName("file").description("Payment template to upload, required"),
                                partWithName("template").description("Template name, required"))
                ));
    }

    @Test
    void approvePayment() throws Exception {
        AuthUser authUser = mockAuthUser();
        when(pendingPaymentService.approvePayment(PAYMENT_ID, authUser)).thenReturn(
                new PaymentView());
        mockMvc.perform(post("/api/v1/payments/pending/{paymentId}/approve", PAYMENT_ID))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("pending-payment/approve", endpoint(),
                        pathParameters(parameterWithName("paymentId").description("Payment ID"))
                ));
    }

    @Test
    void rejectPayment() throws Exception {
        mockMvc.perform(delete("/api/v1/payments/pending/{paymentId}/reject", PAYMENT_ID))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNoContent())
                .andDo(document("pending-payment/reject", endpoint(),
                        pathParameters(parameterWithName("paymentId").description("Payment ID"))
                ));
    }

    @Test
    void createPaymentCollection() throws Exception {
        AuthUser authUser = mockAuthUser();
        String jsonRequest = Tool.readResourceAsString("data/json/createPaymentCollectionRequest.json");
        when(pendingPaymentService.createPaymentCollection(any(PaymentCollectionView.class), eq(authUser))).thenReturn(
                new PaymentCollectionView());

        mockMvc.perform(post("/api/v1/payments/pending/collections", COLLECTION_ID).contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andDo(document("pending-payment/collection-create", endpoint(),
                        requestFields(
                                fieldWithPath("sessionId").description("Session ID"),
                                fieldWithPath("paymentMethodId").description("Payment method ID"),
                                fieldWithPath("channel").description("Channel"),
                                fieldWithPath("paymentIds").description("Array of Payment identifiers")
                        )
                ));
    }

    @Test
    void rejectPaymentCollection() throws Exception {
        AuthUser authUser = mockAuthUser();
        mockMvc.perform(delete("/api/v1/payments/pending/collections/{collectionId}/reject", COLLECTION_ID))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNoContent())
                .andDo(document("pending-payment/collection-reject", endpoint(),
                        pathParameters(parameterWithName("collectionId").description("Payment collection ID"))
                ));
        verify(pendingPaymentService).rejectPaymentCollection(COLLECTION_ID, authUser);
    }

    @Test
    void approvePaymentCollection() throws Exception {
        mockMvc.perform(post("/api/v1/payments/pending/collections/{collectionId}/approve", COLLECTION_ID))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andDo(document("pending-payment/collection-approve", endpoint(),
                        pathParameters(parameterWithName("collectionId").description("Payment collection ID"))
                ));
    }

    private static PaymentView createMockPayment() {
        return new PaymentView().setId(1).setStatus(PENDING).setCreatedDate(Tool.currentDateTime()).setAccountId(2).setAccountSide(DR)
                .setDescription("Some payment").setTaxDeclarationId(3).setCount(1).setTotal(new BigDecimal("1.0")).setTaxReasonId(4).setMeta(
                        new Tool.MapBuilder<String, Object>(new HashMap<>()).put(CREATED_ID, 5).put(MODIFIED_ID, 6).put(MODIFIED_DATE, "2022-08-10 11:40:45")
                                .put(APPROVED_ID, 7).put(APPROVED_DATE, "2022-08-10 11:40:45").put(APPROVED2_ID, 8).put(APPROVED2_DATE, "2022-08-10 11:40:45")
                                .put(REJECTED_ID, 9).put(REJECTED_DATE, "2022-08-10 11:40:45").put(IMPORT_TEMPLATE, RTGS_TEMPLATE)
                                .put("notifyTo", "263718733835").put("notifySubject", "Subject").put("notifyMessage", "Body")
                                .put("locationId", 1).put("channel", "WEB").put("notes", "Notes").build())
                .setDocuments(List.of(new PaymentDocument("99c9e1e0b3decc6f01f7c3bf322093ec", "payment_example_202009")))
                .setPaymentLines(List.of(new PaymentLineView().setId(7).setAccountId(8).setDetails("Payment line details")
                        .setAmount(new BigDecimal("1.0")).setCurrency("ZWL").setTransactionCode("TRN").setTransactionId(9).setMeta(Map.of(
                                "paymentMethod", "RTGS", "bankName", "FBC", "bankBin", "601704", "branchCode", "002",
                                "swiftCode", "FBCPZWH0", "bankAccountNo", "10099209810", "beneficiaryName", "Gordon Gangata",
                                "beneficiaryReference", "Test RTGS transaction"
                        ))));
    }

    private AuthUser mockAuthUser() {
        AuthUser authUser = new AuthUser();
        when(authUserService.getAuthUser()).thenReturn(authUser);
        return authUser;
    }
}
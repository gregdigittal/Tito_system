package cash.ice.api.controller;

import cash.ice.api.controller.zim.PaymentDocumentsRestController;
import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.PaymentDocumentContent;
import cash.ice.api.errors.ApiExceptionHandler;
import cash.ice.api.service.AuthUserService;
import cash.ice.api.service.PaymentDocumentsService;
import cash.ice.common.constant.IceCashProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static cash.ice.api.documentation.IcecashEndpointDocumentation.endpoint;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = PaymentDocumentsRestController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {PaymentDocumentsRestController.class, ApiExceptionHandler.class})
@AutoConfigureRestDocs(outputDir = "target/generated-snippets", uriScheme = "https", uriHost = "uat-gateway.icecash.mobi", uriPort = 80)
@ActiveProfiles(IceCashProfile.PROD)
class PaymentDocumentsControllerTest {
    private static final int PAYMENT_ID = 1;
    private static final int DOCUMENT_ID = 2;

    @MockBean
    private PaymentDocumentsService paymentDocumentsService;
    @MockBean
    private AuthUserService authUserService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadPaymentDocuments() throws Exception {
        String fileName = "documents.zip";
        byte[] content = "content".getBytes();
        MockMultipartFile file = new MockMultipartFile("files", fileName, APPLICATION_OCTET_STREAM_VALUE, content);
        AuthUser authUser = mockAuthUser();

        mockMvc.perform(multipart("/api/v1/payments/pending/{paymentId}/documents", PAYMENT_ID).file(file))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andDo(document("payment-document/upload", endpoint(),
                        pathParameters(parameterWithName("paymentId").description("Payment ID")),
                        requestParts(partWithName("files").description("The file to upload"))
                ));
        verify(paymentDocumentsService).uploadDocument(fileName, content, PAYMENT_ID, authUser);
    }

    @Test
    void getPaymentDocumentContent() throws Exception {
        AuthUser authUser = mockAuthUser();
        when(paymentDocumentsService.getDocumentContent(PAYMENT_ID, String.valueOf(DOCUMENT_ID), authUser)).thenReturn(
                new PaymentDocumentContent().setFilename("documents.zip").setContentType("image/png").setContent(new byte[]{1, 2, 3, 4}));
        mockMvc.perform(get("/api/v1/payments/pending/{paymentId}/documents/{documentId}", PAYMENT_ID, DOCUMENT_ID))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("payment-document/content", endpoint(),
                        pathParameters(
                                parameterWithName("paymentId").description("Database payment ID"),
                                parameterWithName("documentId").description("Database document ID")
                        )
                ));
    }

    @Test
    void deleteDocument() throws Exception {
        AuthUser authUser = mockAuthUser();
        mockMvc.perform(delete("/api/v1/payments/pending/{paymentId}/documents/{documentId}", PAYMENT_ID, DOCUMENT_ID))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNoContent())
                .andDo(document("payment-document/delete", endpoint(),
                        pathParameters(
                                parameterWithName("paymentId").description("Database document ID"),
                                parameterWithName("documentId").description("Database payment ID")
                        )
                ));
        verify(paymentDocumentsService).deleteDocument(PAYMENT_ID, String.valueOf(DOCUMENT_ID), authUser);
    }

    private AuthUser mockAuthUser() {
        AuthUser authUser = new AuthUser();
        when(authUserService.getAuthUser()).thenReturn(authUser);
        return authUser;
    }
}
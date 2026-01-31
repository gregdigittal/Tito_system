package cash.ice.api.controller;

import cash.ice.api.dto.DocumentContent;
import cash.ice.api.errors.ApiExceptionHandler;
import cash.ice.api.service.DocumentsService;
import cash.ice.api.service.JournalService;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.Document;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static cash.ice.api.documentation.IcecashEndpointDocumentation.endpoint;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedRequestFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = DocumentsRestController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {DocumentsRestController.class, ApiExceptionHandler.class})
@AutoConfigureRestDocs(outputDir = "target/generated-snippets", uriScheme = "https", uriHost = "uat-gateway.icecash.mobi", uriPort = 80)
class DocumentsControllerTest {

    @MockBean
    private DocumentsService documentsService;
    @MockBean
    private JournalService journalService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getDocuments() throws Exception {
        when(documentsService.getDocuments(false, 1, 1, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(
                        new Document().setId(1).setFileName("documents.zip").setDocumentTypeId(1).setEntityId(1).placeAddressId(1).setComments("Some comments"),
                        new Document().setId(2).setFileName("documents2.zip").setDocumentTypeId(2).setEntityId(1).placeAddressId(1).setComments("Some comments2")
                )));
        mockMvc.perform(get("/api/v1/documents")
                        .param("page", "0")
                        .param("size", "10")
                        .param("unassigned", "false")
                        .param("entityId", "1")
                        .param("addressId", "1"))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("document/get", endpoint(),
                        queryParameters(
                                parameterWithName("page").description("If provided, documents list page to return, by default page=0"),
                                parameterWithName("size").description("If provided, amount of documents to return on provided page, by default size=10"),
                                parameterWithName("unassigned").description("If provided, specifies which documents to return either signed or assigned"),
                                parameterWithName("entityId").description("If provided, specifies entityId assigned to the documents to return"),
                                parameterWithName("addressId").description("If provided, specifies addressId assigned to the documents to return")
                        )
                ));
    }

    @Test
    void getDocumentContent() throws Exception {
        when(documentsService.getDocumentContent(1)).thenReturn(new DocumentContent()
                .setDocument(new Document().setFileName("documents.zip")).setContent(new byte[]{1, 2, 3, 4}));
        mockMvc.perform(get("/api/v1/documents/{id}/download", 1))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("document/get-content", endpoint(),
                        pathParameters(parameterWithName("id").description("Database document ID"))
                ));
    }

    @Test
    void uploadDocument() throws Exception {
        String fileName = "documents.zip";
        byte[] content = "content".getBytes();
        MockMultipartFile file = new MockMultipartFile("files", fileName, APPLICATION_OCTET_STREAM_VALUE, content);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .file("entityId", "1".getBytes())
                        .file("addressId", "1".getBytes())
                        .file("typeId", "1".getBytes())
                        .file("comments", "Some comments".getBytes()))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andDo(document("document/upload", endpoint(),
                        requestParts(
                                partWithName("files").description("The file to upload"),
                                partWithName("entityId").description("If provided, assign the document to this entityId"),
                                partWithName("addressId").description("If provided, assign the document to this addressId"),
                                partWithName("typeId").description("If provided, set the document type"),
                                partWithName("comments").description("If provided, add comments to the document"))
                ));
    }

    @Test
    void updateDocument() throws Exception {
        String jsonRequest = Tool.readResourceAsString("data/json/updateDocument.json");

        mockMvc.perform(put("/api/v1/documents").contentType(APPLICATION_JSON).content(jsonRequest))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("document/update", endpoint(),
                        relaxedRequestFields(
                                fieldWithPath("id").description("ID of the document in the database, required"),
                                fieldWithPath("entityId").description("Assign the document to a new Entity ID"),
                                fieldWithPath("addressId").description("Assign the document to a new Address ID"),
                                fieldWithPath("documentTypeId").description("Set the document type"),
                                fieldWithPath("comments").description("Update the document comments")
                        )
                ));
    }

    @Test
    void deleteDocument() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/{id}", 1))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNoContent())
                .andDo(document("document/delete", endpoint(),
                        pathParameters(parameterWithName("id").description("Database document ID"))
                ));
    }
}
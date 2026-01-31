package cash.ice.api.service.impl;

import cash.ice.api.dto.DocumentContent;
import cash.ice.api.dto.DocumentView;
import cash.ice.api.errors.DocumentDownloadingException;
import cash.ice.api.errors.DocumentNotFoundException;
import cash.ice.api.errors.DocumentUploadingException;
import cash.ice.sqldb.entity.Document;
import cash.ice.sqldb.repository.*;
import io.minio.*;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static cash.ice.common.error.ErrorCodes.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentsServiceMinioImplTest {
    private static final String FILE_NAME = "TestFileName.png";
    private static final String BUCKET = "test";
    private static final String BASE_FOLDER = "/TestFolder/";

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private DocumentTypeRepository documentTypeRepository;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private MinioClient minioClient;
    @Captor
    private ArgumentCaptor<Example<Document>> exampleDocumentCaptor;
    @Captor
    private ArgumentCaptor<GetObjectArgs> getObjectCaptor;
    @Captor
    private ArgumentCaptor<PutObjectArgs> putObjectCaptor;
    @Captor
    private ArgumentCaptor<RemoveObjectArgs> removeObjectCaptor;
    @Captor
    private ArgumentCaptor<Document> documentCaptor;

    private DocumentsServiceMinioImpl service;

    @BeforeEach
    void init() {
        service = new DocumentsServiceMinioImpl(documentRepository, entityRepository, addressRepository,
                documentTypeRepository, countryRepository, minioClient);
        service.bucketName = BUCKET;
        service.baseFolder = BASE_FOLDER;
    }

    @Test
    void testGetUnassignedDocuments() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Document> page = new PageImpl<>(List.of(new Document(), new Document()));
        when(documentRepository.findUnassigned(pageable)).thenReturn(page);

        Page<Document> actualResult = service.getDocuments(true, null, null, pageable);
        assertThat(actualResult).isEqualTo(page);
    }

    @Test
    void testGetAssignedDocuments() {
        Integer entityId = 2;
        Integer addressId = 4;
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Document> page = new PageImpl<>(List.of(new Document(), new Document()));
        when(documentRepository.findDocuments(entityId, addressId, pageable)).thenReturn(page);

        Page<Document> actualResult = service.getDocuments(false, entityId, addressId, pageable);
        assertThat(actualResult).isEqualTo(page);
    }

    @Test
    void testGetDocumentContent() throws Exception {
        Integer documentId = 4;
        String path = "path";
        byte[] content = new byte[]{1, 2, 3, 4};
        Document document = new Document().setId(documentId).setFileName(FILE_NAME).setPath(path);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(
                new GetObjectResponse(null, null, null, null, new ByteArrayInputStream(content)));

        DocumentContent actualResponse = service.getDocumentContent(documentId);
        verify(minioClient).getObject(getObjectCaptor.capture());
        GetObjectArgs actualValue = getObjectCaptor.getValue();
        assertThat(actualValue.bucket()).isEqualTo(BUCKET);
        assertThat(actualValue.object()).isEqualTo(path);
        assertThat(actualResponse.getDocument()).isEqualTo(document);
        assertThat(actualResponse.getContentType()).isEqualTo("image/png");
        assertThat(actualResponse.getContent()).isEqualTo(content);
    }

    @Test
    void testGetDocumentContentNotFoundException() {
        DocumentNotFoundException actualException = assertThrows(DocumentNotFoundException.class,
                () -> service.getDocumentContent(1));
        AssertionsForClassTypes.assertThat(actualException.getErrorCode()).isEqualTo(EC1014);
    }

    @Test
    void testGetDocumentContentDownloadingException() throws Exception {
        Integer documentId = 4;
        Document document = new Document().setId(documentId).setFileName(FILE_NAME).setPath("path");
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(new RuntimeException());

        DocumentDownloadingException actualException = assertThrows(DocumentDownloadingException.class,
                () -> service.getDocumentContent(documentId));
        AssertionsForClassTypes.assertThat(actualException.getErrorCode()).isEqualTo(EC1016);
    }

    @Test
    void testUploadUnassignedDocument() throws Exception {
        byte[] contentRequest = new byte[]{1, 2, 3, 4};
        Document documentRequest = new Document().setFileName(FILE_NAME);

        service.uploadDocument(contentRequest, documentRequest);
        verify(documentRepository).save(documentRequest);
        verify(minioClient).putObject(putObjectCaptor.capture());
        PutObjectArgs putObject = putObjectCaptor.getValue();
        assertThat(putObject.bucket()).isEqualTo(BUCKET);
        assertThat(putObject.object()).isEqualTo(BASE_FOLDER + "08d6c05a21512a79a1dfeb9d2a8f262f/" + FILE_NAME);
        assertThat(putObject.contentType()).isEqualTo("image/png");
        assertThat(putObject.stream()).isNotNull();
        assertThat(documentRequest.getPath()).isEqualTo(BASE_FOLDER + "08d6c05a21512a79a1dfeb9d2a8f262f/" + FILE_NAME);
    }

    @Test
    void testUploadAssignedDocument() throws Exception {
        byte[] contentRequest = new byte[]{1, 2, 3, 4};
        Document documentRequest = new Document().setFileName(FILE_NAME)
                .setEntityId(1).placeAddressId(2).setDocumentTypeId(3).setComments("Some comments");
        when(entityRepository.existsById(1)).thenReturn(true);
        when(addressRepository.existsById(2)).thenReturn(true);
        when(documentTypeRepository.existsById(3)).thenReturn(true);

        service.uploadDocument(contentRequest, documentRequest);
        verify(documentRepository).save(documentRequest);
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void testUploadException() throws Exception {
        byte[] contentRequest = new byte[]{1, 2, 3, 4};
        Document documentRequest = new Document().setFileName(FILE_NAME);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenThrow(RuntimeException.class);

        DocumentUploadingException actualException = assertThrows(DocumentUploadingException.class,
                () -> service.uploadDocument(contentRequest, documentRequest));
        AssertionsForClassTypes.assertThat(actualException.getErrorCode()).isEqualTo(EC1015);
    }

    @Test
    void testUpdateDocument() {
        DocumentView documentRequest = new DocumentView().setId(1).setFileName(FILE_NAME)
                .setEntityId(2).setAddressId(3).setDocumentTypeId(4).setComments("New comments");
        when(documentRepository.findById(documentRequest.getId())).thenReturn(Optional.of(new Document()));
        when(entityRepository.existsById(2)).thenReturn(true);
        when(addressRepository.existsById(3)).thenReturn(true);
        when(documentTypeRepository.existsById(4)).thenReturn(true);

        service.updateDocument(documentRequest);
        verify(documentRepository).save(documentCaptor.capture());
        Document actualDocument = documentCaptor.getValue();
        assertThat(actualDocument.getComments()).isEqualTo("New comments");
    }

    @Test
    void testUpdateNotFoundException() {
        DocumentNotFoundException actualException = assertThrows(DocumentNotFoundException.class,
                () -> service.updateDocument(new DocumentView().setId(1)));
        AssertionsForClassTypes.assertThat(actualException.getErrorCode()).isEqualTo(EC1014);
    }

    @Test
    void testDeleteDocument() throws Exception {
        Integer documentId = 3;
        String path = "path";
        Document document = new Document().setId(documentId).setPath(path);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentRepository.findByPath(path)).thenReturn(List.of());

        service.deleteDocument(documentId);
        verify(minioClient).removeObject(removeObjectCaptor.capture());
        RemoveObjectArgs actualValue = removeObjectCaptor.getValue();
        assertThat(actualValue.bucket()).isEqualTo(BUCKET);
        assertThat(actualValue.object()).isEqualTo(path);
    }
}
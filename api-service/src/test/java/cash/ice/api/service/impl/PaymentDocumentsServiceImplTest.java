package cash.ice.api.service.impl;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.PaymentDocumentContent;
import cash.ice.api.entity.zim.Payment;
import cash.ice.api.errors.DocumentDownloadingException;
import cash.ice.api.repository.zim.PaymentRepository;
import cash.ice.api.service.PermissionsService;
import cash.ice.api.dto.PaymentDocument;
import io.minio.*;
import io.minio.errors.*;
import okhttp3.Headers;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static cash.ice.common.error.ErrorCodes.EC1016;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentDocumentsServiceImplTest {
    private static final String BUCKET = "test";
    private static final String BASE_FOLDER = "/TestFolder/";
    private static final int PAYMENT_ID = 1;
    private static final String DOCUMENT_ID = "2";
    private static final String FILENAME = "doc.png";
    private static final int ACCOUNT_ID = 3;

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PermissionsService permissionsService;
    @Mock
    private MinioClient minioClient;
    @Captor
    private ArgumentCaptor<GetObjectArgs> getObjectCaptor;
    @Captor
    private ArgumentCaptor<PutObjectArgs> putObjectCaptor;
    @Captor
    private ArgumentCaptor<RemoveObjectArgs> removeObjectCaptor;

    private PaymentDocumentsServiceImpl service;

    @BeforeEach
    void init() {
        service = new PaymentDocumentsServiceImpl(paymentRepository, permissionsService, minioClient);
        service.bucketName = BUCKET;
        service.baseFolder = BASE_FOLDER;
    }

    @Test
    void testGetDocumentContent() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        AuthUser authUser = new AuthUser();
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(new Payment().setAccountId(ACCOUNT_ID)
                .setDocuments(List.of(new PaymentDocument().setDocumentId(DOCUMENT_ID).setFilename(FILENAME)))));
        when(permissionsService.checkWriteRights(authUser, ACCOUNT_ID)).thenReturn(true);
        when(minioClient.getObject(getObjectCaptor.capture())).thenReturn(new GetObjectResponse(
                new Headers.Builder().build(), BUCKET, "region", "obj", InputStream.nullInputStream()));
        PaymentDocumentContent result = service.getDocumentContent(PAYMENT_ID, DOCUMENT_ID, authUser);
        GetObjectArgs args = getObjectCaptor.getValue();
        assertThat(args.bucket()).isEqualTo(BUCKET);
        assertThat(args.object()).isEqualTo(BASE_FOLDER + DOCUMENT_ID + "/" + FILENAME);
        assertThat(result.getFilename()).isEqualTo(FILENAME);
    }

    @Test
    void testGetDocumentContentError() {
        AuthUser authUser = new AuthUser();
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(new Payment().setAccountId(ACCOUNT_ID)
                .setDocuments(List.of(new PaymentDocument().setDocumentId(DOCUMENT_ID).setFilename(FILENAME)))));
        DocumentDownloadingException actualException = assertThrows(DocumentDownloadingException.class,
                () -> service.getDocumentContent(PAYMENT_ID, DOCUMENT_ID, authUser));
        AssertionsForClassTypes.assertThat(actualException.getErrorCode()).isEqualTo(EC1016);
    }

    @Test
    void testUploadDocument() throws Exception {
        AuthUser authUser = new AuthUser();
        byte[] contentRequest = new byte[]{1, 2, ACCOUNT_ID, 4};
        Payment payment = new Payment().setAccountId(ACCOUNT_ID);

        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(permissionsService.checkWriteRights(authUser, ACCOUNT_ID)).thenReturn(true);

        service.uploadDocument(FILENAME, contentRequest, PAYMENT_ID, authUser);
        verify(paymentRepository).save(payment);
        verify(minioClient).putObject(putObjectCaptor.capture());
        PutObjectArgs putObject = putObjectCaptor.getValue();
        assertThat(putObject.bucket()).isEqualTo(BUCKET);
        assertThat(putObject.object()).isEqualTo(BASE_FOLDER + "08d6c05a21512a79a1dfeb9d2a8f262f/" + FILENAME);
        assertThat(putObject.contentType()).isEqualTo("image/png");
        assertThat(putObject.stream()).isNotNull();
        PaymentDocument actualDocument = payment.getDocumentById("08d6c05a21512a79a1dfeb9d2a8f262f").orElse(null);
        assertThat(actualDocument).isNotNull();
        assertThat(actualDocument.getFilename()).isEqualTo(FILENAME);
    }

    @Test
    void testDeleteDocument() throws Exception {
        AuthUser authUser = new AuthUser();
        Payment payment = new Payment().setAccountId(ACCOUNT_ID).setDocuments(new ArrayList<>(List.of(
                new PaymentDocument().setDocumentId(DOCUMENT_ID).setFilename(FILENAME))));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(permissionsService.checkWriteRights(authUser, ACCOUNT_ID)).thenReturn(true);

        service.deleteDocument(PAYMENT_ID, DOCUMENT_ID, authUser);
        verify(paymentRepository).save(payment);
        verify(minioClient).removeObject(removeObjectCaptor.capture());
        RemoveObjectArgs removeObjectArgs = removeObjectCaptor.getValue();
        assertThat(removeObjectArgs.bucket()).isEqualTo(BUCKET);
        assertThat(removeObjectArgs.object()).isEqualTo(BASE_FOLDER + DOCUMENT_ID + "/" + FILENAME);
        PaymentDocument actualDocument = payment.getDocumentById(DOCUMENT_ID).orElse(null);
        assertThat(actualDocument).isNull();
    }
}
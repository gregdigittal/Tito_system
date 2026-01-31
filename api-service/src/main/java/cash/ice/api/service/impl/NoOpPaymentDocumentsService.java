package cash.ice.api.service.impl;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.PaymentDocumentContent;
import cash.ice.api.errors.DocumentDownloadingException;
import cash.ice.api.errors.DocumentNotFoundException;
import cash.ice.api.errors.DocumentUploadingException;
import cash.ice.api.service.PaymentDocumentsService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import io.minio.MinioClient;

/**
 * No-op implementation when Minio is not configured (e.g. Tito deploy without document storage).
 * All methods throw with a clear message to set minio.* properties.
 */
@Service
@ConditionalOnMissingBean(MinioClient.class)
public class NoOpPaymentDocumentsService implements PaymentDocumentsService {

    private static final String MSG = "Minio (object storage) is not configured. Set minio.url, minio.access-name, and minio.access-secret to enable payment document storage.";

    @Override
    public PaymentDocumentContent getDocumentContent(Integer paymentId, String documentId, AuthUser authUser) throws DocumentNotFoundException, DocumentDownloadingException {
        throw new DocumentDownloadingException(MSG, null);
    }

    @Override
    public void uploadDocument(String filename, byte[] content, Integer paymentId, AuthUser authUser) throws DocumentUploadingException {
        throw new DocumentUploadingException(MSG, null);
    }

    @Override
    public void deleteDocument(Integer paymentId, String documentId, AuthUser authUser) throws Exception {
        throw new IllegalStateException(MSG);
    }
}

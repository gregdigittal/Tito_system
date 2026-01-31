package cash.ice.api.service.impl;

import cash.ice.api.dto.DocumentContent;
import cash.ice.api.dto.DocumentView;
import cash.ice.api.errors.DocumentDownloadingException;
import cash.ice.api.errors.DocumentNotFoundException;
import cash.ice.api.errors.DocumentUploadingException;
import cash.ice.api.service.DocumentsService;
import cash.ice.sqldb.entity.Document;
import cash.ice.sqldb.entity.EntityClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import io.minio.MinioClient;

import java.util.List;

/**
 * No-op implementation when Minio is not configured (e.g. Tito deploy without document storage).
 * List/read operations return empty; upload and get content throw with a clear message.
 */
@Service
@ConditionalOnMissingBean(MinioClient.class)
public class DocumentsServiceNoOpImpl implements DocumentsService {

    private static final String MSG = "Minio (object storage) is not configured. Set minio.url, minio.access-name, and minio.access-secret to enable document storage.";

    @Override
    public Page<Document> getDocuments(boolean unassigned, Integer entityId, Integer addressId, Pageable pageable) {
        return Page.empty(pageable);
    }

    @Override
    public DocumentContent getDocumentContent(Integer documentId) throws DocumentNotFoundException, DocumentDownloadingException {
        throw new DocumentDownloadingException(MSG, null);
    }

    @Override
    public DocumentContent getKenDocumentContent(Integer entityId, String documentType) throws DocumentNotFoundException, DocumentDownloadingException {
        throw new DocumentDownloadingException(MSG, null);
    }

    @Override
    public Page<Document> getJournalsDocuments(List<Integer> journalIds, Pageable pageable) {
        return Page.empty(pageable);
    }

    @Override
    public Document uploadJournalDocument(byte[] content, Document document) throws DocumentUploadingException {
        throw new DocumentUploadingException(MSG, null);
    }

    @Override
    public Document uploadKenDocument(byte[] content, String documentType, Document document) throws DocumentUploadingException {
        throw new DocumentUploadingException(MSG, null);
    }

    @Override
    public void failoverDeleteJournalDocuments(Integer journalId) {
        // no-op when Minio not configured
    }

    @Override
    public Document uploadDocument(byte[] documentContent, Document document) throws DocumentUploadingException {
        throw new DocumentUploadingException(MSG, null);
    }

    @Override
    public void updateDocument(DocumentView documentView) {
        // no-op when Minio not configured (no storage to update)
    }

    @Override
    public void deleteDocument(Integer id) throws Exception {
        // no-op when Minio not configured
    }

    @Override
    public void deleteDocumentsByEntityId(Integer entityId) {
        // no-op when Minio not configured
    }

    @Override
    public void assignDocumentToEntity(EntityClass entity, Integer documentId) {
        // no-op when Minio not configured
    }
}

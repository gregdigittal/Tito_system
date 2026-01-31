package cash.ice.api.service;

import cash.ice.api.dto.DocumentContent;
import cash.ice.api.dto.DocumentView;
import cash.ice.api.errors.DocumentDownloadingException;
import cash.ice.api.errors.DocumentNotFoundException;
import cash.ice.api.errors.DocumentUploadingException;
import cash.ice.sqldb.entity.Document;
import cash.ice.sqldb.entity.EntityClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DocumentsService {

    Page<Document> getDocuments(boolean unassigned, Integer entityId, Integer addressId, Pageable pageable);

    DocumentContent getDocumentContent(Integer documentId) throws DocumentNotFoundException, DocumentDownloadingException;

    DocumentContent getKenDocumentContent(Integer entityId, String documentType) throws DocumentNotFoundException, DocumentDownloadingException;

    Page<Document> getJournalsDocuments(List<Integer> journalIds, Pageable pageable);

    Document uploadJournalDocument(byte[] content, Document document) throws DocumentUploadingException;

    Document uploadKenDocument(byte[] content, String documentType, Document document) throws DocumentUploadingException;

    void failoverDeleteJournalDocuments(Integer journalId);

    Document uploadDocument(byte[] documentContent, Document document) throws DocumentUploadingException;

    void updateDocument(DocumentView documentView);

    void deleteDocument(Integer id) throws Exception;

    void deleteDocumentsByEntityId(Integer entityId);

    void assignDocumentToEntity(EntityClass entity, Integer documentId);
}

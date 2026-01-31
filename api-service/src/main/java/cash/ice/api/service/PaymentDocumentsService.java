package cash.ice.api.service;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.PaymentDocumentContent;
import cash.ice.api.errors.DocumentDownloadingException;
import cash.ice.api.errors.DocumentNotFoundException;
import cash.ice.api.errors.DocumentUploadingException;

public interface PaymentDocumentsService {

    PaymentDocumentContent getDocumentContent(Integer paymentId, String documentId, AuthUser authUser) throws DocumentNotFoundException, DocumentDownloadingException;

    void uploadDocument(String filename, byte[] content, Integer paymentId, AuthUser authUser) throws DocumentUploadingException;

    void deleteDocument(Integer paymentId, String documentId, AuthUser authUser) throws Exception;
}

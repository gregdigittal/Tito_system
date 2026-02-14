package cash.ice.api.service.impl;

import cash.ice.api.dto.DocumentContent;
import cash.ice.api.dto.DocumentView;
import cash.ice.api.errors.*;
import cash.ice.api.service.DocumentsService;
import cash.ice.common.error.ApiValidationException;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.sqldb.entity.Country;
import cash.ice.sqldb.entity.Document;
import cash.ice.sqldb.entity.DocumentType;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.repository.*;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import java.util.List;

import io.minio.MinioClient;

import static cash.ice.common.error.ErrorCodes.EC1065;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBean(MinioClient.class)
public class DocumentsServiceMinioImpl implements DocumentsService {
    private final DocumentRepository documentRepository;
    private final EntityRepository entityRepository;
    private final AddressRepository addressRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final CountryRepository countryRepository;
    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    String bucketName;

    @Value("${minio.folder}")
    String baseFolder;

    @Override
    public Page<Document> getDocuments(boolean unassigned, Integer entityId, Integer addressId, Pageable pageable) {
        Page<Document> documents;
        if (unassigned) {
            documents = documentRepository.findUnassigned(pageable);
        } else {
            documents = documentRepository.findDocuments(entityId, addressId, pageable);
        }
        return documents;
    }

    @Override
    public DocumentContent getDocumentContent(Integer documentId) throws DocumentNotFoundException, DocumentDownloadingException {
        Document document = documentRepository.findById(documentId).orElseThrow(() ->
                new DocumentNotFoundException(documentId));
        return getDocumentContent(document);
    }

    public DocumentContent getKenDocumentContent(Integer entityId, String documentType) throws DocumentNotFoundException, DocumentDownloadingException {
        Country kenCountry = countryRepository.findByIsoCode(Country.KEN).orElseThrow(() ->
                new KenInternalException(String.format("Unknown country with '%s' ISO code", Country.KEN), EC1065));
        DocumentType docType = documentTypeRepository.findByNameAndCountryIdAndActive(documentType, kenCountry.getId(), true).orElseThrow(() ->
                new ApiValidationException(String.format("Active document type '%s' for '%s' country code does not exist", documentType, Country.KEN), ErrorCodes.EC1017));
        Document document = documentRepository.findByEntityIdAndDocumentTypeId(entityId, docType.getId()).orElseThrow(() ->
                new ApiValidationException(String.format("Document (typeId=%s) for entity (id=%s) not found", docType.getId(), entityId), ErrorCodes.EC1017));
        return getDocumentContent(document);
    }

    private DocumentContent getDocumentContent(Document document) throws DocumentNotFoundException, DocumentDownloadingException {
        try {
            GetObjectResponse minioResponse = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(document.getPath())
                    .build());
            return new DocumentContent()
                    .setDocument(document)
                    .setContentType(URLConnection.guessContentTypeFromName(document.getFileName()))
                    .setContent(IOUtils.toByteArray(minioResponse));
        } catch (Exception e) {
            throw new DocumentDownloadingException(e.getMessage(), e);
        }
    }

    @Override
    public Page<Document> getJournalsDocuments(List<Integer> journalIds, Pageable pageable) {
        return documentRepository.findJournalsDocuments(journalIds != null ? journalIds : List.of(), pageable);
    }

    @Transactional(timeout = 30)
    @Override
    public Document uploadJournalDocument(byte[] content, Document document) throws DocumentUploadingException {
        DocumentType journalType = documentTypeRepository.findByName("Journal").orElseThrow(() ->
                new ICEcashException("Document type 'Journal' not found", ErrorCodes.EC1017));
        document.setDocumentTypeId(journalType.getId());
        return uploadDocument(content, document);
    }

    @Transactional(timeout = 30)
    @Override
    public Document uploadKenDocument(byte[] content, String documentType, Document document) throws DocumentUploadingException {
        Country kenCountry = countryRepository.findByIsoCode(Country.KEN).orElseThrow(() ->
                new KenInternalException(String.format("Unknown country with '%s' ISO code", Country.KEN), EC1065));
        DocumentType docType = documentTypeRepository.findByNameAndCountryIdAndActive(documentType, kenCountry.getId(), true).orElseThrow(() ->
                new ApiValidationException(String.format("Active document type '%s' for '%s' country code not found", documentType, Country.KEN), ErrorCodes.EC1017));
        document.setDocumentTypeId(docType.getId());
        return uploadDocument(content, document);
    }

    @Override
    public void failoverDeleteJournalDocuments(Integer journalId) {
        try {
            Page<Document> documents = getJournalsDocuments(List.of(journalId), PageRequest.of(0, Integer.MAX_VALUE));
            for (Document document : documents) {
                deleteDocument(document);
            }
        } catch (Exception e) {
            log.error("Error deleting documents for journal(id={}): {}", journalId, e.getMessage(), e);
        }
    }

    @Transactional(timeout = 30)
    @Override
    public Document uploadDocument(byte[] content, Document document) throws DocumentUploadingException {
        checkDocumentIds(document);
        document.setPath(baseFolder + DigestUtils.md5DigestAsHex(content) + "/" + document.getFileName());
        Document result = documentRepository.save(document);
        try {
            createIfNeedMinioBucket();
            String contentType = URLConnection.guessContentTypeFromName(document.getFileName());
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(document.getPath())
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build());
            return result;
        } catch (Exception e) {
            throw new DocumentUploadingException(e.getMessage(), e);
        }
    }

    @Transactional(timeout = 30)
    @Override
    public void updateDocument(DocumentView documentView) {
        Document document = documentRepository.findById(documentView.getId()).orElseThrow(() ->
                new DocumentNotFoundException(documentView.getId()));
        document.setEntityId(documentView.getEntityId());
        document.placeAddressId(documentView.getAddressId());
        document.setDocumentTypeId(documentView.getDocumentTypeId());
        document.setComments(documentView.getComments());
        checkDocumentIds(document);
        documentRepository.save(document);
    }

    @Transactional(timeout = 30)
    @Override
    public void deleteDocument(Integer id) throws Exception {
        Document document = documentRepository.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
        deleteDocument(document);
    }

    private void deleteDocument(Document document) throws Exception {
        documentRepository.delete(document);
        List<Document> documents = documentRepository.findByPath(document.getPath());
        if (documents.isEmpty()) {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(document.getPath())
                    .build());
        }
    }

    @Override
    public void deleteDocumentsByEntityId(Integer entityId) {
        List<Document> documents = documentRepository.findByEntityId(entityId);
        documentRepository.deleteAll(documents);
        documents.forEach(document -> {
            List<Document> sameDocuments = documentRepository.findByPath(document.getPath());
            if (sameDocuments.isEmpty()) {
                try {
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(document.getPath())
                            .build());
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        });
    }

    public void assignDocumentToEntity(EntityClass entity, Integer documentId) {
        if (documentId != null) {
            log.debug("  assigning documentId: {} to entityId: {}", documentId, entity.getId());
            Document document = documentRepository.findById(documentId).orElseThrow(() ->
                    new DocumentNotFoundException(documentId));
            documentRepository.save(document.setEntityId(entity.getId()));
        }
    }

    private void createIfNeedMinioBucket() throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        }
    }

    private void checkDocumentIds(Document document) {
        if (document.getEntityId() != null && !entityRepository.existsById(document.getEntityId())) {
            throw new IllegalIdentifierException("Entity", document.getEntityId());
        }
        if (document.extractAddressId() != null && !addressRepository.existsById(document.extractAddressId())) {
            throw new IllegalIdentifierException("Address", document.extractAddressId());
        }
        if (document.getDocumentTypeId() != null && !documentTypeRepository.existsById(document.getDocumentTypeId())) {
            throw new IllegalIdentifierException("DocumentType", document.getDocumentTypeId());
        }
    }
}

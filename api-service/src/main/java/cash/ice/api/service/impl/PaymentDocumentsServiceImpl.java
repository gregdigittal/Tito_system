package cash.ice.api.service.impl;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.PaymentDocumentContent;
import cash.ice.api.entity.zim.Payment;
import cash.ice.api.errors.*;
import cash.ice.api.repository.zim.PaymentRepository;
import cash.ice.api.service.PaymentDocumentsService;
import cash.ice.api.service.PermissionsService;
import cash.ice.api.dto.PaymentDocument;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.util.DigestUtils;

import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import java.util.ArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBean(MinioClient.class)
public class PaymentDocumentsServiceImpl implements PaymentDocumentsService {
    private final PaymentRepository paymentRepository;
    private final PermissionsService permissionsService;
    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    String bucketName;

    @Value("${minio.payments-folder}")
    String baseFolder;

    @Override
    public PaymentDocumentContent getDocumentContent(Integer paymentId, String documentId, AuthUser authUser) throws DocumentNotFoundException, DocumentDownloadingException {
        try {
            Payment payment = getPayment(paymentId);
            if (!permissionsService.checkWriteRights(authUser, payment.getAccountId())) {
                throw new ForbiddenException("to get payment document content");
            }
            PaymentDocument paymentDocument = getPaymentDocument(payment, documentId);
            GetObjectResponse minioResponse = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(baseFolder + paymentDocument.getDocumentId() + "/" + paymentDocument.getFilename())
                    .build());
            return new PaymentDocumentContent()
                    .setFilename(paymentDocument.getFilename())
                    .setContentType(URLConnection.guessContentTypeFromName(paymentDocument.getFilename()))
                    .setContent(IOUtils.toByteArray(minioResponse));
        } catch (PaymentNotFoundException | DocumentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentDownloadingException(e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void uploadDocument(String filename, byte[] content, Integer paymentId, AuthUser authUser) throws DocumentUploadingException {
        Payment payment = getPayment(paymentId);
        if (!permissionsService.checkWriteRights(authUser, payment.getAccountId())) {
            throw new ForbiddenException("to upload payment document");
        }
        PaymentDocument paymentDocument = new PaymentDocument(DigestUtils.md5DigestAsHex(content), filename);
        if (payment.getDocuments() == null) {
            payment.setDocuments(new ArrayList<>());
        }
        if (payment.getDocumentById(paymentDocument.getDocumentId()).isEmpty()) {
            payment.getDocuments().add(paymentDocument);
            paymentRepository.save(payment);

            try {
                createIfNeedMinioBucket();
                String contentType = URLConnection.guessContentTypeFromName(paymentDocument.getFilename());
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(baseFolder + paymentDocument.getDocumentId() + "/" + paymentDocument.getFilename())
                        .stream(new ByteArrayInputStream(content), content.length, -1)
                        .contentType(contentType != null ? contentType : "application/octet-stream")
                        .build());
            } catch (Exception e) {
                throw new DocumentUploadingException(e.getMessage(), e);
            }
        }
    }

    @Override
    @Transactional
    public void deleteDocument(Integer paymentId, String documentId, AuthUser authUser) throws Exception {
        Payment payment = getPayment(paymentId);
        if (!permissionsService.checkWriteRights(authUser, payment.getAccountId())) {
            throw new ForbiddenException("to delete payment document");
        }
        PaymentDocument paymentDocument = getPaymentDocument(payment, documentId);
        payment.getDocuments().remove(paymentDocument);
        paymentRepository.save(payment);

        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(baseFolder + paymentDocument.getDocumentId() + "/" + paymentDocument.getFilename())
                .build());
    }

    private void createIfNeedMinioBucket() throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        }
    }

    private Payment getPayment(Integer paymentId) {
        return paymentRepository.findById(paymentId).orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    private PaymentDocument getPaymentDocument(Payment payment, String documentId) {
        return payment.getDocumentById(documentId).orElseThrow(() -> new DocumentNotFoundException(documentId));
    }
}

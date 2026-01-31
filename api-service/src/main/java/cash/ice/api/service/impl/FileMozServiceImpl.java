package cash.ice.api.service.impl;

import cash.ice.api.config.property.MozProperties;
import cash.ice.api.dto.FileContent;
import cash.ice.api.dto.moz.IdTypeMoz;
import cash.ice.api.errors.DocumentDownloadingException;
import cash.ice.api.errors.MozRegistrationException;
import cash.ice.api.errors.UnexistingUserException;
import cash.ice.api.service.DocumentsService;
import cash.ice.api.service.FileMozService;
import cash.ice.sqldb.entity.Document;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.repository.EntityRepository;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import java.util.Map;
import java.util.Objects;

import static cash.ice.common.error.ErrorCodes.EC1001;
import static cash.ice.common.error.ErrorCodes.EC1015;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileMozServiceImpl implements FileMozService {
    private final DocumentsService documentsService;
    private final EntityRepository entityRepository;
    private final MinioClient minioClient;
    private final MozProperties mozProperties;

    @Override
    public ObjectWriteResponse savePhoto(byte[] photo, String photoFileName, EntityClass entity) {
        if (photo == null) {
            return null;
        }
        try {
            createIfNeedMinioBucket();
            String path = mozProperties.getPhotoBaseFolder() + entity.getEmail() + "/" + entity.getId();
            String contentType = URLConnection.guessContentTypeFromName(photoFileName);
            log.debug("Saving photo: '{}', type: '{}', size: {}, bucket: '{}'", path, contentType, photo.length, mozProperties.getPhotoBucketName());
            return minioClient.putObject(PutObjectArgs.builder()
                    .bucket(mozProperties.getPhotoBucketName())
                    .object(path)
                    .userMetadata(Map.of("fileName", photoFileName))
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .stream(new ByteArrayInputStream(photo), photo.length, -1)
                    .build());
        } catch (Exception e) {
            throw new MozRegistrationException(EC1015, "Saving photo to minio failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void removePhoto(EntityClass entity) {
        try {
            String path = mozProperties.getPhotoBaseFolder() + entity.getEmail() + "/" + entity.getId();
            log.debug("Removing photo: '{}', bucket: '{}'", path, mozProperties.getPhotoBucketName());
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(mozProperties.getPhotoBucketName())
                    .object(mozProperties.getPhotoBaseFolder() + entity.getEmail() + "/" + entity.getId())
                    .build());
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    @Override
    public FileContent getPhoto(EntityClass entity) throws DocumentDownloadingException {
        try {
            GetObjectResponse minioResponse = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(mozProperties.getPhotoBucketName())
                    .object(mozProperties.getPhotoBaseFolder() + entity.getEmail() + "/" + entity.getId())
                    .build());
            String fileNameHeader = minioResponse.headers().get("x-amz-meta-filename");
            String fileName = fileNameHeader != null ? fileNameHeader : "photo";
            return new FileContent()
                    .setFileName(fileName)
                    .setContentType(URLConnection.guessContentTypeFromName(fileName))
                    .setContent(IOUtils.toByteArray(minioResponse));
        } catch (Exception e) {
            throw new DocumentDownloadingException(e.getMessage(), e);
        }
    }

    @Transactional
    @Override
    public Document uploadKycDocument(IdTypeMoz idType, String idNumber, byte[] bytes, Document documentData) {
        if (idType != null) {
            EntityClass entity = entityRepository.findById(documentData.getEntityId())
                    .orElseThrow(() -> new UnexistingUserException("id: " + documentData.getEntityId()));
            if (!Objects.equals(entity.getIdType(), idType.getDbId()) || !Objects.equals(entity.getIdNumber(), idNumber)) {
                if (mozProperties.isValidateIdUniqueness() && entityRepository.existsAccountByIdNumberAndIdType(idNumber, idType.getDbId())) {
                    throw new MozRegistrationException(EC1001, "Such ID already exists", true);
                }
                entityRepository.save(entity
                        .setIdType(idType.getDbId())
                        .setIdNumber(idNumber));
            }
        }
        return documentsService.uploadDocument(bytes, documentData);
    }

    private void createIfNeedMinioBucket() throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(mozProperties.getPhotoBucketName()).build())) {
            log.debug("Creating bucket: '{}'", mozProperties.getPhotoBucketName());
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(mozProperties.getPhotoBucketName())
                    .build());
        }
    }
}

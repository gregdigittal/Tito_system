package cash.ice.api.service.impl;

import cash.ice.api.dto.FileContent;
import cash.ice.api.dto.moz.IdTypeMoz;
import cash.ice.api.service.FileMozService;
import cash.ice.sqldb.entity.Document;
import cash.ice.sqldb.entity.EntityClass;
import io.minio.ObjectWriteResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import io.minio.MinioClient;

/**
 * No-op implementation when Minio is not configured (e.g. Tito deploy without document storage).
 * All methods throw with a clear message to set minio.* properties.
 */
@Service
@ConditionalOnMissingBean(MinioClient.class)
public class NoOpFileMozService implements FileMozService {

    private static final String MSG = "Minio (object storage) is not configured. Set minio.url, minio.access-name, and minio.access-secret to enable file storage.";

    @Override
    public ObjectWriteResponse savePhoto(byte[] photo, String photoFileName, EntityClass entity) {
        throw new IllegalStateException(MSG);
    }

    @Override
    public void removePhoto(EntityClass entity) {
        // no-op when Minio not configured
    }

    @Override
    public FileContent getPhoto(EntityClass entity) {
        throw new IllegalStateException(MSG);
    }

    @Override
    public Document uploadKycDocument(IdTypeMoz idType, String idNumber, byte[] bytes, Document document) {
        throw new IllegalStateException(MSG);
    }
}

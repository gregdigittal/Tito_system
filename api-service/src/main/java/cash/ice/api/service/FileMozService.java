package cash.ice.api.service;

import cash.ice.api.dto.FileContent;
import cash.ice.api.dto.moz.IdTypeMoz;
import cash.ice.sqldb.entity.Document;
import cash.ice.sqldb.entity.EntityClass;
import io.minio.ObjectWriteResponse;

public interface FileMozService {

    ObjectWriteResponse savePhoto(byte[] photo, String photoFileName, EntityClass entity);

    void removePhoto(EntityClass entity);

    FileContent getPhoto(EntityClass entity);

    Document uploadKycDocument(IdTypeMoz idType, String idNumber, byte[] bytes, Document document);
}

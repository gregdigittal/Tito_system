package cash.ice.api.dto;

import cash.ice.common.dto.ResponseStatus;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DocumentUploadResponse {
    private ResponseStatus status = ResponseStatus.SUCCESS;
    private Integer documentId;

    public DocumentUploadResponse(Integer documentId) {
        this.documentId = documentId;
    }
}

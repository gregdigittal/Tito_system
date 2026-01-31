package cash.ice.api.dto;

import cash.ice.sqldb.entity.Document;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class DocumentView {
    @NotNull
    private Integer id;
    private String fileName;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer documentTypeId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer entityId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer addressId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String comments;
    private LocalDateTime createdDate;

    public static DocumentView create(Document document) {
        return new DocumentView()
                .setId(document.getId())
                .setFileName(document.getFileName())
                .setDocumentTypeId(document.getDocumentTypeId())
                .setEntityId(document.getEntityId())
                .setAddressId(document.extractAddressId())
                .setComments(document.getComments())
                .setCreatedDate(document.getCreatedDate());
    }
}

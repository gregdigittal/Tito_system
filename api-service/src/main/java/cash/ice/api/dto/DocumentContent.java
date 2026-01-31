package cash.ice.api.dto;

import cash.ice.sqldb.entity.Document;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DocumentContent {
    private Document document;
    private String contentType;
    private byte[] content;

    public String getFileName() {
        return document.getFileName();
    }

    public int getContentLength() {
        return content == null ? 0 : content.length;
    }
}

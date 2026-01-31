package cash.ice.api.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PaymentDocumentContent {
    private String filename;
    private String contentType;
    private byte[] content;

    public int getContentLength() {
        return content == null ? 0 : content.length;
    }
}

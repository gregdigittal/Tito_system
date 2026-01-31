package cash.ice.emola.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EmolaResponse {
    private String errorCode;
    private String description;
    private String gwtransid;
    private String originalErrorCode;
    private String originalResponseCode;
    private String originalMessage;
    private String originalOrgResponseCode;
    private String originalOrgResponseMessage;
    private String originalBalance;
    private String originalRequestId;
}

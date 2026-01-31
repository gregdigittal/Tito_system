package cash.ice.fbc.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class FbcTransferSubmissionResponse {
    private Integer statusCode;
    private Response response;

    private Instant timestamp;
    private Integer status;
    private String error;

    @Data
    @Accessors(chain = true)
    public static class Response {
        private String resultCode;
        private String resultDescription;
        private String hostReference;
        private String externalReference;
        private String branchCode;
        private String phoneNumber;
        private String accountNumber;
    }
}

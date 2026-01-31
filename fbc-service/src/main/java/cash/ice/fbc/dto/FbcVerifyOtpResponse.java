package cash.ice.fbc.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class FbcVerifyOtpResponse {
    private Integer statusCode;
    private String response;

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}

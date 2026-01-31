package cash.ice.api.dto.moz;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class LinkNfcTagRequest {
    @Deprecated
    private String requestId;
    @Deprecated
    private String deviceSerial;
    private String device;
    private String accountNumber;
    private String otp;
    private String tagNumber;
    private LocalDateTime dateTime;
}

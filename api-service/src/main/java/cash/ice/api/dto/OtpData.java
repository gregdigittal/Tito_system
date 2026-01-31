package cash.ice.api.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "OtpData")
@Data
@Accessors(chain = true)
public class OtpData {
    @Id
    private String id;
    private OtpType otpType;
    private String msisdn;
    private Integer entityId;
    private String otpKey;
    private String otpPvv;
    private Instant createdDate;
}

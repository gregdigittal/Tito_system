package cash.ice.api.dto.moz;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "MozLinkTagData")
@Data
@Accessors(chain = true)
public class MozLinkTagData {
    @Id
    private String id;
    private String requestId;
    private String device;
    private String accountNumber;
    private int accountId;
    private Integer subsidyAccountId;
    private int initiatorTypeId;
    private int initiatorCategoryId;
    private Integer activeInitiatorStatusId;
    private Integer unassignedInitiatorStatusId;
    private String otpKey;
    private String otpPvv;
    private boolean otpValidated;
    private String firstName;
    private String lastName;
    private Instant createdDate;
}

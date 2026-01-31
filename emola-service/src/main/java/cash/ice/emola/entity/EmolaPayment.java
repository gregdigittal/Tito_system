package cash.ice.emola.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "EmolaPayment")
@Data
@Accessors(chain = true)
public class EmolaPayment {

    @Id
    private String id;

    private String vendorRef;
    private String transactionCode;
    private String transactionId;
    private String responseErrorCode;
    private String responseDescription;
    private String responseGwtransid;
    private String responseOriginalErrorCode;
    private String responseOriginalMessage;
    private String responseOriginalRequestId;
    private String msisdn;
    private String errorCode;
    private String errorMessage;
    private BigDecimal amount;
    private boolean refunded;
    private Instant createdTime;
    private Instant updatedTime;

    public boolean isFinishedPayment() {
        return "0".equals(responseErrorCode);
    }
}

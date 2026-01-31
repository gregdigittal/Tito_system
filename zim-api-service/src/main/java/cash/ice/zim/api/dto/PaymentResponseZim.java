package cash.ice.zim.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class PaymentResponseZim {

    @Id
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String vendorRef;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ResponseStatus status;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCode;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String mobile;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String externalTransactionId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime date;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object spResult;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean tryingToRefund;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private int spTries;

    @JsonIgnore
    private String bankName;
    @JsonIgnore
    private Instant lastSpTry;

    public void incrementSpTries() {
        spTries++;
    }

    public static PaymentResponseZim makeError(String vendorRef, String message, String errorCode, LocalDateTime date) {
        return new PaymentResponseZim().setVendorRef(vendorRef)
                .setStatus(ResponseStatus.ERROR)
                .setMessage(message)
                .setErrorCode(errorCode)
                .setDate(date);
    }
}

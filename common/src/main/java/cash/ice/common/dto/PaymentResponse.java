package cash.ice.common.dto;

import cash.ice.common.utils.Tool;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Accessors(chain = true)
public class PaymentResponse {
    @Id
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String vendorRef;
    private LocalDateTime date;
    private ResponseStatus status;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCode;
    private String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> payload;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String transactionId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal balance;
    @JsonIgnore
    private String primaryMsisdn;
    @JsonIgnore
    private String locale;

    protected PaymentResponse() {
    }

    public static PaymentResponse success(String vendorRef, String transactionId, BigDecimal balance, String primaryMsisdn, String locale) {
        PaymentResponse response = new PaymentResponse();
        response.vendorRef = vendorRef;
        response.date = Tool.currentDateTime();
        response.status = ResponseStatus.SUCCESS;
        response.message = "Transaction processed successfully";
        response.transactionId = transactionId;
        response.balance = balance;
        response.primaryMsisdn = primaryMsisdn;
        response.locale = locale;
        return response;
    }

    public static PaymentResponse subResult(String vendorRef, Map<String, Object> payload) {
        PaymentResponse response = new PaymentResponse();
        response.vendorRef = vendorRef;
        response.date = Tool.currentDateTime();
        response.status = ResponseStatus.PROCESSING;
        response.message = "Operation is in progress";
        response.payload = payload;
        return response;
    }

    public static PaymentResponse error(String vendorRef, String errorCode, String errorMessage) {
        PaymentResponse response = new PaymentResponse();
        response.vendorRef = vendorRef;
        response.date = Tool.currentDateTime();
        response.status = ResponseStatus.ERROR;
        response.errorCode = errorCode;
        response.message = errorMessage;
        return response;
    }

    public static PaymentResponse processing(String vendorRef) {
        PaymentResponse response = new PaymentResponse();
        response.vendorRef = vendorRef;
        response.date = Tool.currentDateTime();
        response.status = ResponseStatus.PROCESSING;
        response.message = "Operation is in progress";
        return response;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getPayloadData() {
        return payload;
    }

    public void setPayloadData(String jsonString) {
        this.payload = Tool.jsonStringToMap(jsonString);
    }
}

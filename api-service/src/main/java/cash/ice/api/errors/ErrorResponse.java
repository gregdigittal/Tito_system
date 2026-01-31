package cash.ice.api.errors;

import cash.ice.common.dto.ResponseStatus;
import cash.ice.common.dto.VendorRefAware;
import cash.ice.common.utils.Tool;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorResponse implements VendorRefAware {
    private ResponseStatus status = ResponseStatus.ERROR;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String vendorRef;
    private String errorCode;
    private String message;
    private LocalDateTime date;

    public ErrorResponse(String errorCode, String message) {
        this(null, errorCode, message);
    }

    public ErrorResponse(String vendorRef, String errorCode, String message) {
        this.vendorRef = vendorRef;
        this.date = Tool.currentDateTime();
        this.errorCode = errorCode;
        this.message = message;
    }
}

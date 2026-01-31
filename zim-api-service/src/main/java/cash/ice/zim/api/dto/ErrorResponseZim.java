package cash.ice.zim.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@NoArgsConstructor
public class ErrorResponseZim {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String vendorRef;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ResponseStatus status;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCode;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime date;

    public ErrorResponseZim(String errorCode, String message, LocalDateTime date) {
        this.status = ResponseStatus.ERROR;
        this.errorCode = errorCode;
        this.message = message;
        this.date = date;
    }
}

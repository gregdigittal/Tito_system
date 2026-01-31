package cash.ice.api.dto.moz;

import cash.ice.common.dto.ResponseStatus;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.EntityClass;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class RegisterKenResponse {
    private LocalDateTime date;
    private ResponseStatus status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCode;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String accountNumber;

    @JsonIgnore
    private EntityClass entity;

    private RegisterKenResponse() {
    }

    public static RegisterKenResponse success(String accountNumber, EntityClass entity) {
        RegisterKenResponse response = new RegisterKenResponse();
        response.date = Tool.currentDateTime();
        response.status = ResponseStatus.SUCCESS;
        response.message = "Registration processed successfully";
        response.accountNumber = accountNumber;
        response.entity = entity;
        return response;
    }

    public static RegisterKenResponse error(String errorCode, String errorMessage) {
        RegisterKenResponse response = new RegisterKenResponse();
        response.date = Tool.currentDateTime();
        response.status = ResponseStatus.ERROR;
        response.errorCode = errorCode;
        response.message = errorMessage;
        return response;
    }
}

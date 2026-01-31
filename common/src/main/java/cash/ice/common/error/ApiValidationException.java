package cash.ice.common.error;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiValidationException extends ICEcashException {

    public ApiValidationException(String message, String errorCode) {
        super(message, errorCode, false);
    }

    public ApiValidationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}

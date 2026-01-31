package cash.ice.common.error;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ICEcashException extends RuntimeException {
    private final String errorCode;
    private final boolean internalError;

    public ICEcashException(String message, String errorCode) {
        this(message, errorCode, false);
    }

    public ICEcashException(String message, String errorCode, boolean internalError) {
        super(message);
        this.errorCode = errorCode;
        this.internalError = internalError;
    }

    public ICEcashException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.internalError = false;
    }
}

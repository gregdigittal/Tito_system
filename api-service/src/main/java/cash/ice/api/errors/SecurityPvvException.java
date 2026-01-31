package cash.ice.api.errors;

import cash.ice.common.error.ICEcashException;

public class SecurityPvvException extends ICEcashException {

    public SecurityPvvException(String errorCode, String message) {
        super(message, errorCode, true);
    }

    public SecurityPvvException(String errorCode, String message, Throwable cause) {
        super(message, errorCode, cause);
    }
}

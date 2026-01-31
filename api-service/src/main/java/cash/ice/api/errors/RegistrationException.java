package cash.ice.api.errors;

import cash.ice.common.error.ICEcashException;

public class RegistrationException extends ICEcashException {

    public RegistrationException(String errorCode, String message) {
        super(message, errorCode, true);
    }

    public RegistrationException(String errorCode, String message, Throwable cause) {
        super(message, errorCode, cause);
    }
}

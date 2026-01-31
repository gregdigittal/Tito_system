package cash.ice.api.errors;

import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;

public class ForbiddenException extends ICEcashException {

    public ForbiddenException(String message) {
        super(String.format("No permissions %s", message), ErrorCodes.EC1021);
    }

    public ForbiddenException(String message, String errorCode) {
        super(message, errorCode);
    }
}


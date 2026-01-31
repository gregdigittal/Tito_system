package cash.ice.api.errors;

import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;

public class UnexistingUserException extends ICEcashException {

    public UnexistingUserException(String enterId) {
        super(String.format("User '%s' does not exist", enterId), ErrorCodes.EC1010, false);
    }

    public UnexistingUserException() {
        super("User not authorized", ErrorCodes.EC1010, false);
    }
}

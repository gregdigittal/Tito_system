package cash.ice.zim.api.error;

import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;

public class ApiValidationException extends ICEcashException {

    public ApiValidationException(String message) {
        super(message, ErrorCodes.EC1102);
    }
}

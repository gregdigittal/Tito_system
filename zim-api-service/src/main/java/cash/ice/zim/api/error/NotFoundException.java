package cash.ice.zim.api.error;

import cash.ice.common.error.ICEcashException;

public class NotFoundException extends ICEcashException {

    public NotFoundException(String message, String errorCode) {
        super(message, errorCode);
    }
}

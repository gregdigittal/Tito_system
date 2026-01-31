package cash.ice.fee.error;

import cash.ice.common.error.ICEcashException;

public class ICEcashGroupApiException extends ICEcashException {

    public ICEcashGroupApiException(String message, String errorCode) {
        super(message, errorCode, true);
    }

    public ICEcashGroupApiException(String message, String receivedErrorCode, String errorCode) {
        super(String.format("error: %s, result: %s", message, receivedErrorCode), errorCode, true);
    }
}

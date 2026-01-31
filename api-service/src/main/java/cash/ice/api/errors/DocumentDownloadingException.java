package cash.ice.api.errors;

import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;

public class DocumentDownloadingException extends ICEcashException {

    public DocumentDownloadingException(String message, Throwable throwable) {
        super(message, ErrorCodes.EC1016, throwable);
    }
}

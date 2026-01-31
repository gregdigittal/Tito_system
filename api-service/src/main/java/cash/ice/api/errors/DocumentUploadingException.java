package cash.ice.api.errors;

import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;

public class DocumentUploadingException extends ICEcashException {

    public DocumentUploadingException(String message, Throwable throwable) {
        super(message, ErrorCodes.EC1015, throwable);
    }
}

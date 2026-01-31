package cash.ice.fee.error;

import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;

import java.util.function.Supplier;

public class ICEcashInvalidRequestException extends ICEcashException {

    public ICEcashInvalidRequestException(String message, String errorCode) {
        super(message, errorCode, false);
    }

    public ICEcashInvalidRequestException(String message, String errorCode, boolean internalError) {
        super(message, errorCode, internalError);
    }

    public static Supplier<ICEcashException> with(String argName, String argValue) {
        return with(argName, argValue, ErrorCodes.EC3002);
    }

    public static Supplier<ICEcashException> with(String argName, String argValue, String errorCode) {
        return () -> new ICEcashInvalidRequestException(String.format("Invalid %s requested: %s", argName, argValue),
                errorCode);
    }
}

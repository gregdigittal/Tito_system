package cash.ice.api.errors;

import cash.ice.common.error.ICEcashException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KenInternalException extends ICEcashException {

    public KenInternalException(String message, String errorCode) {
        super(message, errorCode, false);
    }

    public KenInternalException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}

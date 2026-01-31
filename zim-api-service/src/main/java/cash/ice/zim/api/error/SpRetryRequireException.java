package cash.ice.zim.api.error;

import cash.ice.common.error.ICEcashException;
import lombok.Getter;

@Getter
public class SpRetryRequireException extends ICEcashException {

    public SpRetryRequireException(String message, String errorCode) {
        super(message, errorCode, false);
    }
}

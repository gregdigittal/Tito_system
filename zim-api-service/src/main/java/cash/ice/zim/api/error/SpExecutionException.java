package cash.ice.zim.api.error;

import cash.ice.common.error.ICEcashException;
import lombok.Getter;

@Getter
public class SpExecutionException extends ICEcashException {

    public SpExecutionException(String message, String errorCode) {
        super(message, errorCode, false);
    }
}

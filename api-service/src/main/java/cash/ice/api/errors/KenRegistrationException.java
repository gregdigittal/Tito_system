package cash.ice.api.errors;

import cash.ice.common.error.ICEcashException;
import lombok.Getter;

@Getter
public class KenRegistrationException extends ICEcashException {
    private final boolean sendMessageToClient;

    public KenRegistrationException(String errorCode, String message, boolean sendMessageToClient) {
        super(message, errorCode, true);
        this.sendMessageToClient = sendMessageToClient;
    }

    public KenRegistrationException(String errorCode, String message, Throwable cause) {
        super(message, errorCode, cause);
        this.sendMessageToClient = false;
    }
}

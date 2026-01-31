package cash.ice.api.errors;

import cash.ice.common.error.ICEcashException;
import lombok.Getter;

@Getter
public class Me60Exception extends ICEcashException {
    private String details;

    public Me60Exception(String message, String errorCode) {
        super(message, errorCode);
    }

    public Me60Exception(String message, String details, String errorCode) {
        super(message, errorCode, false);
        this.details = details;
    }
}

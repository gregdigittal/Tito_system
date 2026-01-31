package cash.ice.onemoney.error;

import cash.ice.common.error.ICEcashException;
import cash.ice.onemoney.entity.OnemoneyPayment;
import lombok.Getter;

@Getter
public class OnemoneyException extends ICEcashException {
    private final OnemoneyPayment onemoneyPayment;

    public OnemoneyException(String message, String errorCode) {
        this(null, message, errorCode);
    }

    public OnemoneyException(OnemoneyPayment onemoneyPayment, String message, String errorCode) {
        super(message, errorCode);
        this.onemoneyPayment = onemoneyPayment;
    }
}

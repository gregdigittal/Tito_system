package cash.ice.fbc.error;

import cash.ice.common.error.ICEcashException;
import cash.ice.fbc.entity.FlexcubePayment;
import lombok.Getter;

@Getter
public class FlexcubeException extends ICEcashException {
    private final FlexcubePayment flexcubePayment;

    public FlexcubeException(FlexcubePayment flexcubePayment, String message, String errorCode) {
        super(message, errorCode);
        this.flexcubePayment = flexcubePayment;
    }
}

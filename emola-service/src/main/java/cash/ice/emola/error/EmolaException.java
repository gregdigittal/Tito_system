package cash.ice.emola.error;

import cash.ice.common.error.ICEcashException;
import cash.ice.emola.entity.EmolaPayment;
import lombok.Getter;

@Getter
public class EmolaException extends ICEcashException {
    private final EmolaPayment emolaPayment;

    public EmolaException(EmolaPayment emolaPayment, String message, String errorCode) {
        super(message, errorCode);
        this.emolaPayment = emolaPayment;
    }
}

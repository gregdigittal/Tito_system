package cash.ice.api.errors;

import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;

public class PaymentNotFoundException extends ICEcashException {

    public PaymentNotFoundException(Integer paymentId) {
        super(String.format("Payment with id: %s does not exist", paymentId), ErrorCodes.EC1018);
    }
}
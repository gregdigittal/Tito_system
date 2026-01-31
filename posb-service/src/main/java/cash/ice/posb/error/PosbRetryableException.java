package cash.ice.posb.error;

import cash.ice.posb.dto.PosbPayment;

public class PosbRetryableException extends PosbException {

    public PosbRetryableException(PosbPayment posbPayment, String message, Throwable cause) {
        super(posbPayment, message, cause);
    }
}

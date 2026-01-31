package error;

import cash.ice.common.error.ICEcashException;
import cash.ice.ecocash.entity.EcocashPayment;
import lombok.Getter;

@Getter
public class EcocashException extends ICEcashException {
    private final EcocashPayment ecocashPayment;

    public EcocashException(String message, String errorCode) {
        this(null, message, errorCode);
    }

    public EcocashException(EcocashPayment ecocashPayment, String message, String errorCode) {
        super(message, errorCode);
        this.ecocashPayment = ecocashPayment;
    }

    public EcocashException(EcocashPayment ecocashPayment, String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
        this.ecocashPayment = ecocashPayment;
    }

    public String getVendorRef() {
        return ecocashPayment != null ? ecocashPayment.getVendorRef() : null;
    }

    public String getCauseCanonicalName() {
        return getCause() != null ? getCause().getClass().getCanonicalName() : null;
    }
}

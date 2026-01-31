package cash.ice.fbc.error;

import cash.ice.fbc.entity.FbcPayment;
import lombok.Getter;

@Getter
public class FbcException extends RuntimeException {
    private FbcPayment fbcPayment;
    private String vendorRef;

    public FbcException(FbcPayment fbcPayment, String message, Throwable cause) {
        super(message, cause);
        this.fbcPayment = fbcPayment;
    }

    public FbcException(String vendorRef, String message) {
        super(message);
        this.vendorRef = vendorRef;
    }

    public String getVendorRef() {
        return fbcPayment != null ? fbcPayment.getVendorRef() : vendorRef;
    }

    public String getCauseCanonicalName() {
        return getCause() != null ? getCause().getClass().getCanonicalName() : null;
    }
}

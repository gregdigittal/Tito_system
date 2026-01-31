package cash.ice.mpesa.error;

import cash.ice.common.error.ICEcashException;
import cash.ice.mpesa.entity.MpesaPayment;
import lombok.Getter;

@Getter
public class MpesaException extends ICEcashException {
    private MpesaPayment mpesaPayment;
    private String vendorRef;

    public MpesaException(MpesaPayment mpesaPayment, String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
        this.mpesaPayment = mpesaPayment;
    }

    public MpesaException(MpesaPayment mpesaPayment, String message, String errorCode) {
        super(message, errorCode);
        this.mpesaPayment = mpesaPayment;
    }

    public MpesaException(String vendorRef, String message, String errorCode) {
        super(message, errorCode);
        this.vendorRef = vendorRef;
    }

    public String getVendorRef() {
        return mpesaPayment != null ? mpesaPayment.getVendorRef() : vendorRef;
    }

    public String getCauseCanonicalName() {
        return getCause() != null ? getCause().getClass().getCanonicalName() : null;
    }
}

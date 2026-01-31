package cash.ice.posb.error;

import cash.ice.posb.dto.PosbPayment;
import lombok.Getter;

@Getter
public class PosbException extends RuntimeException {
    private PosbPayment posbPayment;
    private String vendorRef;

    public PosbException(PosbPayment posbPayment, String message, Throwable cause) {
        super(message, cause);
        this.posbPayment = posbPayment;
    }

    public PosbException(String vendorRef, String message) {
        super(message);
        this.vendorRef = vendorRef;
    }

    public String getVendorRef() {
        return posbPayment != null ? posbPayment.getVendorRef() : vendorRef;
    }
}

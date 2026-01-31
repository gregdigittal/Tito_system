package cash.ice.api.errors;

import lombok.Getter;

@Getter
public class ApiPaymentException extends RuntimeException {
    private final String vendorRef;
    private final String errorCode;

    public ApiPaymentException(String vendorRef, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.vendorRef = vendorRef;
        this.errorCode = errorCode;
    }
}

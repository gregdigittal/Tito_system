package cash.ice.paygo.error;

import lombok.Getter;

@Getter
public class PaygoException extends RuntimeException {
    private final String payGoId;
    private final String deviceReference;

    public PaygoException(Throwable e, String payGoId, String deviceReference) {
        super(e);
        this.payGoId = payGoId;
        this.deviceReference = deviceReference;
    }
}

package cash.ice.zim.api.error;

import cash.ice.common.error.ICEcashException;
import lombok.Getter;

@Getter
public class AfterLedgerException extends ICEcashException {
    private final Object spResult;

    public AfterLedgerException(Exception e, Object spResult) {
        super(e.getMessage(), null, e);
        this.spResult = spResult;
    }
}

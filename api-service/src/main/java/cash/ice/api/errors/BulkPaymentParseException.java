package cash.ice.api.errors;

import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import lombok.Getter;

@Getter
public class BulkPaymentParseException extends ICEcashException {
    private Integer row;
    private Integer col;

    public BulkPaymentParseException(int row, int col, String message) {
        super(message, ErrorCodes.EC1019);
        this.row = row;
        this.col = col;
    }

    public BulkPaymentParseException(String message) {
        super(message, ErrorCodes.EC1020);
    }
}
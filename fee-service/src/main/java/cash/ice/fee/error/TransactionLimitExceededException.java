package cash.ice.fee.error;

import cash.ice.common.error.ICEcashException;

import java.math.BigDecimal;

import static cash.ice.common.error.ErrorCodes.EC3029;

public class TransactionLimitExceededException extends ICEcashException {

    public TransactionLimitExceededException(String type, BigDecimal limit, BigDecimal amount) {
        super(String.format("%s transaction limit check failed (%s) for amount: %s", type, limit, amount), EC3029, true);
    }

    public TransactionLimitExceededException(String type) {
        super(String.format("%s transaction limit exceeded", type), EC3029, true);
    }
}

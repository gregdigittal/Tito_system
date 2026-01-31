package cash.ice.fee.error;

import cash.ice.common.error.ICEcashException;

import static cash.ice.common.error.ErrorCodes.EC3004;

public class ICEcashKycRequiredException extends ICEcashException {

    public ICEcashKycRequiredException(String transactionCode, Integer entityId) {
        super(String.format("Invalid KYC for transactionCode: %s, entityId: %s", transactionCode, entityId), EC3004, true);
    }
}

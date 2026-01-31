package cash.ice.common.error;

public class ICEcashWrongBalanceException extends ICEcashException {

    public ICEcashWrongBalanceException(String message, String errorCode) {
        super(message, errorCode);
    }
}

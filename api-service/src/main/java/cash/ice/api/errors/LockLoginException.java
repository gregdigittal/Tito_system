package cash.ice.api.errors;

import jakarta.ws.rs.NotAuthorizedException;

public class LockLoginException extends Exception {

    public LockLoginException(NotAuthorizedException exception) {
        super(exception);
    }

    public NotAuthorizedException getInitialException() {
        return (NotAuthorizedException) getCause();
    }
}

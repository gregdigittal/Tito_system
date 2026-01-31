package cash.ice.api.errors;

import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;

public class IllegalIdentifierException extends ICEcashException {

    public IllegalIdentifierException(String entityName, Integer id) {
        super(String.format("%s with id = %s does not exist", entityName, id), ErrorCodes.EC1017);
    }
}

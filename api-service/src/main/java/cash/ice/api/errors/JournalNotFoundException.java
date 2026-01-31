package cash.ice.api.errors;

import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;

public class JournalNotFoundException extends ICEcashException {

    public JournalNotFoundException(Integer journalId) {
        super(String.format("Journal with id: %s does not exist", journalId), ErrorCodes.EC1086);
    }
}

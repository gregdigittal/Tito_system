package cash.ice.api.errors;

import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;

public class DocumentNotFoundException extends ICEcashException {

    public DocumentNotFoundException(Integer documentId) {
        super(String.format("Document with id: %s does not exist", documentId), ErrorCodes.EC1014);
    }

    public DocumentNotFoundException(String documentId) {
        super(String.format("Document with id: %s does not exist", documentId), ErrorCodes.EC1014);
    }

    public DocumentNotFoundException(Integer entityId, Integer documentTypeId) {
        super(String.format("Document (type: %s) for entityId: %s does not exist", documentTypeId, entityId), ErrorCodes.EC1014);
    }
}

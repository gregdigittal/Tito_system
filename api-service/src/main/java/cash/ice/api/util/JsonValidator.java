package cash.ice.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import cash.ice.common.error.ICEcashException;

import static cash.ice.common.error.ErrorCodes.EC1001;

/**
 * Validates that a string is valid JSON. Throws EC1001 if not.
 */
public final class JsonValidator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonValidator() {}

    /**
     * Validate that the given string is parseable JSON (object or array).
     * @param json the string to validate
     * @param fieldName the field name for error messages
     * @throws ICEcashException with EC1001 if json is not valid
     */
    public static void requireValidJson(String json, String fieldName) {
        if (json == null || json.isBlank()) {
            return; // null/blank handled by caller; empty means no rule
        }
        try {
            OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            throw new ICEcashException(
                    String.format("'%s' must be valid JSON: %s", fieldName, e.getMessage()), EC1001);
        }
    }
}

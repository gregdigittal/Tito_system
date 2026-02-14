package cash.ice.api.util;

import cash.ice.common.error.ICEcashException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonValidatorTest {

    @Test
    void validJsonObject_noException() {
        assertDoesNotThrow(() -> JsonValidator.requireValidJson("{\"share\": 50}", "shareJson"));
    }

    @Test
    void validJsonArray_noException() {
        assertDoesNotThrow(() -> JsonValidator.requireValidJson("[{\"a\":1}]", "shareJson"));
    }

    @Test
    void nullOrBlank_noException() {
        assertDoesNotThrow(() -> JsonValidator.requireValidJson(null, "shareJson"));
        assertDoesNotThrow(() -> JsonValidator.requireValidJson("", "shareJson"));
        assertDoesNotThrow(() -> JsonValidator.requireValidJson("   ", "shareJson"));
    }

    @Test
    void invalidJson_throwsException() {
        ICEcashException ex = assertThrows(ICEcashException.class,
                () -> JsonValidator.requireValidJson("{invalid", "shareJson"));
        assertTrue(ex.getMessage().contains("shareJson"));
        assertTrue(ex.getMessage().contains("must be valid JSON"));
    }

    @Test
    void plainString_throwsException() {
        assertThrows(ICEcashException.class,
                () -> JsonValidator.requireValidJson("not json at all", "shareJson"));
    }
}

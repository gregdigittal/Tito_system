package cash.ice.api.graphql;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class NullStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext context) throws IOException, JsonProcessingException {
        String value = p.getText();
        if (value != null) {
            value = value.replace("\"null\"", "null");
        }
        return value;
    }
}
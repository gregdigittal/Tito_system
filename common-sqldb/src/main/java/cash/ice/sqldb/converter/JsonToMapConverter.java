package cash.ice.sqldb.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.io.Serializable;

@Converter
public class JsonToMapConverter implements AttributeConverter<Serializable, String> {
    private final static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Serializable meta) {
        try {
            return meta == null ? null : objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Convert error while trying to convert string(JSON) to map data structure.");
        }
    }

    @Override
    public Serializable convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : objectMapper.readValue(dbData, Serializable.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not convert map to json string.");
        }
    }
}

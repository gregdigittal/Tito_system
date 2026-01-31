package cash.ice.api.converter;

import cash.ice.api.dto.PaymentDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.List;

@Converter
public class JsonToPaymentDocumentConverter implements AttributeConverter<List<PaymentDocument>, String> {
    private final static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<PaymentDocument> meta) {
        try {
            return meta == null ? null : objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Convert error while trying to convert string(JSON) to map data structure.");
        }
    }

    @Override
    public List<PaymentDocument> convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : objectMapper.readValue(dbData, new TypeReference<List<PaymentDocument>>() {
            });
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not convert map to json string.");
        }
    }
}

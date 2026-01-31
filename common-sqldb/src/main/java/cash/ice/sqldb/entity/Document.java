package cash.ice.sqldb.entity;

import cash.ice.sqldb.converter.JsonToMapConverter;
import cash.ice.common.utils.Tool;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "document")
@Data
@Accessors(chain = true)
public class Document implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "document_type_id")
    private Integer documentTypeId;

    @Column(name = "entity_id")
    private Integer entityId;

    @SuppressWarnings("JpaAttributeTypeInspection")
    @Column(name = "meta_data", columnDefinition = "json")
    @Convert(converter = JsonToMapConverter.class)
    private Map<String, Object> meta;

    @Column(name = "comments")
    private String comments;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @JsonIgnore
    public Map<String, Object> getMetaData() {
        return meta;
    }

    public void setMetaData(String jsonString) {
        this.meta = Tool.jsonStringToMap(jsonString);
    }

    private void ensureMetaData() {
        if (meta == null) {
            meta = new HashMap<>();
        }
    }

    public Document placeAddressId(Integer addressId) {
        ensureMetaData();
        meta.put("addressId", addressId);
        return this;
    }

    public Integer extractAddressId() {
        return meta != null ? (Integer) meta.get("addressId") : null;
    }

    public Document placeJournalId(Integer journalId) {
        ensureMetaData();
        meta.put("journalId", journalId);
        return this;
    }

    public Integer extractJournalId() {
        return meta != null ? (Integer) meta.get("journalId") : null;
    }
}

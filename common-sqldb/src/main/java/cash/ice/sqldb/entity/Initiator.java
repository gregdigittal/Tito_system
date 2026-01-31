package cash.ice.sqldb.entity;

import cash.ice.sqldb.converter.JsonToMapConverter;
import cash.ice.common.utils.Tool;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "initiator")
@Data
@Accessors(chain = true)
public class Initiator implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "initiator_type_id", nullable = false)
    private Integer initiatorTypeId;

    @Column(name = "identifier")
    private String identifier;

    @Column(name = "account_id")
    private Integer accountId;

    @Column(name = "initiator_category_id")
    private Integer initiatorCategoryId;

    @Column(name = "initiator_status_id")
    private Integer initiatorStatusId;

    @Column(name = "pvv")
    private String pvv;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @SuppressWarnings("JpaAttributeTypeInspection")
    @Column(name = "meta_data", columnDefinition = "json")
    @Convert(converter = JsonToMapConverter.class)
    private Map<String, Object> metaData;

    @SuppressWarnings("JpaAttributeTypeInspection")
    @Column(name = "configuration", columnDefinition = "json")
    @Convert(converter = JsonToMapConverter.class)
    private Map<String, Object> configuration;

    public void setMetaData(String jsonString) {
        this.metaData = Tool.jsonStringToMap(jsonString);
    }

    public Initiator setMetaDataMap(Map<String, Object> metaData) {
        this.metaData = metaData;
        return this;
    }

    public void setConfiguration(String jsonString) {
        this.configuration = Tool.jsonStringToMap(jsonString);
    }

    public Initiator setConfigurationMap(Map<String, Object> configuration) {
        this.configuration = configuration;
        return this;
    }
}

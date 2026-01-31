package cash.ice.sqldb.entity.moz;

import cash.ice.sqldb.converter.JsonToMapConverter;
import cash.ice.common.utils.Tool;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "device")
@Data
@Accessors(chain = true)
public class Device implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "serial", nullable = false, unique = true)
    private String serial;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeviceStatus status;

    @Column(name = "account_id")
    private Integer accountId;

    @Column(name = "vehicle_id")
    private Integer vehicleId;

    @SuppressWarnings("JpaAttributeTypeInspection")
    @Column(name = "meta_data", columnDefinition = "json")
    @Convert(converter = JsonToMapConverter.class)
    private Map<String, Object> meta;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "modified_date", nullable = false)
    private LocalDateTime modifiedDate;

    @JsonIgnore
    public Map<String, Object> getMetaData() {
        return meta;
    }

    public void setMetaData(String jsonString) {
        this.meta = Tool.jsonStringToMap(jsonString);
    }
}

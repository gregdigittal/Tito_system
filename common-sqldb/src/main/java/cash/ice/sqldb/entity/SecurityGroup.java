package cash.ice.sqldb.entity;

import cash.ice.sqldb.converter.JsonToMapConverter;
import cash.ice.common.utils.Tool;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static jakarta.persistence.CascadeType.*;

@Entity
@Table(name = "security_group")
@Data
@Accessors(chain = true)
public class SecurityGroup implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    @SuppressWarnings("JpaAttributeTypeInspection")
    @Column(name = "meta_data", columnDefinition = "json")
    @Convert(converter = JsonToMapConverter.class)
    private Map<String, Object> meta;

    @ManyToMany(cascade = {PERSIST, MERGE, REFRESH, DETACH}, fetch = FetchType.EAGER)
    @JoinTable(name = "security_group_right",
            joinColumns = {@JoinColumn(name = "security_group_id")},
            inverseJoinColumns = {@JoinColumn(name = "security_right_id")}
    )
    private Set<SecurityRight> rights = new HashSet<>();

    @Column(name = "legacy_id")
    private String legacyId;

    @JsonIgnore
    public Map<String, Object> getMetaData() {
        return meta;
    }

    public void setMetaData(String jsonString) {
        this.meta = Tool.jsonStringToMap(jsonString);
    }

    public void updateMetaDataValue(String key, Object val) {
        if (meta == null) {
            meta = new HashMap<>();
        }
        if (val != null) {
            meta.put(key, val);
        } else {
            meta.remove(key);
        }
    }
}

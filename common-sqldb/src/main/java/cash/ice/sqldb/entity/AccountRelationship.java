package cash.ice.sqldb.entity;

import cash.ice.common.utils.Tool;
import cash.ice.sqldb.converter.JsonToMapConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "account_relationship")
@Data
@Accessors(chain = true)
public class AccountRelationship implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "entity_id", nullable = false)
    private Integer entityId;

    @Column(name = "partner_account_id", nullable = false)
    private Integer partnerAccountId;

    @Column(name = "security_groups", columnDefinition = "json")
    @Convert(converter = JsonToMapConverter.class)
    private Map<String, Object> securityGroups;

    @Column(name = "meta_data", columnDefinition = "json")
    @Convert(converter = JsonToMapConverter.class)
    private Map<String, Object> metaData;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    public void setSecurityGroups(String jsonString) {
        this.securityGroups = Tool.jsonStringToMap(jsonString);
    }

    public AccountRelationship setSecurityGroupsMap(Map<String, Object> securityGroups) {
        this.securityGroups = securityGroups;
        return this;
    }

    public void setMetaData(String jsonString) {
        this.metaData = Tool.jsonStringToMap(jsonString);
    }

    public AccountRelationship setMetaDataMap(Map<String, Object> metaData) {
        this.metaData = metaData;
        return this;
    }

    public Integer getSecurityGroupFor(String realm) {
        return securityGroups != null && securityGroups.get(realm) instanceof Integer ? (Integer) securityGroups.get(realm) : null;
    }

    public void updateSecurityGroupFor(String realm, Integer value) {
        if (securityGroups == null) {
            securityGroups = new HashMap<>();
        }
        if (value != null) {
            securityGroups.put(realm, value);
        } else {
            securityGroups.remove(realm);
        }
    }
}

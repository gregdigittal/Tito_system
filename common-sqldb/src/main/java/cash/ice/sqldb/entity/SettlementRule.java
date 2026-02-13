package cash.ice.sqldb.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Phase 8-5: Settlement rule per entity (Sacco/owner). share_json defines split (e.g. Driver %, Conductor %, Platform %).
 */
@Entity
@Table(name = "settlement_rule")
@Data
@Accessors(chain = true)
public class SettlementRule implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "entity_id", nullable = false)
    private Integer entityId;

    @Column(name = "rule_name", length = 128)
    private String ruleName;

    @Column(name = "share_json", columnDefinition = "TEXT")
    private String shareJson;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}

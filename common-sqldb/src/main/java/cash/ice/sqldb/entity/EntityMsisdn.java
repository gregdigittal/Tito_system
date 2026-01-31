package cash.ice.sqldb.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "entity_msisdn")
@Data
@Accessors(chain = true)
public class EntityMsisdn implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "entity_id")
    private Integer entityId;

    @Column(name = "msisdn")
    private String msisdn;

    @Column(name = "description")
    private String description;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MsisdnType msisdnType;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}

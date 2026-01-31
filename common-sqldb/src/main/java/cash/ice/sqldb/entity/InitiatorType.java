package cash.ice.sqldb.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "initiator_type")
@Data
@Accessors(chain = true)
public class InitiatorType implements Serializable {
    public static final String JOURNAL = "journal";
    public static final String TAG = "tag";
    public static final String ACCOUNT_NUMBER = "accountNumber";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "description")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "entity_id")
    private Integer entityId;
}

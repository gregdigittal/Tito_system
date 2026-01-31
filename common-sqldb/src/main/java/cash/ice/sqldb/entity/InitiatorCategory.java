package cash.ice.sqldb.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "initiator_category")
@Data
@Accessors(chain = true)
public class InitiatorCategory implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "entity_id")
    private Integer entityId;
}

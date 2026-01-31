package cash.ice.sqldb.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "initiator_status")
@Data
@Accessors(chain = true)
public class InitiatorStatus implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "permit_transaction", nullable = false)
    private boolean permitTransaction;

    @Column(name = "active", nullable = false)
    private boolean active;
}

package cash.ice.sqldb.entity.zim;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tax_reason")
@Data
@Accessors(chain = true)
public class TaxReason implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "display", nullable = false)
    private boolean display;
}

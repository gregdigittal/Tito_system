package cash.ice.sqldb.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "meta_data")
@Data
@Accessors(chain = true)
public class MetaData implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "table_name", nullable = false)
    private String table;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;
}

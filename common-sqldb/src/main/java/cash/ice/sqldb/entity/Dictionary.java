package cash.ice.sqldb.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "dictionary")
@Data
@Accessors(chain = true)
public class Dictionary implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "lookup_key", nullable = false)
    private String lookupKey;

    @Column(name = "environment", nullable = false)
    private String environment;

    @Column(name = "language_id", nullable = false)
    private Integer languageId;

    @Column(name = "section", nullable = false)
    private String section;

    @Column(name = "value", nullable = false)
    private String value;
}

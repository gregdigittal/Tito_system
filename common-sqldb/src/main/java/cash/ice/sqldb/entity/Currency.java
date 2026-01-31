package cash.ice.sqldb.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "currency")
@Data
@Accessors(chain = true)
public class Currency implements Serializable {
    public static final String MZN = "MZN";
    public static final String KES = "KES";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "ISO_Code", nullable = false)
    private String isoCode;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "postilion_code")
    private Integer postilionCode;
}

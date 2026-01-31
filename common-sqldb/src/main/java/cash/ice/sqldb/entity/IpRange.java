package cash.ice.sqldb.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "ip_whitelist")
@Data
@Accessors(chain = true)
public class IpRange implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "ip_range", nullable = false)
    private String address;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "description")
    private String description;
}

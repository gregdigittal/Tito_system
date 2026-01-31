package cash.ice.sqldb.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "entity_type")
@Data
@Accessors(chain = true)
public class EntityType implements Serializable {
    public static final String PRIVATE = "Private";
    public static final String BUSINESS = "Business";
    public static final String FNDS_FARMER = "FNDS Farmer";
    public static final String FNDS_AGRI_DEALER = "FNDS Agri Dealer";
    public static final String FNDS_AGENT = "FNDS Agent";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "description")
    private String description;

    @Column(name = "entity_type_group_id")
    private Integer entityTypeGroupId;

}

package cash.ice.api.entity.ken;

import cash.ice.api.entity.moz.ProductRelationshipType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "entity_products")
@Data
@Accessors(chain = true)
public class EntityProduct implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "entity_id", nullable = false)
    private Integer entityId;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "relationship_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductRelationshipType relationshipType;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
}

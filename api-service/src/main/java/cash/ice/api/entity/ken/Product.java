package cash.ice.api.entity.ken;

import cash.ice.api.entity.moz.ProductType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "product")
@Data
@Accessors(chain = true)
public class Product implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductType productType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "currency_id", nullable = false)
    private Integer currencyId;

    @Column(name = "price", nullable = false)
    private BigDecimal price;
}

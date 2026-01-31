package cash.ice.api.entity.moz;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "route_details")
@Data
@Accessors(chain = true)
public class RouteDetails implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "route_id", nullable = false)
    private Integer routeId;

    @Column(name = "operator_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private OperatorType operatorType;

    @Column(name = "min_distance", nullable = false)
    private Double minDistance;

    @Column(name = "min_fare", nullable = false)
    private BigDecimal minFare;

    @Column(name = "fare_per_km", nullable = false)
    private BigDecimal farePerKm;

    @Column(name = "max_fare", nullable = false)
    private BigDecimal maxFare;

    @Column(name = "max_distance")
    private Double maxDistance;
}

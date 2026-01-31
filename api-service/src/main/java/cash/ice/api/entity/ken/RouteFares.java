package cash.ice.api.entity.ken;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "route_fares")
@Data
@Accessors(chain = true)
public class RouteFares implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "route_id", nullable = false)
    private Integer routeId;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "peak", nullable = false)
    private boolean peak;

    @Column(name = "price", nullable = false)
    private BigDecimal price;
}

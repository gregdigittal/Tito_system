package cash.ice.sqldb.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Phase 8-4: User trip (tap-in/tap-out) for commuter history.
 */
@Entity
@Table(name = "trip")
@Data
@Accessors(chain = true)
public class Trip implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "entity_id", nullable = false)
    private Integer entityId;

    @Column(name = "tap_in_at")
    private LocalDateTime tapInAt;

    @Column(name = "tap_out_at")
    private LocalDateTime tapOutAt;

    @Column(name = "route_id")
    private Integer routeId;

    @Column(name = "route_name")
    private String routeName;

    @Column(name = "fare", precision = 19, scale = 2)
    private BigDecimal fare;

    @Column(name = "currency_id")
    private Integer currencyId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TripStatus status;

    @Column(name = "device_id")
    private Integer deviceId;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}

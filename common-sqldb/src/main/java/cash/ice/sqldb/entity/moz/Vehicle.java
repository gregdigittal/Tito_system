package cash.ice.sqldb.entity.moz;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "vehicle")
@Data
@Accessors(chain = true)
public class Vehicle implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "vrn", nullable = false, unique = true)
    private String vrn;

    @Column(name = "make", nullable = false)
    private String make;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "vehicle_type")
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    @Column(name = "route_id")
    private Integer routeId;

    @Column(name = "driver_entity_id")
    private Integer driverEntityId;

    @Column(name = "collector_entity_id")
    private Integer collectorEntityId;

    @Column(name = "entity_id")
    private Integer entityId;

    @Column(name = "account_id")
    private Integer accountId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private VehicleStatus status;

    public void fillDetails(Vehicle details) {
        vrn = details.vrn;
        make = details.make;
        model = details.model;
        vehicleType = details.vehicleType;
        routeId = details.routeId;
        driverEntityId = details.driverEntityId;
        collectorEntityId = details.collectorEntityId;
        status = details.status;
    }
}

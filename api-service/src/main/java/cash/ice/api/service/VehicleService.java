package cash.ice.api.service;

import cash.ice.api.dto.SortInput;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.moz.Vehicle;
import org.springframework.data.domain.Page;

public interface VehicleService {

    Vehicle createVehicle(Vehicle vehicle, EntityClass entity);

    Vehicle updateVehicle(Integer vehicleId, Vehicle vehicleDetails);

    Vehicle deleteVehicle(Integer vehicleId);

    Page<Vehicle> getVehicles(EntityClass entity, int page, int size, SortInput sort);
}

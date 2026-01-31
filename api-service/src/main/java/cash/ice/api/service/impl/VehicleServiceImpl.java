package cash.ice.api.service.impl;

import cash.ice.api.dto.SortInput;
import cash.ice.api.repository.moz.RouteRepository;
import cash.ice.api.service.EntityMozService;
import cash.ice.api.service.VehicleService;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.AccountType;
import cash.ice.sqldb.entity.Currency;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.moz.Vehicle;
import cash.ice.sqldb.entity.moz.VehicleType;
import cash.ice.sqldb.repository.EntityRepository;
import cash.ice.sqldb.repository.moz.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

import static cash.ice.common.error.ErrorCodes.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleServiceImpl implements VehicleService {
    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final EntityRepository entityRepository;
    private final EntityMozService entityMozService;

    @Override
    public Vehicle createVehicle(Vehicle vehicle, EntityClass entity) {
        validate(vehicle);
        return vehicleRepository.save(vehicle
                .setEntityId(entity.getId())
                .setAccountId(entityMozService.getAccount(entity, AccountType.PRIMARY_ACCOUNT, Currency.MZN).getId()));
    }

    @Override
    public Vehicle updateVehicle(Integer vehicleId, Vehicle vehicleDetails) {
        validate(vehicleDetails);
        Vehicle vehicle = getVehicle(vehicleId);
        Tool.logDifference(String.format("Vehicle with id=%s was changed. ", vehicle.getId()), vehicle, vehicleDetails, List.of("id", "entityId"));
        vehicle.fillDetails(vehicleDetails);
        return vehicleRepository.save(vehicle);
    }

    @Override
    public Vehicle deleteVehicle(Integer vehicleId) {
        Vehicle vehicle = getVehicle(vehicleId);
        vehicleRepository.deleteById(vehicle.getId());
        return vehicle;
    }

    @Override
    public Page<Vehicle> getVehicles(EntityClass entity, int page, int size, SortInput sort) {
        return vehicleRepository.findByEntityId(entity.getId(), PageRequest.of(page, size, SortInput.toSort(sort)));
    }

    private Vehicle getVehicle(int id) {
        return vehicleRepository.findById(id).orElseThrow(() ->
                new ICEcashException(String.format("Vehicle with ID: %s does not exist", id), EC1076));
    }

    private void validate(Vehicle vehicle) {
        if (vehicle.getRouteId() == null && vehicle.getVehicleType() == VehicleType.TAXI
                || vehicle.getRouteId() != null && !routeRepository.existsById(vehicle.getRouteId())) {
            throw new ICEcashException(EC1072, "Invalid route", true);
        } else if (vehicle.getDriverEntityId() == null && vehicle.getVehicleType() == VehicleType.TAXI
                || vehicle.getDriverEntityId() != null && !entityRepository.existsById(vehicle.getDriverEntityId())) {
            throw new ICEcashException(EC1073, "Invalid driver entity", true);
        } else if (vehicle.getCollectorEntityId() == null && vehicle.getVehicleType() == VehicleType.TAXI
                || vehicle.getCollectorEntityId() != null && !entityRepository.existsById(vehicle.getCollectorEntityId())) {
            throw new ICEcashException(EC1073, "Invalid collector entity", true);
        }
    }
}

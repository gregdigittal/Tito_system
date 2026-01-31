package cash.ice.api.service.impl;

import cash.ice.api.dto.SortInput;
import cash.ice.api.repository.moz.RouteRepository;
import cash.ice.api.service.EntityMozService;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.AccountType;
import cash.ice.sqldb.entity.Currency;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.moz.Vehicle;
import cash.ice.sqldb.entity.moz.VehicleStatus;
import cash.ice.sqldb.entity.moz.VehicleType;
import cash.ice.sqldb.repository.EntityRepository;
import cash.ice.sqldb.repository.moz.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceImplTest {
    private static final int ENTITY_ID = 1;
    private static final int ROUTE_ID = 2;
    private static final int DRIVER_ENTITY_ID = 3;
    private static final int COLLECTOR_ENTITY_ID = 4;
    private static final int ACCOUNT_ID = 5;
    private static final int VEHICLE_ID = 6;
    private static final String VRN = "vrn";
    private static final String MODEL = "model";
    private static final String MAKE = "make";

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private RouteRepository routeRepository;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private EntityMozService entityMozService;
    @InjectMocks
    private VehicleServiceImpl service;

    @Test
    void testCreateVehicle() {
        Vehicle request = new Vehicle().setVehicleType(VehicleType.TAXI).setRouteId(ROUTE_ID).setDriverEntityId(DRIVER_ENTITY_ID).setCollectorEntityId(COLLECTOR_ENTITY_ID).setAccountId(ACCOUNT_ID)
                .setVrn(VRN).setModel(MODEL).setMake(MAKE).setStatus(VehicleStatus.ACTIVE);
        EntityClass authEntity = new EntityClass().setId(ENTITY_ID);

        when(routeRepository.existsById(ROUTE_ID)).thenReturn(true);
        when(entityRepository.existsById(DRIVER_ENTITY_ID)).thenReturn(true);
        when(entityRepository.existsById(COLLECTOR_ENTITY_ID)).thenReturn(true);
        when(entityMozService.getAccount(authEntity, AccountType.PRIMARY_ACCOUNT, Currency.MZN)).thenReturn(new Account().setId(ACCOUNT_ID));
        when(vehicleRepository.save(request)).thenAnswer(invocation -> invocation.getArguments()[0]);

        Vehicle actualResponse = service.createVehicle(request, authEntity);
        assertThat(actualResponse.getEntityId()).isEqualTo(ENTITY_ID);
    }

    @Test
    void testUpdateVehicle() {
        Vehicle request = new Vehicle().setVehicleType(VehicleType.TAXI).setRouteId(ROUTE_ID).setDriverEntityId(DRIVER_ENTITY_ID).setCollectorEntityId(COLLECTOR_ENTITY_ID)
                .setVrn(VRN).setModel(MODEL).setMake(MAKE).setStatus(VehicleStatus.ACTIVE);

        when(routeRepository.existsById(ROUTE_ID)).thenReturn(true);
        when(entityRepository.existsById(DRIVER_ENTITY_ID)).thenReturn(true);
        when(entityRepository.existsById(COLLECTOR_ENTITY_ID)).thenReturn(true);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(new Vehicle().setId(VEHICLE_ID).setEntityId(ENTITY_ID)));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

        Vehicle actualResponse = service.updateVehicle(VEHICLE_ID, request);
        assertThat(actualResponse.getId()).isEqualTo(VEHICLE_ID);
        assertThat(actualResponse.getVehicleType()).isEqualTo(VehicleType.TAXI);
        assertThat(actualResponse.getRouteId()).isEqualTo(ROUTE_ID);
        assertThat(actualResponse.getDriverEntityId()).isEqualTo(DRIVER_ENTITY_ID);
        assertThat(actualResponse.getCollectorEntityId()).isEqualTo(COLLECTOR_ENTITY_ID);
        assertThat(actualResponse.getEntityId()).isEqualTo(ENTITY_ID);
        assertThat(actualResponse.getVrn()).isEqualTo(VRN);
        assertThat(actualResponse.getModel()).isEqualTo(MODEL);
        assertThat(actualResponse.getMake()).isEqualTo(MAKE);
        assertThat(actualResponse.getStatus()).isEqualTo(VehicleStatus.ACTIVE);
    }

    @Test
    void testDeleteVehicle() {
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(new Vehicle().setId(VEHICLE_ID).setEntityId(ENTITY_ID)));
        service.deleteVehicle(VEHICLE_ID);
        verify(vehicleRepository).deleteById(VEHICLE_ID);
    }

    @Test
    void testGetVehicles() {
        int page = 0, size = 30, vehicleId2 = 111;
        when(vehicleRepository.findByEntityId(ENTITY_ID, PageRequest.of(page, size, SortInput.toSort(null))))
                .thenReturn(new PageImpl<>(List.of(new Vehicle().setId(VEHICLE_ID), new Vehicle().setId(vehicleId2))));
        Page<Vehicle> actualVehicles = service.getVehicles(new EntityClass().setId(ENTITY_ID), page, size, null);
        assertThat(actualVehicles.getContent().size()).isEqualTo(2);
        assertThat(actualVehicles.getContent().get(0).getId()).isEqualTo(VEHICLE_ID);
        assertThat(actualVehicles.getContent().get(1).getId()).isEqualTo(vehicleId2);
    }
}
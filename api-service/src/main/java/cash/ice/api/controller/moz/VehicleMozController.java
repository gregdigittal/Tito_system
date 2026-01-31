package cash.ice.api.controller.moz;

import cash.ice.api.config.property.MozProperties;
import cash.ice.api.dto.SortInput;
import cash.ice.api.dto.moz.AccountTypeMoz;
import cash.ice.api.entity.moz.Route;
import cash.ice.api.repository.moz.RouteRepository;
import cash.ice.api.service.AuthUserService;
import cash.ice.api.service.EntityService;
import cash.ice.api.service.PermissionsGroupService;
import cash.ice.api.service.VehicleService;
import cash.ice.common.constant.EntityMetaKey;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.moz.Vehicle;
import cash.ice.sqldb.repository.AccountRepository;
import cash.ice.sqldb.repository.EntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

import static cash.ice.api.util.MappingUtil.itemsToCategoriesMap;

@Controller
@RequiredArgsConstructor
@Slf4j
public class VehicleMozController {
    private final VehicleService vehicleService;
    private final EntityService entityService;
    private final AuthUserService authUserService;
    private final PermissionsGroupService permissionsGroupService;
    private final RouteRepository routeRepository;
    private final EntityRepository entityRepository;
    private final AccountRepository accountRepository;
    private final MozProperties mozProperties;

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Vehicle createVehicle(@Argument Vehicle vehicle) {
        EntityClass entity = entityService.getEntity(authUserService.getAuthUser());
        log.debug("> create vehicle: {}, entityId: {}", vehicle, entity != null ? entity.getId() : null);
        if (mozProperties.isVehicleValidateOwner()) {
            permissionsGroupService.validateUserMozSecurityGroup(entity, AccountTypeMoz.TransportOwnerPrivate.getSecurityGroupId());
        }
        return vehicleService.createVehicle(vehicle, entity);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Vehicle updateVehicle(@Argument Integer id, @Argument Vehicle vehicleDetails) {
        EntityClass entity = entityService.getEntity(authUserService.getAuthUser());
        log.debug("> update vehicle: id: {}, entityId: {}, details: {}", id, entity != null ? entity.getId() : null, vehicleDetails);
        if (mozProperties.isVehicleValidateOwner()) {
            permissionsGroupService.validateUserMozSecurityGroup(entity, AccountTypeMoz.TransportOwnerPrivate.getSecurityGroupId());
        }
        return vehicleService.updateVehicle(id, vehicleDetails);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Vehicle deleteVehicle(@Argument Integer id) {
        EntityClass entity = entityService.getEntity(authUserService.getAuthUser());
        log.debug("> delete vehicle: {}, entityId: {}", id, entity != null ? entity.getId() : null);
        if (mozProperties.isVehicleValidateOwner()) {
            permissionsGroupService.validateUserMozSecurityGroup(entity, AccountTypeMoz.TransportOwnerPrivate.getSecurityGroupId());
        }
        return vehicleService.deleteVehicle(id);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Page<Vehicle> vehicles(@Argument int page, @Argument int size, @Argument SortInput sort) {
        EntityClass authEntity = entityService.getEntity(authUserService.getAuthUser());
        log.info("> GET vehicles (moz): entity: {} ({} {}), accountType: {}, page: {}, size: {}, sort: {}", authEntity.getId(),
                authEntity.getFirstName(), authEntity.getLastName(), getAccountType(authEntity), page, size, sort);
        return vehicleService.getVehicles(authEntity, page, size, sort);
    }

    private Object getAccountType(EntityClass entity) {
        return entity.getMeta() != null ? entity.getMeta().get(EntityMetaKey.AccountTypeMoz) : null;
    }

    @BatchMapping(typeName = "Vehicle", field = "route")
    public Map<Vehicle, Route> vehicleRoute(List<Vehicle> vehicles) {
        return itemsToCategoriesMap(vehicles, Vehicle::getRouteId, Route::getId, routeRepository);
    }

    @BatchMapping(typeName = "Vehicle", field = "driverEntity")
    public Map<Vehicle, EntityClass> vehicleDriver(List<Vehicle> vehicles) {
        return itemsToCategoriesMap(vehicles, Vehicle::getDriverEntityId, EntityClass::getId, entityRepository);
    }

    @BatchMapping(typeName = "Vehicle", field = "collectorEntity")
    public Map<Vehicle, EntityClass> vehicleCollector(List<Vehicle> vehicles) {
        return itemsToCategoriesMap(vehicles, Vehicle::getCollectorEntityId, EntityClass::getId, entityRepository);
    }

    @BatchMapping(typeName = "Vehicle", field = "account")
    public Map<Vehicle, Account> vehicleAccount(List<Vehicle> vehicles) {
        return itemsToCategoriesMap(vehicles, Vehicle::getAccountId, Account::getId, accountRepository);
    }
}

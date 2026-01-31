package cash.ice.api.service.impl;

import cash.ice.api.config.property.EntitiesProperties;
import cash.ice.api.dto.LoginEntityRequest;
import cash.ice.api.dto.LoginResponse;
import cash.ice.api.errors.UnexistingUserException;
import cash.ice.api.service.*;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.moz.Device;
import cash.ice.sqldb.entity.moz.Vehicle;
import cash.ice.sqldb.repository.*;
import cash.ice.sqldb.repository.moz.DeviceRepository;
import cash.ice.sqldb.repository.moz.VehicleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

import static cash.ice.common.error.ErrorCodes.EC1076;
import static cash.ice.common.error.ErrorCodes.EC3019;

@Service("EntityLoginServiceMoz")
@Slf4j
public class EntityLoginServiceMozImpl extends EntityLoginServiceImpl {
    private final DeviceRepository deviceRepository;
    private final VehicleRepository vehicleRepository;

    public EntityLoginServiceMozImpl(EntityRepository entityRepository, AccountRepository accountRepository, EntityService entityService, SecurityPvvService securityPvvService, KeycloakService keycloakService, MfaService mfaService, NotificationService notificationService, PermissionsService permissionsService, InitiatorRepository initiatorRepository, EntityMsisdnRepository entityMsisdnRepository, ChannelRepository channelRepository, DictionaryRepository dictionaryRepository, EntitiesProperties entitiesProperties, DeviceRepository deviceRepository, VehicleRepository vehicleRepository) {
        super(entityRepository, accountRepository, entityService, securityPvvService, keycloakService, mfaService, notificationService, permissionsService, initiatorRepository, entityMsisdnRepository, channelRepository, dictionaryRepository, entitiesProperties);
        this.deviceRepository = deviceRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    @Transactional
    public LoginResponse makePosDeviceLogin(String deviceSerial, LoginEntityRequest loginEntityRequest) {
        LoginResponse loginResponse = makeLogin(loginEntityRequest);
        Device device = deviceRepository.findBySerial(deviceSerial).orElseThrow(() ->
                new ICEcashException("Invalid deviceSerial", EC3019));
        if (device.getVehicleId() == null) {
            throw new ICEcashException("POS device is not linked to vehicle", ErrorCodes.EC1079);
        }
        Vehicle vehicle = vehicleRepository.findById(device.getVehicleId()).orElseThrow(() ->
                new ICEcashException(String.format("Vehicle with ID: %s does not exist", device.getVehicleId()), EC1076));
        int entityId = loginResponse.getEntity().getId();
        if (!Objects.equals(vehicle.getEntityId(), entityId) && !Objects.equals(vehicle.getDriverEntityId(), entityId) && !Objects.equals(vehicle.getCollectorEntityId(), entityId)) {
            throw new ICEcashException("User is not assigned to the vehicle as an owner, driver or collector", ErrorCodes.EC1080);
        }
        log.debug(String.format("User 'id=%s' is %s of vehicle 'id=%s'", entityId, Objects.equals(vehicle.getEntityId(), entityId) ?
                "Owner" : Objects.equals(vehicle.getDriverEntityId(), entityId) ? "Driver" : "Collector", vehicle.getId()));
        return loginResponse;
    }

    @Override
    public EntityClass findEntity(String enterId) {
        Integer entityId = tryParseEntityId(enterId);               // entityId
        if (entityId != null) {
            Optional<EntityClass> entity = entityRepository.findById(entityId);
            if (entity.isPresent()) {
                return entity.get();
            }
        }

        EntityClass entity = tryFindByAccountNumber(enterId);       // account number
        if (entity != null) {
            return entity;
        }

        // national id
        EntityClass entityClass = tryFindByIdNumber(enterId);
        if (entityClass != null) {
            return entityClass;
        } else {
            throw new UnexistingUserException(enterId);
        }
    }

    private Integer tryParseEntityId(String enterId) {
        try {
            return Integer.parseInt(enterId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

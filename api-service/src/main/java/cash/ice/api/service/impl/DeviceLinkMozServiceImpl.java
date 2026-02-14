package cash.ice.api.service.impl;

import cash.ice.api.config.property.MozProperties;
import cash.ice.api.dto.OtpType;
import cash.ice.api.dto.moz.AccountTypeMoz;
import cash.ice.api.dto.moz.LinkNfcTagRequest;
import cash.ice.api.dto.moz.TagInfoMoz;
import cash.ice.api.errors.Me60Exception;
import cash.ice.api.errors.UnexistingUserException;
import cash.ice.api.service.DeviceLinkMozService;
import cash.ice.api.service.EntityMozService;
import cash.ice.api.service.OtpService;
import cash.ice.api.service.PermissionsGroupService;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.entity.moz.Device;
import cash.ice.sqldb.entity.moz.DeviceStatus;
import cash.ice.sqldb.entity.moz.Vehicle;
import cash.ice.sqldb.repository.*;
import cash.ice.sqldb.repository.moz.DeviceRepository;
import cash.ice.sqldb.repository.moz.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

import static cash.ice.common.error.ErrorCodes.*;
import static cash.ice.sqldb.entity.AccountType.PREPAID_TRANSPORT;
import static cash.ice.sqldb.entity.InitiatorType.TAG;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceLinkMozServiceImpl implements DeviceLinkMozService {
    private static final String TRANSPORT_CATEGORY = "MZ Transport";
    private static final String ACTIVE = "Active";
    private static final String UNASSIGNED = "Unassigned";
    private static final String MT = "MT";

    private final DeviceRepository deviceRepository;
    private final EntityRepository entityRepository;
    private final VehicleRepository vehicleRepository;
    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final InitiatorTypeRepository initiatorTypeRepository;
    private final InitiatorCategoryRepository initiatorCategoryRepository;
    private final InitiatorStatusRepository initiatorStatusRepository;
    private final InitiatorRepository initiatorRepository;
    private final EntityMozService entityMozService;
    private final PermissionsGroupService permissionsGroupService;
    private final OtpService otpService;
    private final MozProperties mozProperties;

    @Override
    public Device linkPosDevice(String posDeviceSerial, Integer entityId, String otp) {
        if (mozProperties.isLinkPosCheckOtp()) {
            otpService.validateOtp(OtpType.MOZ_POS_LINK, entityId, otp);
        }
        EntityClass entity = entityRepository.findById(entityId).orElseThrow(() ->
                new UnexistingUserException("id: " + entityId));
        if (mozProperties.isLinkPosValidateOwner()) {
            permissionsGroupService.validateUserMozSecurityGroup(entity, AccountTypeMoz.TransportOwnerPrivate.getSecurityGroupId());
        }
        Device device = deviceRepository.findBySerial(posDeviceSerial).orElseThrow(() ->
                new ICEcashException("Invalid deviceSerial", EC3019));
        Account account = entityMozService.getAccount(entity, AccountType.PRIMARY_ACCOUNT, Currency.MZN);
        return deviceRepository.save(device.setAccountId(account.getId()));
    }

    @Override
    public Device linkPosDeviceToVehicle(String posDeviceSerial, int authEntityId, Integer vehicleId) {
        Device device = deviceRepository.findBySerial(posDeviceSerial).orElseThrow(() ->
                new ICEcashException("Invalid deviceSerial", EC3019));
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow(() ->
                new ICEcashException(String.format("Vehicle with ID: %s does not exist", vehicleId), EC1076));
        Account vehicleAccount = accountRepository.findById(vehicle.getAccountId()).stream().findFirst().orElseThrow(() ->
                new ICEcashException(String.format("Vehicle Account with ID: %s does not exist", vehicle.getAccountId()), EC1022));
        EntityClass vehicleEntity = entityRepository.findById(vehicleAccount.getEntityId())
                .orElseThrow(() -> new ICEcashException("Entity with id=" + vehicleAccount.getEntityId() + " does not exist", EC1048));
        if (!Objects.equals(vehicleEntity.getId(), authEntityId)) {
            throw new ICEcashException("Vehicle does not belong to the user", ErrorCodes.EC1077);
        }
        return deviceRepository.save(device.setVehicleId(vehicle.getId())
                .setStatus(DeviceStatus.ACTIVE));
    }

    @Override
    @Transactional(timeout = 30)
    public TagInfoMoz linkNfcTag(LinkNfcTagRequest nfcTag, String otp) {
        if (nfcTag.getDevice() != null && !nfcTag.getDevice().isBlank()) {
            validateNfcTagForLinking(nfcTag.getDevice());
        }
        Account account = accountRepository.findByAccountNumber(nfcTag.getAccountNumber()).stream().findFirst()
                .orElseThrow(() -> new ICEcashException("Invalid account: " + nfcTag.getAccountNumber(), EC1022));
        EntityClass entity = entityRepository.findById(account.getEntityId())
                .orElseThrow(() -> new ICEcashException("Entity with id=" + account.getEntityId() + " does not exist", EC1048));
        if (mozProperties.isLinkTagCheckOtp()) {
            otpService.validateOtp(OtpType.MOZ_TAG_LINK, entity.getId(), otp);
        }
        InitiatorType initiatorType = initiatorTypeRepository.findByDescription(TAG)
                .orElseThrow(() -> new ICEcashException("'tag' initiator type does not exist", EC1057, true));
        InitiatorStatus activeStatus = initiatorStatusRepository.findByName(ACTIVE)
                .orElseThrow(() -> new ICEcashException("'Active' initiator status does not exist", EC1059, true));
        InitiatorStatus unassignedStatus = initiatorStatusRepository.findByName(UNASSIGNED)
                .orElseThrow(() -> new ICEcashException("'Unassigned' initiator status does not exist", EC1059, true));
        InitiatorCategory initiatorCategory = initiatorCategoryRepository.findByCategory(TRANSPORT_CATEGORY)
                .orElseThrow(() -> new ICEcashException("'MZ Transport' initiator category does not exist", EC1058, true));
        AccountType accountType = accountTypeRepository.findById(account.getAccountTypeId()).orElse(null);
        if (accountType == null || !PREPAID_TRANSPORT.equals(accountType.getName())) {
            throw new ICEcashException(String.format("Wrong account type: %s, only 'Prepaid' account can be linked", accountType), EC1068);
        }
        Initiator tag = initiatorRepository.findByIdentifier(nfcTag.getTagNumber()).orElseThrow(() ->
                new ICEcashException(String.format("Tag '%s' does not exist", nfcTag.getTagNumber()), EC1066));
        if (Objects.equals(tag.getInitiatorStatusId(), activeStatus.getId())) {
            throw new ICEcashException("Tag already linked: " + nfcTag.getTagNumber(), EC1064);
        } else if (!Objects.equals(tag.getInitiatorStatusId(), unassignedStatus.getId())) {
            throw new ICEcashException(String.format("Tag '%s' is not in 'Unassigned' status", nfcTag.getTagNumber()), EC1067);
        }
        initiatorRepository.save(tag
                .setIdentifier(nfcTag.getTagNumber())
                .setInitiatorTypeId(initiatorType.getId())
                .setAccountId(account.getId())
                .setInitiatorCategoryId(initiatorCategory.getId())
                .setInitiatorStatusId(activeStatus.getId())
                .setCreatedDate(nfcTag.getDateTime() != null ? nfcTag.getDateTime() : Tool.currentDateTime())
                .setStartDate(nfcTag.getDateTime() != null ? nfcTag.getDateTime().toLocalDate() : LocalDate.now()));
        return entityMozService.getTagInfo(nfcTag.getTagNumber());
    }

    @Override
    @Transactional(timeout = 30)
    public TagInfoMoz delinkNfcTag(String identifier, Integer authEntityId) {
        Initiator tag = initiatorRepository.findByIdentifier(identifier).orElseThrow(() ->
                new ICEcashException(String.format("Tag '%s' does not exist", identifier), EC1066));
        InitiatorStatus activeStatus = initiatorStatusRepository.findByName(ACTIVE)
                .orElseThrow(() -> new ICEcashException("'Active' initiator status does not exist", EC1059, true));
        InitiatorStatus unassignedStatus = initiatorStatusRepository.findByName(UNASSIGNED)
                .orElseThrow(() -> new ICEcashException("'Unassigned' initiator status does not exist", EC1059, true));
        if (!Objects.equals(tag.getInitiatorStatusId(), activeStatus.getId())) {
            throw new ICEcashException("Tag is not linked: " + identifier, EC1067);
        }
        Account account = accountRepository.findById(tag.getAccountId()).stream().findFirst()
                .orElseThrow(() -> new ICEcashException("Tag account not found", EC1022));
        if (!Objects.equals(account.getEntityId(), authEntityId)) {
            throw new ICEcashException("Tag does not belong to this user", EC1077);
        }
        initiatorRepository.save(tag
                .setAccountId(null)
                .setInitiatorStatusId(unassignedStatus.getId()));
        return entityMozService.getTagInfo(identifier);
    }

    private void validateNfcTagForLinking(String deviceSerialOrCode) {
        if (mozProperties.isLinkTagValidateDevice()) {
            Device device = deviceRepository.findBySerial(deviceSerialOrCode).orElseGet(
                    () -> deviceRepository.findByCode(deviceSerialOrCode).orElse(null));
            if (device == null) {
                throw new Me60Exception("POS Device is not registered", deviceSerialOrCode, EC1055);
            } else if (device.getStatus() != DeviceStatus.ACTIVE) {
                throw new Me60Exception("POS Device is inactive", deviceSerialOrCode, EC1055);
            }
        }
    }
}

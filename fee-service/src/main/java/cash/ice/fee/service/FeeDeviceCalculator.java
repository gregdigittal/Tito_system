package cash.ice.fee.service;


import cash.ice.common.constant.PaymentMetaKey;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.fee.FeeEntry;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.error.ErrorCodes;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.entity.moz.Device;
import cash.ice.sqldb.entity.moz.DeviceStatus;
import cash.ice.sqldb.entity.moz.Vehicle;
import cash.ice.sqldb.entity.moz.VehicleStatus;
import cash.ice.sqldb.repository.*;
import cash.ice.sqldb.repository.moz.DeviceRepository;
import cash.ice.sqldb.repository.moz.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cash.ice.common.error.ErrorCodes.*;
import static cash.ice.sqldb.entity.AccountType.FNDS_ACCOUNT;
import static cash.ice.sqldb.entity.AccountType.PREPAID_TRANSPORT;
import static cash.ice.sqldb.entity.InitiatorType.ACCOUNT_NUMBER;
import static cash.ice.sqldb.entity.InitiatorType.TAG;
import static cash.ice.sqldb.entity.TransactionCode.KIT;
import static cash.ice.sqldb.entity.TransactionCode.TSF;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeeDeviceCalculator {
    private final DeviceRepository deviceRepository;
    private final VehicleRepository vehicleRepository;
    private final InitiatorRepository initiatorRepository;
    private final InitiatorStatusRepository initiatorStatusRepository;
    private final EntityRepository entityRepository;
    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final EntityMsisdnRepository entityMsisdnRepository;

    public void preHandleFees(List<FeeEntry> feeEntries, FeesData feesData, Map<Integer, Fee> feeMap) {              // TSF, KIT
        if (feesData.getMetaData() == null) {
            feesData.setMetaData(new HashMap<>());
        }
        PaymentRequest request = feesData.getPaymentRequest();
        Device device = findDevice(request.getMetaData());
        Account deviceAccount = findDeviceAccount(feesData, device);
        Account initiatorAccount = findInitiatorAccount(request.getInitiator(), feesData.getInitiatorTypeId(), feesData.getInitiatorTypeDescription(), feesData);
        EntityClass initiatorEntity = entityRepository.findById(initiatorAccount.getEntityId())
                .orElseThrow(() -> new ICEcashException("Entity with id=" + initiatorAccount.getEntityId() + " does not exist", EC3031));
        AccountType initiatorAccountType = accountTypeRepository.findById(initiatorAccount.getAccountTypeId()).orElse(null);
        checkInitiatorAccountType(initiatorAccountType, feesData.getPaymentRequest().getTx());

        feesData.setInitiatorEntityId(initiatorEntity.getId())
                .setInitiatorLocale(initiatorEntity.getLocale().toString())
                .setPrimaryMsisdn(entityMsisdnRepository.findByEntityIdAndPrimaryMsisdn(initiatorEntity.getId())
                        .map(EntityMsisdn::getMsisdn).orElseThrow(() ->
                                new ICEcashException(String.format("Invalid initiator msisdn (entityId=%s)", initiatorEntity.getId()), ErrorCodes.EC3028)))
                .getMetaData().put("posDeviceId", device.getId());
        FeeEntry originalFeeEntry = feeEntries.getFirst();
        Fee originalFee = feeMap.get(originalFeeEntry.getFeeId());
        originalFee.setDrEntityAccount(initiatorAccount)
                .setCrEntityAccount(deviceAccount);
        log.debug("  InitiatorEntityId: {}, initiatorLocale: {}, drAccount: {}, crAccount: {}",
                feesData.getInitiatorEntityId(), feesData.getInitiatorLocale(), initiatorAccount.getId(), deviceAccount.getId());
        handleAdditionalFees(feeEntries, feesData, feeMap);
    }

    private void checkInitiatorAccountType(AccountType initiatorAccountType, String transactionCode) {
        if (initiatorAccountType == null) {
            throw new ICEcashException("Initiator account type is absent", EC1068);
        } else if (TSF.equals(transactionCode) && !PREPAID_TRANSPORT.equals(initiatorAccountType.getName())) {
            throw new ICEcashException(String.format("Wrong account type, only '%s' account can be charged", PREPAID_TRANSPORT), EC1068);
        } else if (KIT.equals(transactionCode) && !FNDS_ACCOUNT.equals(initiatorAccountType.getName())) {
            throw new ICEcashException(String.format("Wrong account type, only '%s' account can be charged", FNDS_ACCOUNT), EC1068);
        }
    }

    private void handleAdditionalFees(List<FeeEntry> feeEntries, FeesData feesData, Map<Integer, Fee> feeMap) {
        if (TSF.equals(feesData.getPaymentRequest().getTx())) {
            AccountType subsidyAccountType = accountTypeRepository.findByNameAndCurrencyId(AccountType.SUBSIDY_ACCOUNT, feesData.getCurrencyId())
                    .orElseThrow(() -> new ICEcashException("Subsidy account type is absent", ErrorCodes.EC3025, true));
            Account subsidyAccount = accountRepository.findByEntityIdAndAccountTypeId(feesData.getInitiatorEntityId(), subsidyAccountType.getId())
                    .orElseThrow(() -> new ICEcashException("Subsidy account is absent", ErrorCodes.EC3026, true));

            BigDecimal subsidyAccountBalance = accountBalanceRepository.findByAccountId(subsidyAccount.getId())
                    .map(AccountBalance::getBalance).orElse(BigDecimal.ZERO);

            feesData.setSubsidyAccountBalance(subsidyAccountBalance);
            if (feeEntries.size() > 1) {
                if (subsidyAccountBalance.compareTo(BigDecimal.ZERO) > 0) {                        // subsidy balance not empty
                    FeeEntry originalFeeEntry = feeEntries.getFirst();
                    FeeEntry secondFeeEntry = feeEntries.get(1);
                    Fee originalFee = feeMap.get(originalFeeEntry.getFeeId());
                    Fee secondFee = feeMap.get(secondFeeEntry.getFeeId());
                    secondFee.setDrEntityAccount(subsidyAccount)
                            .setCrEntityAccount(originalFee.getCrEntityAccount());
                    if (subsidyAccountBalance.compareTo(secondFeeEntry.getAmount()) < 0) {                 // subsidy balance is not enough
                        secondFeeEntry.setAmount(subsidyAccountBalance);
                    }
                    originalFeeEntry.setAmount(originalFeeEntry.getAmount().subtract(secondFeeEntry.getAmount()));
                    feesData.setSubsidyAccountBalance(feesData.getSubsidyAccountBalance().subtract(secondFeeEntry.getAmount()));
                } else {
                    feeEntries.remove(1);
                }
            }
        }
    }

    private Device findDevice(Map<String, Object> meta) {
        String deviceCode = (String) meta.get(PaymentMetaKey.DeviceCode);
        Device device;
        if (deviceCode != null) {
            device = deviceRepository.findByCode(deviceCode).orElseThrow(() ->
                    new ICEcashException("Invalid deviceCode: " + deviceCode, ErrorCodes.EC3019));
        } else {
            String deviceSerial = (String) meta.get("deviceSerial");
            device = deviceSerial != null ? deviceRepository.findBySerial(deviceSerial).orElse(null) : null;
            if (device == null) {
                throw new ICEcashException("Invalid deviceSerial: " + deviceSerial, ErrorCodes.EC3019);
            }
        }
        if (device.getStatus() != DeviceStatus.ACTIVE) {
            throw new ICEcashException("Inactive device: " + device.getId(), ErrorCodes.EC3019);
        }
        return device;
    }

    private Account findDeviceAccount(FeesData feesData, Device device) {
        if (TSF.equals(feesData.getPaymentRequest().getTx()) && "2".equals(feesData.getPaymentRequest().getApiVersion())) {
            if (device.getVehicleId() == null) {
                throw new ICEcashException(String.format("POS device (id=%s) is not linked to vehicle", device.getId()), ErrorCodes.EC1079);
            }
            Vehicle vehicle = vehicleRepository.findById(device.getVehicleId()).orElseThrow(() ->
                    new ICEcashException(String.format("Vehicle with ID: %s does not exist.", device.getVehicleId()), EC1076));
            if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
                throw new ICEcashException("Vehicle is not active: " + vehicle.getId(), ErrorCodes.EC1083);
            } else if (vehicle.getAccountId() == null) {
                throw new ICEcashException("Account is not linked to vehicle: " + vehicle.getId(), ErrorCodes.EC3023);
            }
            feesData.getMetaData().putAll(new Tool.MapBuilder<String, Object>(new HashMap<>())
                    .putIfNonNull("vrnId", vehicle.getId())
                    .putIfNonNull("routeId", vehicle.getRouteId())
                    .build());
            return accountRepository.findById(vehicle.getAccountId()).orElseThrow(() ->
                    new ICEcashException("Invalid account linked to vehicle: " + vehicle.getAccountId(), ErrorCodes.EC3023));
        } else {
            if (device.getAccountId() == null) {
                throw new ICEcashException("Account is not linked to POS device: " + device.getId(), ErrorCodes.EC3023);
            }
            return accountRepository.findById(device.getAccountId()).orElseThrow(() ->
                    new ICEcashException("Invalid account linked to POS device: " + device.getAccountId(), ErrorCodes.EC3023));
        }
    }

    private Account findInitiatorAccount(String initiatorIdentifier, Integer initiatorTypeId, String initiatorTypeDescription, FeesData feesData) {
        switch (initiatorTypeDescription) {

            case TAG -> {
                InitiatorStatus activeInitiatorStatus = initiatorStatusRepository.findByName("Active").orElseThrow(() ->
                        new ICEcashException("Active initiator status is absent", ErrorCodes.EC3021, true));
                Initiator initiator = initiatorRepository.findByIdentifierAndInitiatorTypeId(initiatorIdentifier, initiatorTypeId).orElse(null);
                if (initiator == null) {
                    throw new ICEcashException("Invalid initiator tag: " + initiatorIdentifier, ErrorCodes.EC3022);
                } else if (!Objects.equals(initiator.getInitiatorStatusId(), activeInitiatorStatus.getId())) {
                    throw new ICEcashException("Inactive initiator tag: " + initiator.getId(), ErrorCodes.EC3022);
                }
                feesData.setInitiatorId(initiator.getId())
                        .getMetaData().put("tagId", initiator.getId());
                return accountRepository.findById(initiator.getAccountId()).orElseThrow(() ->
                        new ICEcashException("Invalid initiator account: " + initiator.getAccountId(), ErrorCodes.EC3024));
            }
            case ACCOUNT_NUMBER -> {
                List<Account> accounts = accountRepository.findByAccountNumber(initiatorIdentifier);
                if (accounts.isEmpty()) {
                    throw new ICEcashException(String.format("Incorrect initiator account number provided: %s", initiatorIdentifier), ErrorCodes.EC3034);
                }
                return accounts.getLast();
            }
            default -> throw new ICEcashException(String.format("Incorrect initiator type: %s (must be 'tag')",
                    initiatorTypeDescription), ErrorCodes.EC3020);
        }
    }
}

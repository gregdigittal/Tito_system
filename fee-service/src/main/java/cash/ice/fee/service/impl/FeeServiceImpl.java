package cash.ice.fee.service.impl;

import cash.ice.common.constant.PaymentMetaKey;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.fee.FeeEntry;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.fee.error.ICEcashInvalidRequestException;
import cash.ice.fee.error.ICEcashKycRequiredException;
import cash.ice.fee.service.FeeDeviceCalculator;
import cash.ice.fee.service.FeeService;
import cash.ice.fee.service.TransactionLimitCheckService;
import cash.ice.sqldb.entity.Currency;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.EntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cash.ice.common.error.ErrorCodes.*;
import static cash.ice.common.utils.Tool.correctValueWithin;
import static cash.ice.sqldb.entity.ChargeType.ORIGINAL;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeeServiceImpl implements FeeService {
    private final CacheableDataService dataService;
    private final FeeDeviceCalculator feeDeviceCalculator;
    private final EntityRepository entityRepository;
    private final TransactionLimitCheckService transactionLimitCheckService;

    @Transactional(timeout = 30)
    @Override
    public FeesData process(PaymentRequest paymentRequest) {
        if (paymentRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw ICEcashInvalidRequestException.with("amount", paymentRequest.getAmount().toString(), EC3001).get();
        }

        Currency currency = dataService.getCurrency(paymentRequest.getCurrency());
        TransactionCode transactionCode = dataService.getTransactionCode(paymentRequest.getTx());
        if (!transactionCode.isActive()) {
            throw new ICEcashInvalidRequestException("TransactionCode " + paymentRequest.getTx() + " is inactive", EC3011, true);
        }

        InitiatorType initiatorType = dataService.getInitiator(paymentRequest.getInitiatorType());
        if (!initiatorType.isActive()) {
            throw new ICEcashInvalidRequestException("InitiatorType " + initiatorType.getDescription() + " is inactive", EC3012, true);
        }

        List<Fee> fees = dataService.getFees(transactionCode.getId(), currency.getId());
        if (fees.isEmpty()) {
            throw new ICEcashException(String.format("No fees available for transactionCode: %s, currency: %s",
                    transactionCode.getCode(), currency.getIsoCode()), EC3015, true);
        }
        log.debug("fees {}: {}", fees.stream().map(Fee::getId).toList(), fees);

        FeesData feesData = new FeesData();
        feesData.setPaymentRequest(paymentRequest);
        feesData.setVendorRef(paymentRequest.getVendorRef());
        feesData.setCurrencyId(currency.getId());
        feesData.setCurrencyCode(currency.getIsoCode());
        feesData.setTransactionCodeId(transactionCode.getId());
        feesData.setTransactionCode(transactionCode.getCode());
        feesData.setInitiatorTypeId(initiatorType.getId());
        feesData.setInitiatorTypeDescription(initiatorType.getDescription());
        feesData.setInitiatorTypeEntityId(initiatorType.getEntityId());
        Map<Integer, Fee> feeMap = fees.stream().collect(Collectors.toMap(Fee::getId, e -> e));
        feesData.setFeeEntries(calculateFeeEntries(fees, paymentRequest.getAmount()));
        preHandleFees(feesData, transactionCode.getChargeAccountType(), feeMap);
        Map<Integer, EntityClass> allEntities = gatherAllEntities(feeMap);
        feesData.setFeeEntries(removeFeesWithInactiveAccounts(feesData.getFeeEntries(), feeMap, allEntities));
        checkAccountTypesCurrencies(feesData, feeMap);

        Fee originalFee = getOriginalFee(feesData.getFeeEntries(), feeMap);
        if (originalFee == null) {
            throw new ICEcashException(String.format("no ORIGINAL fee for transactionCode: %s, currency: %s",
                    transactionCode.getCode(), currency.getIsoCode()), EC3016, true);
        }
        Account originalDrAccount = originalFee.getDrEntityAccount();
        if (transactionCode.isKycRequired() && allEntities.get(originalDrAccount.getEntityId()).getKycStatusId() == 0) {
            throw new ICEcashKycRequiredException(transactionCode.getCode(), originalDrAccount.getEntityId());
        }
        feesData.setOriginalDrAccountId(originalDrAccount.getId());
        fillEntryFeeData(feesData, allEntities, feeMap);
        transactionLimitCheckService.checkLimits(feesData, allEntities);
        return feesData;
    }

    private List<FeeEntry> calculateFeeEntries(List<Fee> fees, BigDecimal amount) {
        ArrayList<FeeEntry> feeEntries = new ArrayList<>();
        HashMap<Integer, BigDecimal> feeIdToAmountMap = new HashMap<>();
        for (Fee fee : fees) {
            if (fee.isActive()) {
                FeeEntry feeEntry = new FeeEntry();
                if (fee.getChargeType() == ORIGINAL || fee.getSrcAmountFeeId() == null) {
                    feeEntry.setSourceAmount(amount);
                } else {
                    BigDecimal srcAmount = feeIdToAmountMap.getOrDefault(fee.getSrcAmountFeeId(), new BigDecimal("0"));
                    feeEntry.setSourceAmount(srcAmount);
                }
                feeEntry.setAmount(getChargeAmount(fee, feeEntry.getSourceAmount()));
                feeEntry.setFeeId(fee.getId());
                feeIdToAmountMap.put(fee.getId(), feeEntry.getAmount());
                feeEntries.add(feeEntry);
            } else if (fee.getChargeType() == ORIGINAL) {
                throw new ICEcashInvalidRequestException("Original fee charge for " + fee.getTransactionCode().getCode() +
                        " transaction code is inactive or invalid", EC3013, true);
            } else {
                log.warn("Skipping fee with absent credit and debit accounts! " + fee);
            }
        }
        return feeEntries;
    }

    //calculate fee
    private BigDecimal getChargeAmount(Fee fee, BigDecimal amount) {
        switch (fee.getChargeType()) {
            case ORIGINAL -> {
                return amount;
            }
            case FIXED -> {
                return correctValueWithin(fee.getAmount(), fee.getMinCharge(), fee.getMaxCharge());
            }
            case PERCENT -> {
                BigDecimal chargeAmount = (amount.multiply(fee.getAmount(), new MathContext(4)));
                return correctValueWithin(chargeAmount, fee.getMinCharge(), fee.getMaxCharge());
            }
//            case LOOKUP:   // todo is include the lookup chargeAmount process
            default -> {
                return new BigDecimal("0");
            }
        }
    }

    private void preHandleFees(FeesData feesData, ChargeAccountType chargeAccountType, Map<Integer, Fee> feeMap) {
        if (InitiatorType.JOURNAL.equals(feesData.getPaymentRequest().getInitiatorType())) {
            handleJournalPayment(feesData.getFeeEntries(), feesData, feeMap);

        } else {
            switch (chargeAccountType) {

                case PARTNER -> {           // paygo (PGCBZ)
                    if (feesData.getPaymentRequest().getPartnerId() == null) {
                        throw new ICEcashInvalidRequestException("'partnerId' is not provided", EC3014);
                    }
                    log.debug("  replacing partner account: " + feesData.getPaymentRequest().getPartnerId());
                    replaceNullCrDrAccounts(feesData.getFeeEntries(), feeMap,
                            dataService.getAccount(Integer.parseInt(feesData.getPaymentRequest().getPartnerId())));
                }
                case INITIATOR -> {         // flexcube (TRN)
                    if (feesData.getPaymentRequest().getInitiator() == null) {
                        throw new ICEcashInvalidRequestException("'initiator' is not provided", EC3018);
                    }
                    log.debug("  replacing initiator account: " + feesData.getPaymentRequest().getInitiator());
                    replaceNullCrDrAccounts(feesData.getFeeEntries(), feeMap,
                            dataService.getAccount(Integer.parseInt(feesData.getPaymentRequest().getInitiator())));
                }
                case META_ACCOUNT -> {      // ecocash (EPAYG), onemoney (OPAYG), mpesa (MPI, MPO), emola (EMI, EMO)
                    if (feesData.getPaymentRequest().getMetaData() == null || feesData.getPaymentRequest().getMetaData().get(PaymentMetaKey.AccountNumber) == null) {
                        throw new ICEcashInvalidRequestException("'meta.account' is not provided", EC3030);
                    }
                    String replacingAccountNumber = (String) feesData.getPaymentRequest().getMetaData().get(PaymentMetaKey.AccountNumber);
                    log.debug("  replacing meta account: " + replacingAccountNumber);
                    replaceNullCrDrAccounts(feesData.getFeeEntries(), feeMap,
                            dataService.getAccount(replacingAccountNumber));
                }
                case POS_DEVICE ->
                        feeDeviceCalculator.preHandleFees(feesData.getFeeEntries(), feesData, feeMap);       // TSF, KIT
                case null, default -> {
                    boolean needReplacer = feesData.getFeeEntries().stream().map(fe -> feeMap.get(fe.getFeeId()))
                            .anyMatch(fee -> fee.getDrEntityAccount() == null || fee.getCrEntityAccount() == null);
                    if (needReplacer) {
                        if (feesData.getInitiatorTypeEntityId() == null) {
                            throw new ICEcashInvalidRequestException("InitiatorType " + feesData.getInitiatorTypeDescription() + " isn't supported", EC3003, true);
                        }
                        log.debug("  replacing account, entityId: {}, currencyId: {}", feesData.getInitiatorTypeEntityId(), feesData.getCurrencyId());
                        AccountType accountType = dataService.getAccountType(feesData.getCurrencyId(), AccountType.PRIMARY_ACCOUNT);
                        replaceNullCrDrAccounts(feesData.getFeeEntries(), feeMap,
                                dataService.getAccount(feesData.getInitiatorTypeEntityId(), accountType.getId()));
                    }
                }
            }
        }
    }

    private void replaceNullCrDrAccounts(List<FeeEntry> feeEntries, Map<Integer, Fee> feeMap, Account account) {
        feeEntries.stream().map(fe -> feeMap.get(fe.getFeeId())).forEach(fee -> {
            if (fee.getDrEntityAccount() == null) {
                fee.setDrEntityAccount(account);
            } else if (fee.getCrEntityAccount() == null) {
                fee.setCrEntityAccount(account);
            }
        });
    }

    private Map<Integer, EntityClass> gatherAllEntities(Map<Integer, Fee> feeMap) {
        List<Integer> allEntitiesIds = feeMap.values().stream()
                .flatMap(fee -> Stream.of(fee.getDrEntityAccount(), fee.getCrEntityAccount()))
                .filter(Objects::nonNull).map(Account::getEntityId).distinct().toList();
        return entityRepository.findAllById(allEntitiesIds).stream().collect(Collectors.toMap(EntityClass::getId, e -> e));
    }

    private void fillEntryFeeData(FeesData feesData, Map<Integer, EntityClass> allEntities, Map<Integer, Fee> feeMap) {
        feesData.getFeeEntries().forEach(feeEntry -> {
            Fee fee = feeMap.get(feeEntry.getFeeId());
            feeEntry.setTransactionCodeId(fee.getTransactionCode().getId());
            feeEntry.setTransactionCodeDescription(fee.getTransactionCode().getDescription());
            feeEntry.setAffordabilityCheck(fee.isAffordabilityCheck());
            feeEntry.setDrAccountBalanceMinimum(fee.getDrEntityAccount().getBalanceMinimum());
            feeEntry.setDrAccountOverdraftLimit(fee.getDrEntityAccount().getOverdraftLimit());
            feeEntry.setDrAccountId(fee.getDrEntityAccount().getId());
            feeEntry.setDrAccountTypeId(fee.getDrEntityAccount().getAccountTypeId());
            feeEntry.setDrEntityId(fee.getDrEntityAccount().getEntityId());
            feeEntry.setDrAuthorisationTypeString(fee.getDrEntityAccount().getAuthorisationTypeString());
            feeEntry.setCrAccountId(fee.getCrEntityAccount().getId());
            feeEntry.setCrAccountTypeId(fee.getCrEntityAccount().getAccountTypeId());
            feeEntry.setCrEntityId(fee.getCrEntityAccount().getEntityId());
            feeEntry.setCrAuthorisationTypeString(fee.getCrEntityAccount().getAuthorisationTypeString());

            EntityClass drEntity = allEntities.get(fee.getDrEntityAccount().getEntityId());
            EntityClass crEntity = allEntities.get(fee.getCrEntityAccount().getEntityId());
            feeEntry.setDrEntityFirstName(drEntity.getFirstName());
            feeEntry.setDrEntityLastName(drEntity.getLastName());
            feeEntry.setCrEntityFirstName(crEntity.getFirstName());
            feeEntry.setCrEntityLastName(crEntity.getLastName());
        });
    }

    private void checkAccountTypesCurrencies(FeesData feesData, Map<Integer, Fee> feeMap) {
        feesData.getFeeEntries().stream().map(feeEntry -> feeMap.get(feeEntry.getFeeId())).forEach(fee -> {
            Integer crAccountTypeId = fee.getCrEntityAccount().getAccountTypeId();
            Integer drAccountTypeId = fee.getDrEntityAccount().getAccountTypeId();
            if (!Objects.equals(crAccountTypeId, drAccountTypeId)) {
                AccountType crAccountType = dataService.getAccountType(crAccountTypeId);
                AccountType drAccountType = dataService.getAccountType(drAccountTypeId);
                if (!Objects.equals(crAccountType.getCurrencyId(), drAccountType.getCurrencyId())) {
                    throw new ICEcashException(String.format("Transaction for accounts with different currencies, feeId=%s, CR: (accountId=%s currencyId=%s), DR: (accountId=%s currencyId=%s)",
                            fee.getId(), fee.getCrEntityAccount().getId(), crAccountType.getCurrencyId(), fee.getDrEntityAccount().getId(), drAccountType.getCurrencyId()), EC3033, false);
                }
            }
        });
    }

    private void handleJournalPayment(List<FeeEntry> feeEntries, FeesData feesData, Map<Integer, Fee> feeMap) {
        Fee originalFee = getOriginalFee(feeEntries, feeMap);
        Map<String, Object> metaData = feesData.getPaymentRequest().getMetaData();
        originalFee.setDrEntityAccount(dataService.getAccount((Integer) metaData.get(PaymentMetaKey.JournalDrAccountId)));
        originalFee.setCrEntityAccount(dataService.getAccount((Integer) metaData.get(PaymentMetaKey.JournalCrAccountId)));
        List<Map<String, Object>> feesUpdates = (List<Map<String, Object>>) metaData.get(PaymentMetaKey.JournalFees);
        List<FeeEntry> removing = new ArrayList<>();
        for (FeeEntry entry : feeEntries) {
            Fee fee = feeMap.get(entry.getFeeId());
            if (fee.getChargeType() != ORIGINAL) {
                BigDecimal amount = null;
                if (feesUpdates != null) {
                    amount = feesUpdates.stream().filter(f -> Objects.equals(f.get("feeId"), entry.getFeeId()))
                            .findAny().map(m -> new BigDecimal(String.valueOf(m.get("amount")))).orElse(null);
                }
                if (amount == null || !Tool.isGreater(amount, BigDecimal.ZERO)) {
                    removing.add(entry);
                } else {
                    entry.setAmount(amount);
                }
            }
        }
        if (!removing.isEmpty()) {
            feeEntries.removeAll(removing);
        }
        log.debug("    final journal fees: " + feeEntries.stream().map(e -> String.format("%s -> %s", e.getFeeId(), e.getAmount())).collect(Collectors.joining(", ", "[", "]")));
    }

    private Fee getOriginalFee(List<FeeEntry> feesEntries, Map<Integer, Fee> feeMap) {
        return feesEntries.stream()
                .map(feeEntry -> feeMap.get(feeEntry.getFeeId()))
                .filter(Fee::isOriginal).findAny()
                .orElse(null);
    }

    private List<FeeEntry> removeFeesWithInactiveAccounts(List<FeeEntry> feeEntries, Map<Integer, Fee> feeMap, Map<Integer, EntityClass> allEntities) {
        return feeEntries.stream().filter(feeEntry -> {
            Fee fee = feeMap.get(feeEntry.getFeeId());
            return fee.getCrEntityAccount() != null
                    && fee.getCrEntityAccount().getAccountStatus() == AccountStatus.ACTIVE
                    && allEntities.get(fee.getCrEntityAccount().getEntityId()).getStatus() == EntityStatus.ACTIVE
                    && fee.getDrEntityAccount() != null
                    && fee.getDrEntityAccount().getAccountStatus() == AccountStatus.ACTIVE
                    && allEntities.get(fee.getDrEntityAccount().getEntityId()).getStatus() == EntityStatus.ACTIVE;
        }).toList();
    }
}

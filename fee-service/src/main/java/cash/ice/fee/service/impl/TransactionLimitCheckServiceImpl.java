package cash.ice.fee.service.impl;

import cash.ice.common.dto.fee.FeeEntry;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.utils.Tool;
import cash.ice.fee.config.property.LimitsProperties;
import cash.ice.fee.dto.TransactionLimitData;
import cash.ice.fee.error.TransactionLimitExceededException;
import cash.ice.fee.repository.TransactionLimitDataRepository;
import cash.ice.fee.service.TransactionLimitCheckService;
import cash.ice.fee.service.TransactionLimitOverrideService;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.PaymentDirection;
import cash.ice.sqldb.entity.TransactionLimit;
import cash.ice.sqldb.repository.TransactionLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionLimitCheckServiceImpl implements TransactionLimitCheckService {
    private final TransactionLimitOverrideService transactionLimitOverrideService;
    private final TransactionLimitRepository transactionLimitRepository;
    private final TransactionLimitDataRepository transactionLimitDataRepository;
    private final LimitsProperties limitsProperties;

    @Override
    public void checkLimits(FeesData feesData, Map<Integer, EntityClass> allEntities) {
        if (limitsProperties.isEnabled()) {
            Map<Integer, TransactionLimitData> updatedLimits = new HashMap<>();
            feesData.getFeeEntries().forEach(feeEntry -> {
                List<TransactionLimit> debitLimits = findLimits(PaymentDirection.Debit, feeEntry, true, feesData, allEntities);
                List<TransactionLimit> creditLimits = findLimits(PaymentDirection.Credit, feeEntry, false, feesData, allEntities);

                Stream.concat(debitLimits.stream(), creditLimits.stream()).forEach(limit -> {
                    TransactionLimitData limitData = updatedLimits.get(limit.getId());
                    if (limitData == null) {
                        limitData = transactionLimitDataRepository.findByTransactionLimitId(limit.getId())
                                .orElseGet(() -> new TransactionLimitData().setTransactionLimitId(limit.getId()));
                        updatedLimits.put(limitData.getTransactionLimitId(), limitData.setLastAmount(feeEntry.getAmount()));
                    }
                    updateLimitData(limitData, feeEntry.getAmount(), Tool.currentDateTime());
                    checkLimit(limit, limitData, feeEntry.getAmount());
                });
            });
            if (!updatedLimits.isEmpty()) {
                List<TransactionLimitData> savedLimits = transactionLimitDataRepository.saveAll(updatedLimits.values());
                Map<String, BigDecimal> transactionLimitsMap = savedLimits.stream().collect(Collectors.toMap(TransactionLimitData::getId, TransactionLimitData::getLastAmount));
                feesData.setLimitData(new HashMap<>(transactionLimitsMap));
            }
        }
    }

    @Override
    public void rollbackLimitDataIfNeed(FeesData feesData) {
        if (feesData != null && feesData.getLimitData() != null) {
            log.debug("  rollback limitData: " + feesData.getLimitData());
            Iterable<TransactionLimitData> limitDataList = transactionLimitDataRepository.findAllById(feesData.getLimitData().keySet());
            limitDataList.forEach(limitData -> {
                limitData.setDayTransactions(limitData.getDayTransactions() > 0 ? limitData.getDayTransactions() - 1 : 0);
                limitData.setWeekTransactions(limitData.getWeekTransactions() > 0 ? limitData.getWeekTransactions() - 1 : 0);
                limitData.setMonthTransactions(limitData.getMonthTransactions() > 0 ? limitData.getMonthTransactions() - 1 : 0);
                BigDecimal amount = feesData.getLimitData().get(limitData.getId());
                if (amount != null) {
                    limitData.setDayAmount(limitData.getDayAmount() != null ? limitData.getDayAmount().subtract(amount) : BigDecimal.ZERO);
                    limitData.setWeekAmount(limitData.getWeekAmount() != null ? limitData.getWeekAmount().subtract(amount) : BigDecimal.ZERO);
                    limitData.setMonthAmount(limitData.getMonthAmount() != null ? limitData.getMonthAmount().subtract(amount) : BigDecimal.ZERO);
                }
            });
            transactionLimitDataRepository.saveAll(limitDataList);
        }
    }

    private List<TransactionLimit> findLimits(PaymentDirection paymentDirection, FeeEntry feeEntry, boolean debit, FeesData feesData, Map<Integer, EntityClass> allEntities) {
        EntityClass entity = allEntities.get(debit ? feeEntry.getDrEntityId() : feeEntry.getCrEntityId());
        List<TransactionLimit> limits = transactionLimitRepository.findTransactionLimits(
                feesData.getCurrencyId(),
                paymentDirection.name(),
                feesData.getTransactionCodeId(),
                entity.getKycStatusId(),
                entity.getEntityTypeId(),
                debit ? feeEntry.getDrAccountTypeId() : feeEntry.getCrAccountTypeId(),
                feesData.getInitiatorTypeId(),
                entity.getMetaTierString(),
                debit ? feeEntry.getDrAuthorisationTypeString() : feeEntry.getCrAuthorisationTypeString());
        if (!limits.isEmpty()) {
            log.debug("  {} {}) found {} limits: {} for {}, accountId: {}, entityId: {}", feeEntry.getFeeId(), paymentDirection.name(), limits.size(),
                    limits.stream().map(TransactionLimit::getId).toList(), feesData.getPaymentRequest().getVendorRef(),
                    debit ? feeEntry.getDrAccountId() : feeEntry.getCrAccountId(), debit ? feeEntry.getDrEntityId() : feeEntry.getCrEntityId());
        }
        if (limits.size() > 1 && limitsProperties.isOverridesEnabled()) {
            limits = transactionLimitOverrideService.overrideLimits(limits);
            log.debug("  after overriding left {} limits: {} for {}", limits.size(), limits.stream().map(TransactionLimit::getId).toList(), feesData.getPaymentRequest().getVendorRef());
        }
        return limits;
    }

    private void checkLimit(TransactionLimit limit, TransactionLimitData limitData, BigDecimal amount) {
        if (limit.getTransactionMinLimit() != null && Tool.isLess(amount, limit.getTransactionMinLimit())) {
            log.info("  min limit check failed, amount: {}, limit: {}, limitId: {}", amount, limit.getTransactionMinLimit(), limit.getId());
            throw new TransactionLimitExceededException("Min", limit.getTransactionMinLimit(), amount);

        } else if (limit.getTransactionMaxLimit() != null && Tool.isGreater(amount, limit.getTransactionMaxLimit())) {
            log.info("  max limit check failed, amount: {}, limit: {}, limitId: {}", amount, limit.getTransactionMaxLimit(), limit.getId());
            throw new TransactionLimitExceededException("Max", limit.getTransactionMaxLimit(), amount);

        } else if (limit.getDailyLimit() != null && Tool.isGreater(limitData.getDayAmount(), limit.getDailyLimit())) {
            log.info("  daily limit exceeded, amount: {}, dayAmount: {}, limit: {}, limitId: {}", amount, limitData.getDayAmount(), limit.getDailyLimit(), limit.getId());
            throw new TransactionLimitExceededException("Daily");

        } else if (limit.getWeeklyLimit() != null && Tool.isGreater(limitData.getWeekAmount(), limit.getWeeklyLimit())) {
            log.info("  weekly limit exceeded, amount: {}, weekAmount: {}, limit: {}, limitId: {}", amount, limitData.getWeekAmount(), limit.getWeeklyLimit(), limit.getId());
            throw new TransactionLimitExceededException("Weekly");

        } else if (limit.getMonthlyLimit() != null && Tool.isGreater(limitData.getMonthAmount(), limit.getMonthlyLimit())) {
            log.info("  monthly limit exceeded, amount: {}, monthAmount: {}, limit: {}, limitId: {}", amount, limitData.getMonthAmount(), limit.getMonthlyLimit(), limit.getId());
            throw new TransactionLimitExceededException("Monthly");
        }
    }

    private void updateLimitData(TransactionLimitData limitData, BigDecimal amount, LocalDateTime dateTime) {
        // day
        limitData.setLastUpdate(Tool.currentDateTime());
        int currentDay = dateTime.getDayOfYear();
        if (limitData.getDay() != currentDay || limitData.getDayAmount() == null) {
            limitData.setDay(currentDay);
            limitData.setDayTransactions(1);
            limitData.setDayAmount(amount);
        } else {
            limitData.setDayTransactions(limitData.getDayTransactions() + 1);
            limitData.setDayAmount(limitData.getDayAmount().add(amount));
        }

        // week
        int currentWeek = dateTime.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
        if (limitData.getWeek() != currentWeek || limitData.getWeekAmount() == null) {
            limitData.setWeek(currentWeek);
            limitData.setWeekTransactions(1);
            limitData.setWeekAmount(amount);
        } else {
            limitData.setWeekTransactions(limitData.getWeekTransactions() + 1);
            limitData.setWeekAmount(limitData.getWeekAmount().add(amount));
        }

        // month
        int currentMonth = limitData.getLastUpdate().getMonthValue();
        if (limitData.getMonth() != currentMonth || limitData.getMonthAmount() == null) {
            limitData.setMonth(currentMonth);
            limitData.setMonthTransactions(1);
            limitData.setMonthAmount(amount);
        } else {
            limitData.setMonthTransactions(limitData.getMonthTransactions() + 1);
            limitData.setMonthAmount(limitData.getMonthAmount().add(amount));
        }
        log.debug("    updated limit data: " + limitData);
    }
}

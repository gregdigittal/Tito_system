package cash.ice.api.service.impl;

import cash.ice.api.dto.TransactionLimitView;
import cash.ice.api.service.TransactionLimitService;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

import static cash.ice.common.error.ErrorCodes.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionLimitServiceImpl implements TransactionLimitService {
    private final TransactionLimitRepository transactionLimitRepository;
    private final CurrencyRepository currencyRepository;
    private final TransactionCodeRepository transactionCodeRepository;
    private final EntityTypeRepository entityTypeRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final InitiatorTypeRepository initiatorTypeRepository;

    @Override
    public Page<TransactionLimit> get(TransactionLimitView filter, Pageable pageable) {
        Page<TransactionLimit> limits = transactionLimitRepository.findAll(getExample(getTransactionLimitKey(filter), true), pageable);
        log.debug("  found {} from {} limits, ids: {}", limits.getContent().size(), limits.getTotalElements(), limits.getContent().stream().map(TransactionLimit::getId).toList());
        return limits;
    }

    @Override
    public TransactionLimit addOrUpdate(TransactionLimitView transactionLimitView) {
        TransactionLimit limitKey = getTransactionLimitKey(transactionLimitView);
        List<TransactionLimit> existingLimits = transactionLimitRepository.findAll(getExample(limitKey, false));
        log.debug(existingLimits.isEmpty() ? "  creating new limit" : "  updating existing " + existingLimits.size() + " limit: " + existingLimits.stream().map(TransactionLimit::getId).toList());
        TransactionLimit updatingLimit = !existingLimits.isEmpty() ? existingLimits.get(0) : limitKey.setCreatedDate(Tool.currentDateTime());
        return transactionLimitRepository.save(updatingLimit
                .setActive(transactionLimitView.isActive())
                .setTransactionMinLimit(transactionLimitView.getTransactionMinLimit())
                .setTransactionMaxLimit(transactionLimitView.getTransactionMaxLimit())
                .setDailyLimit(transactionLimitView.getDailyLimit())
                .setWeeklyLimit(transactionLimitView.getWeeklyLimit())
                .setMonthlyLimit(transactionLimitView.getMonthlyLimit()));
    }

    @Override
    public TransactionLimit setActive(TransactionLimitView transactionLimitView, boolean active) {
        List<TransactionLimit> limits = transactionLimitRepository.findAll(getExample(getTransactionLimitKey(transactionLimitView), false));
        List<TransactionLimit> transactionLimits = transactionLimitRepository.saveAll(limits.stream().peek(transactionLimit -> transactionLimit.setActive(active)).toList());
        return transactionLimits.isEmpty() ? null : transactionLimits.get(0);
    }

    @Override
    public TransactionLimit delete(Integer id) {
        TransactionLimit transactionLimit = transactionLimitRepository.findById(id).orElseThrow(
                () -> new ICEcashException(String.format("Transaction limit '%s' does not exist!", id), EC1069));
        transactionLimitRepository.delete(transactionLimit);
        return transactionLimit;
    }

    private TransactionLimit getTransactionLimitKey(TransactionLimitView tlView) {
        TransactionLimit transactionLimit = new TransactionLimit()
                .setCurrencyId(currencyRepository.findByIsoCode(tlView.getCurrency()).map(Currency::getId).orElseThrow(
                        () -> new ICEcashException(tlView.getCurrency() + " currency does not exist!", EC1062)))
                .setKycStatusId(tlView.getKycStatusId())
                .setTier(tlView.getTier())
                .setAuthorisationType(tlView.getAuthorisationType())
                .setDirection(tlView.getDirection() == PaymentDirection.Credit ? PaymentDirection.Credit : PaymentDirection.Debit);
        if (tlView.getTransactionCode() != null) {
            transactionLimit.setTransactionCodeId(transactionCodeRepository.getTransactionCodeByCode(tlView.getTransactionCode()).map(TransactionCode::getId).orElseThrow(
                    () -> new ICEcashException(String.format("Transaction code '%s' does not exist!", tlView.getTransactionCode()), EC1061)));
        }
        if (tlView.getEntityType() != null) {
            transactionLimit.setEntityTypeId(entityTypeRepository.findByDescription(tlView.getEntityType()).map(EntityType::getId).orElseThrow(
                    () -> new ICEcashException(String.format("Entity type '%s' does not exist!", tlView.getEntityType()), EC1061)));
        }
        if (tlView.getAccountType() != null) {
            transactionLimit.setAccountTypeId(accountTypeRepository.findByNameAndCurrencyId(tlView.getAccountType(), transactionLimit.getCurrencyId()).map(AccountType::getId).orElseThrow(
                    () -> new ICEcashException(String.format("Account type '%s' for %s currency does not exist", tlView.getAccountType(), transactionLimit.getCurrencyId()), EC1060)));
        }
        if (tlView.getInitiatorType() != null) {
            transactionLimit.setInitiatorTypeId(initiatorTypeRepository.findByDescription(tlView.getInitiatorType()).map(InitiatorType::getId).orElseThrow(
                    () -> new ICEcashException(String.format("Initiator type '%s' does not exist", tlView.getInitiatorType()), EC1057)));
        }
        return transactionLimit;
    }

    private Example<TransactionLimit> getExample(TransactionLimit transactionLimit, boolean ignoreNulls) {
        ExampleMatcher exampleMatcher = ExampleMatcher.matching()
                .withMatcher("currency", ExampleMatcher.GenericPropertyMatchers.exact())
                .withMatcher("transactionCode", ExampleMatcher.GenericPropertyMatchers.exact())
                .withMatcher("kycStatusId", ExampleMatcher.GenericPropertyMatchers.exact())
                .withMatcher("entityType", ExampleMatcher.GenericPropertyMatchers.exact())
                .withMatcher("accountType", ExampleMatcher.GenericPropertyMatchers.exact())
                .withMatcher("initiatorType", ExampleMatcher.GenericPropertyMatchers.exact())
                .withMatcher("tier", ExampleMatcher.GenericPropertyMatchers.exact())
                .withMatcher("authorisationType", ExampleMatcher.GenericPropertyMatchers.exact())
                .withMatcher("direction", ExampleMatcher.GenericPropertyMatchers.exact())
                .withIgnorePaths("id", "active", "transactionMinLimit", "transactionMaxLimit", "dailyLimit", "weeklyLimit", "monthlyLimit", "createdDate");
        return Example.of(transactionLimit, ignoreNulls ? exampleMatcher.withIgnoreNullValues() : exampleMatcher.withIncludeNullValues());
    }
}

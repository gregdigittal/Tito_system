package cash.ice.fee.service.impl;

import cash.ice.common.error.ErrorCodes;
import cash.ice.fee.error.ICEcashInvalidRequestException;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

import static cash.ice.common.error.ErrorCodes.EC3032;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheableDataService {
    private final CurrencyRepository currencyRepository;
    private final InitiatorTypeRepository initiatorTypeRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final AccountRepository accountRepository;
    private final TransactionCodeRepository transactionCodeRepository;
    private final FeeRepository feeRepository;
    @PersistenceContext
    private final EntityManager entityManager;

    @Cacheable(value = "currency")
    public Currency getCurrency(String currencyIsoCode) {
        log.debug("  getting '{}' currency from DB", currencyIsoCode);
        return currencyRepository.findByIsoCode(currencyIsoCode)
                .orElseThrow(ICEcashInvalidRequestException.with("currency", currencyIsoCode));
    }

    @Cacheable(value = "initiator")
    public InitiatorType getInitiator(String initiatorType) {
        log.debug("  getting '{}' initiatorType from DB", initiatorType);
        return initiatorTypeRepository.findByDescription(initiatorType)
                .orElseThrow(ICEcashInvalidRequestException.with("initiatorType", initiatorType));
    }

    @Cacheable(value = "accountType")
    public AccountType getAccountType(Integer currencyId, String accountTypeName) {
        log.debug("  getting accountType from DB, currencyId: {}, accountTypeName: {}", currencyId, accountTypeName);
        return accountTypeRepository.findByNameAndCurrencyId(accountTypeName, currencyId).orElseThrow(() ->
                new ICEcashInvalidRequestException(String.format("AccountType '%s' for currencyId: %s does not exist",
                        accountTypeName, currencyId), EC3032));
    }

    @Cacheable(value = "accountType")
    public AccountType getAccountType(Integer accountTypeId) {
        log.debug("  getting accountType from DB, accountTypeId: {}", accountTypeId);
        return accountTypeRepository.findById(accountTypeId).orElseThrow(() ->
                new ICEcashInvalidRequestException(String.format("AccountType (id=%s) does not exist", accountTypeId), EC3032));
    }

    @Cacheable(value = "account")
    public Account getAccount(Integer entityId, Integer accountTypeId) {
        log.debug("  getting account with entityId='{}' accountTypeId='{}' from DB", entityId, accountTypeId);
        return accountRepository.findByEntityIdAndAccountTypeId(entityId, accountTypeId)
                .orElseThrow(ICEcashInvalidRequestException.with("account",
                        String.format("entityId=%s, accountTypeId=%s", entityId, accountTypeId)));
    }

    @Cacheable(value = "account")
    public Account getAccount(Integer accountId) {
        log.debug("  getting '{}' account from DB", accountId);
        return accountRepository.findById(accountId)
                .orElseThrow(ICEcashInvalidRequestException.with("account",
                        String.format("accountId=%s", accountId)));
    }

    @Cacheable(value = "account")
    public Account getAccount(String accountNumber) {
        log.debug("  getting '{}' account from DB", accountNumber);
        List<Account> accounts = accountRepository.findByAccountNumber(accountNumber);
        if (accounts.isEmpty()) {
            throw new ICEcashInvalidRequestException(String.format("Invalid account requested: accountNumber=%s", accountNumber), ErrorCodes.EC3002);
        }
        return accounts.get(0);
    }

    @Cacheable(value = "transactionCode")
    public TransactionCode getTransactionCode(String transactionCode) {
        log.debug("  getting '{}' transactionCode from DB", transactionCode);
        return transactionCodeRepository.getTransactionCodeByCode(transactionCode)
                .orElseThrow(ICEcashInvalidRequestException.with("transactionCode", transactionCode));
    }

    @Cacheable(value = "fees")
    public List<Fee> getFees(Integer transactionCodeId, Integer currencyId) {
        log.debug("  getting fees with transactionCodeId='{}' currencyId='{}' from DB", transactionCodeId, currencyId);
        List<Fee> fees = feeRepository.findByTransactionCodeIdAndCurrencyIdOrderByProcessOrder(transactionCodeId, currencyId);
        fees.forEach(entityManager::detach);
        return fees;
    }
}

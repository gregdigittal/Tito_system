package cash.ice.api.controller.moz;

import cash.ice.api.dto.OtpType;
import cash.ice.api.entity.backoffice.Journal;
import cash.ice.api.entity.ken.EntityProduct;
import cash.ice.api.entity.ken.Product;
import cash.ice.api.entity.moz.ProductRelationshipType;
import cash.ice.api.entity.moz.ProductType;
import cash.ice.api.errors.UnexistingUserException;
import cash.ice.api.service.*;
import cash.ice.common.constant.IceCashProfile;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.entity.moz.Device;
import cash.ice.sqldb.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequiredArgsConstructor
@Profile(IceCashProfile.NOT_PROD)
@Slf4j
public class UatRestController {
    private final KeycloakService keycloakService;
    private final FileMozService fileMozService;
    private final Me60MozService me60MozService;
    private final EntityKenService entityKenService;
    private final MfaService mfaService;
    private final AccountTransferMozService accountTransferMozService;
    private final TransactionStatisticsService transactionStatisticsService;
    private final EntityRepository entityRepository;
    private final EntityMsisdnRepository entityMsisdnRepository;
    private final AddressRepository addressRepository;
    private final CurrencyRepository currencyRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final EntityRegistrationService entityRegistrationService;
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountRelationshipRepository accountRelationshipRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionLinesRepository transactionLinesRepository;
    private final DocumentsService documentsService;
    private final JournalService journalService;
    private final OtpService otpService;

    @PostMapping("/api/v1/moz/account/topup")
    public void topupAccount(@RequestParam(value = "account") @Valid @NotBlank String account,
                             @RequestParam(value = "amount") @Valid @NotBlank BigDecimal amount,
                             @RequestParam(value = "reference") @Valid @NotBlank String reference) {
        log.info("> Top up account={}, amount={}, reference: {}", account, amount, reference);
        accountTransferMozService.topUpAccount(account, amount, reference);
    }

    @GetMapping("/api/v1/user/backoffice/forgot/key")
    @ResponseStatus(code = OK)
    public String getBackofficeForgotKey(@RequestParam String login) {
        log.info("> get backoffice forgot key: " + login);
        return mfaService.getForgotPasswordKey(login);
    }

    @GetMapping("/api/v1/user/totp/code")
    @ResponseStatus(code = OK)
    public String getTotpCode(@RequestParam String mfaSecretCode) {
        log.info("> get totp code: " + mfaSecretCode);
        return mfaService.getTotpCode(mfaSecretCode);
    }

    @GetMapping("/api/v1/moz/me60/tag/link/otp")
    @ResponseStatus(code = OK)
    public String linkTagGetOtp(@RequestParam String requestId) {
        log.info("> get otp: " + requestId);
        return me60MozService.getOtp(requestId);
    }

    @PostMapping("/api/v1/moz/me60/tag/link/clear")
    @ResponseStatus(code = OK)
    public Initiator addClearTag(@RequestParam String tag) {
        log.info("> add/clear tag: " + tag);
        return me60MozService.addClearTag(tag);
    }

    @PostMapping("/api/v1/moz/me60/device/activate")
    @ResponseStatus(code = OK)
    public Device activateDevice(@RequestParam String serialOrCode, @RequestParam String accountNumber) {
        log.info("> activate device: {}, accountNumber: {}", serialOrCode, accountNumber);
        return me60MozService.activateDevice(serialOrCode, accountNumber);
    }

    @DeleteMapping("/api/v1/moz/me60/device/remove")
    @ResponseStatus(code = OK)
    public Device removeDevice(@RequestParam String serialOrCode) {
        log.info("> remove device: {}", serialOrCode);
        return me60MozService.removeDevice(serialOrCode);
    }

    @DeleteMapping("/api/v1/moz/me60/tag/remove")
    @ResponseStatus(code = OK)
    public Initiator removeTag(@RequestParam String tag) {
        log.info("> remove tag: {}", tag);
        return me60MozService.removeTag(tag);
    }

    @PostMapping("/api/v1/moz/statistics/recalculate")
    @ResponseStatus(code = OK)
    public String recalculateTransactionsStatistics() {
        log.info("> recalculate transactions statistics");
        int records = transactionStatisticsService.recalculateTransactionsStatistics();
        return String.format("Done. Handled %s transactions", records);
    }

    @PostMapping("/api/v1/ken/product")
    @ResponseStatus(code = OK)
    public Product addProductFNDS(@RequestParam ProductType productType, @RequestParam String name,
                                  @RequestParam String description, @RequestParam Integer currencyId, @RequestParam BigDecimal price) {
        log.info("> add product, type: {}, name: {}, description: {}, currencyId: {}, price: {}", productType, name, description, currencyId, price);
        return entityKenService.saveProduct(new Product().setProductType(productType).setName(name).setDescription(description).setCurrencyId(currencyId).setPrice(price));
    }

    @DeleteMapping("/api/v1/ken/product/{productId}/remove")
    @ResponseStatus(code = OK)
    public Product removeProductFNDS(@PathVariable Integer productId) {
        log.info("> remove product: {}", productId);
        return entityKenService.removeProduct(productId);
    }

    @PostMapping("/api/v1/ken/entity/product")
    @ResponseStatus(code = OK)
    public EntityProduct entityProductFNDS(@RequestParam Integer entityId, @RequestParam Integer productId,
                                           @RequestParam ProductRelationshipType relationshipType, @RequestParam boolean active) {
        log.info("> set entity(id={})-product(id={}) relationship: {}, active: {}", entityId, productId, relationshipType, active);
        return entityKenService.updateEntityProductRelationship(entityId, productId, relationshipType, active);
    }

    @DeleteMapping("/api/v1/ken/entity/product")
    @ResponseStatus(code = OK)
    public String deleteEntityProductFNDS(@RequestParam Integer entityId, @RequestParam Integer productId, @RequestParam ProductRelationshipType relationshipType) {
        log.info("> delete entity(id={})-product(id={}) relationship: {}", entityId, productId, relationshipType);
        return entityKenService.deleteEntityProductRelationship(entityId, productId, relationshipType);
    }

    @PostMapping("/api/v1/user/{entityId}/transactions/clear")
    public List<AccountBalance> mozUserTransactionsClear(@PathVariable Integer entityId) {
        log.info("> Clear transactions for entity: {}", entityId);
        List<Account> accounts = accountRepository.findByEntityId(entityId);
        removeTransactions(accounts);
        accountBalanceRepository.saveAll(accounts.stream().map(account ->
                new AccountBalance().setAccountId(account.getId()).setBalance(BigDecimal.ZERO).setVersion(0)).toList());
        return accountBalanceRepository.findByAccountIdIn(accounts.stream().map(Account::getId).toList());
    }

    @PostMapping("/api/v1/user/account/balance")
    public BigDecimal userAccountBalance(@RequestParam(value = "entityName") @Valid @NotBlank String entityName,
                                         @RequestParam(value = "accountTypeName") @Valid @NotBlank String accountTypeName,
                                         @RequestParam(value = "currencyCode") @Valid @NotBlank String currencyCode) {
        log.info("> Get user account balance, entity: {}, accountTypeName: {}, currencyCode: {}", entityName, accountTypeName, currencyCode);
        EntityClass entity = entityRepository.findByFirstNameIn(List.of(entityName)).get(0);
        Currency currency = currencyRepository.findByIsoCode(currencyCode).orElseThrow();
        AccountType accountType = accountTypeRepository.findByNameAndCurrencyId(accountTypeName, currency.getId()).orElseThrow();
        Account account = accountRepository.findByEntityIdAndAccountTypeId(entity.getId(), accountType.getId()).orElseThrow();
        AccountBalance accountBalance = accountBalanceRepository.findByAccountId(account.getId()).orElse(new AccountBalance().setBalance(BigDecimal.ZERO));
        return accountBalance.getBalance();
    }

    @PostMapping("/api/v1/user/account/add")
    public String addAccountIfNeed(@RequestParam(value = "entityId") @Valid @NotBlank Integer entityId,
                                   @RequestParam(value = "accountTypeName") @Valid @NotBlank String accountTypeName,
                                   @RequestParam(value = "currencyCode") @Valid @NotBlank String currencyCode) {
        log.info("> Get user account balance, entityId: {}, accountTypeName: {}, currencyCode: {}", entityId, accountTypeName, currencyCode);
        EntityClass entity = entityRepository.findById(entityId).orElseThrow();
        Currency currency = currencyRepository.findByIsoCode(currencyCode).orElseThrow();
        AccountType accountType = accountTypeRepository.findByNameAndCurrencyId(accountTypeName, currency.getId()).orElseThrow();
        Account account = accountRepository.findByEntityIdAndAccountTypeId(entityId, accountType.getId()).orElse(null);
        if (account == null) {
            account = entityRegistrationService.saveAccount(entity, currency, accountTypeName, null);
        }
        return account.getAccountNumber();
    }

    @PostMapping("/api/v1/user/{entityId}/kyc/{kyc}")
    public void userKyc(@PathVariable Integer entityId, @PathVariable KYC kyc) {
        log.info("> Set KYC for entityId: {}, kyc: {}", entityId, kyc);
        EntityClass entity = entityRepository.findById(entityId).orElseThrow();
        entityRepository.save(entity.setKycStatusId(kyc.ordinal()));
    }

    @DeleteMapping("/api/v1/moz/user/{entityId}/remove")
    public void removeMozUser(@PathVariable Integer entityId) {
        log.info("> Remove moz entity: {}", entityId);
        EntityClass entity = removeUser(entityId);
        fileMozService.removePhoto(entity);
    }

    @DeleteMapping("/api/v1/user/{entityId}/remove")
    public EntityClass removeUser(@PathVariable Integer entityId) {
        log.info("> Remove entity: {}", entityId);
        EntityClass entity = entityRepository.findById(entityId).orElseThrow(() -> new UnexistingUserException("id: " + entityId));
        if (entity.getKeycloakId() != null) {
            try {
                keycloakService.removeUser(entity.getKeycloakId());
            } catch (NotFoundException e) {
                log.warn("Keycloak records is absent for entityId: " + entityId);
            }
        }
        documentsService.deleteDocumentsByEntityId(entityId);
        accountRelationshipRepository.deleteAll(accountRelationshipRepository.findByEntityIdIn(List.of(entityId)));
        List<EntityMsisdn> msisdnList = entityMsisdnRepository.findByEntityIdIn(List.of(entityId));
        entityMsisdnRepository.deleteAll(msisdnList);
        List<Address> addressList = addressRepository.findByEntityIdIn(List.of(entityId));
        addressRepository.deleteAll(addressList);
        List<Account> accounts = accountRepository.findByEntityId(entityId);
        removeTransactions(accounts);
        accountRepository.deleteAll(accounts);
        entityRepository.delete(entity);
        return entity;
    }

    @DeleteMapping("/api/v1/journal/{journalId}/remove")
    public Journal deleteJournal(@PathVariable Integer journalId) {
        log.debug("> delete journal: {}", journalId);
        return journalService.deleteJournal(journalId);
    }

    @GetMapping("/api/v1/moz/otp")
    public String getOtpPin(@RequestParam OtpType otpType,
                            @RequestParam(required = false) String msisdn,
                            @RequestParam(required = false) Integer entityId,
                            @RequestParam(required = false) String accountNumber) {
        log.info("GET OTP pin, type: {}, msisdn: {}, entityId: {} accountNumber: {}", otpType, msisdn, entityId, accountNumber);
        return otpService.restorePin(otpType, msisdn, entityId, accountNumber);
    }

    private void removeTransactions(List<Account> accounts) {
        List<Integer> transactionIds = transactionLinesRepository.findByEntityAccountIdIn(accounts.stream().map(Account::getId).toList())
                .stream().map(TransactionLines::getTransactionId).distinct().toList();
        transactionLinesRepository.deleteAll(transactionLinesRepository.findByTransactionIdIn(transactionIds));
        transactionRepository.deleteAllById(transactionIds);
        accountBalanceRepository.deleteAll(accountBalanceRepository.findByAccountIdIn(accounts.stream().map(Account::getId).toList()));
    }
}

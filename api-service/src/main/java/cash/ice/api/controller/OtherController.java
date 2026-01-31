package cash.ice.api.controller;

import cash.ice.api.dto.AuthUser;
import cash.ice.api.dto.OtpType;
import cash.ice.api.dto.SortInput;
import cash.ice.api.errors.UnexistingUserException;
import cash.ice.api.service.AuthUserService;
import cash.ice.api.service.OtpService;
import cash.ice.api.service.PermissionsService;
import cash.ice.api.util.MappingUtil;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

import static cash.ice.api.util.MappingUtil.itemsToCategoriesMap;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OtherController {
    private final static int DEFAULT_DIGITS_AMOUNT = 4;

    private final AuthUserService authUserService;
    private final PermissionsService permissionsService;
    private final OtpService otpService;
    private final EntityRepository entityRepository;
    private final EntityIdTypeRepository entityIdTypeRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final TransactionCodeRepository transactionCodeRepository;
    private final FeeRepository feeRepository;
    private final CurrencyRepository currencyRepository;

    @MutationMapping
    public String sendOtpToAccount(@Argument OtpType otpType, @Argument String accountNumber, @Argument boolean resend) {
        log.info("> send OTP: {}, accountNumber: {}, resend: {}", otpType, accountNumber, resend);
        otpService.sendOtpToAccount(otpType, accountNumber, DEFAULT_DIGITS_AMOUNT, resend);
        return "SUCCESS";
    }

    @MutationMapping
    public String sendOtpToEntity(@Argument OtpType otpType, @Argument Integer entityId, @Argument boolean resend) {
        log.info("> send OTP: {}, msisdn: {}, resend: {}", otpType, entityId, resend);
        EntityClass entity = entityId != null ? getEntityById(entityId) : permissionsService.getAuthEntity(getAuthUser());
        otpService.sendOtp(otpType, entity.getId(), DEFAULT_DIGITS_AMOUNT, resend);
        return "SUCCESS";
    }

    @MutationMapping
    public String sendOtp(@Argument OtpType otpType, @Argument String msisdn, @Argument boolean resend) {
        log.info("> send OTP: {}, msisdn: {}, resend: {}", otpType, msisdn, resend);
        otpService.sendOtp(otpType, msisdn, DEFAULT_DIGITS_AMOUNT, resend);
        return "SUCCESS";
    }

    @QueryMapping
    public Iterable<EntityIdType> allIdTypes(@Argument int page, @Argument int size, @Argument SortInput sort) {
        return entityIdTypeRepository.findAll(PageRequest.of(page, size, SortInput.toSort(sort)));
    }

    @QueryMapping
    public Iterable<DocumentType> documentTypes(@Argument Integer countryId) {
        return documentTypeRepository.findByCountryIdAndActive(countryId, true);
    }

    @QueryMapping
    public Page<TransactionCode> transactionCodes(@Argument Integer currencyId, @Argument int page, @Argument int size, @Argument SortInput sort) {
        log.info("> GET transaction codes. currencyId: {}, page: {}, size: {}, sort: {}", currencyId, page, size, sort);
        if (currencyId == null) {
            return transactionCodeRepository.findAll(PageRequest.of(page, size, SortInput.toSort(sort)));
        } else {
            return transactionCodeRepository.getTransactionCodeByCurrencyId(currencyId, PageRequest.of(page, size, SortInput.toSort(sort)));
        }
    }

    @QueryMapping
    public Iterable<Currency> allCurrencies(@Argument int page, @Argument int size, @Argument SortInput sort) {
        return currencyRepository.findAll(PageRequest.of(page, size, SortInput.toSort(sort)));
    }

    @BatchMapping(typeName = "AccountType", field = "currency")
    public Map<AccountType, Currency> currency(List<AccountType> accountTypes) {
        return itemsToCategoriesMap(accountTypes, AccountType::getCurrencyId, Currency::getId, currencyRepository);
    }

    @BatchMapping(typeName = "TransactionCode", field = "additionalFees")
    public Map<TransactionCode, List<Fee>> transactionCodeFees(List<TransactionCode> transactionCodes) {
        return MappingUtil.categoriesToItemsListMap(transactionCodes, TransactionCode::getId,
                fee -> fee.getTransactionCode().getId(), feeRepository::findNonOriginalByTransactionCodeIdIn);
    }

    @BatchMapping(typeName = "Fee", field = "currency")
    public Map<Fee, Currency> feesCurrencies(List<Fee> fees) {
        return MappingUtil.itemsToCategoriesMap(fees, Fee::getCurrencyId, Currency::getId, currencyRepository);
    }

    public EntityClass getEntityById(Integer entityId) {
        return entityRepository.findById(entityId).orElseThrow(() -> new UnexistingUserException("id: " + entityId));
    }

    protected AuthUser getAuthUser() {
        return authUserService.getAuthUser();
    }
}

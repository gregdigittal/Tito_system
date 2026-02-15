package cash.ice.api.controller;

import cash.ice.api.dto.SortInput;
import cash.ice.api.dto.TransactionLimitView;
import cash.ice.api.service.LoggerService;
import cash.ice.api.service.PaymentService;
import cash.ice.api.service.TransactionLimitService;
import cash.ice.api.util.MappingUtil;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;
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

@Controller
@RequiredArgsConstructor
@Slf4j
public class PaymentsController {
    private final PaymentService paymentService;
    private final LoggerService loggerService;
    private final TransactionLimitService transactionLimitService;
    private final CurrencyRepository currencyRepository;
    private final TransactionCodeRepository transactionCodeRepository;
    private final EntityTypeRepository entityTypeRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final InitiatorTypeRepository initiatorTypeRepository;

    @MutationMapping
    public PaymentResponse makePayment(@Argument PaymentRequest paymentRequest, @Argument boolean waitForResponse) {
        if (waitForResponse) {
            return paymentService.makePaymentSynchronous(paymentRequest);
        } else {
            paymentService.addPayment(paymentRequest);
            PaymentResponse response = loggerService.getResponse(paymentRequest.getVendorRef(), PaymentResponse.class);
            return response != null ? response : PaymentResponse.processing(paymentRequest.getVendorRef());
        }
    }

    @QueryMapping
    public PaymentRequest paymentRequest(@Argument String vendorRef) {
        return paymentService.getPaymentRequest(vendorRef);
    }

    @QueryMapping
    public PaymentResponse paymentResponse(@Argument String vendorRef) {
        return paymentService.getPaymentResponse(vendorRef);
    }

    @QueryMapping
    public Page<TransactionLimit> transactionLimits(@Argument TransactionLimitView filter, @Argument int page, @Argument int size, @Argument SortInput sort) {
        log.info("> GET TransactionLimits: {}, page: {}, size: {}, sort: {}", filter.criteriaToString(), page, size, sort);
        return transactionLimitService.get(filter, PageRequest.of(page, size, SortInput.toSort(sort)));
    }

    @MutationMapping
    public TransactionLimit addOrUpdateTransactionLimit(@Argument TransactionLimitView transactionLimit) {
        log.info("> Update TransactionLimit: {}", transactionLimit);
        return transactionLimitService.addOrUpdate(transactionLimit);
    }

    @MutationMapping
    public TransactionLimit setTransactionLimitActive(@Argument TransactionLimitView transactionLimit, @Argument boolean active) {
        log.info("> Set TransactionLimit active: {}, {}", active, transactionLimit.criteriaToString());
        return transactionLimitService.setActive(transactionLimit, active);
    }

    @MutationMapping
    public TransactionLimit deleteTransactionLimit(@Argument Integer id) {
        log.info("> Delete TransactionLimit: {}", id);
        return transactionLimitService.delete(id);
    }

    @BatchMapping(typeName = "TransactionLimit", field = "currency")
    public Map<TransactionLimit, String> transactionLimitCurrency(List<TransactionLimit> transactionLimits) {
        return MappingUtil.itemsToCategoriesMap(transactionLimits, TransactionLimit::getCurrencyId,
                Currency::getId, currencyRepository, Currency::getIsoCode);
    }

    @BatchMapping(typeName = "TransactionLimit", field = "transactionCode")
    public Map<TransactionLimit, String> transactionLimitTransactionCode(List<TransactionLimit> transactionLimits) {
        return MappingUtil.itemsToCategoriesMap(transactionLimits, TransactionLimit::getTransactionCodeId,
                TransactionCode::getId, transactionCodeRepository, TransactionCode::getCode);
    }

    @BatchMapping(typeName = "TransactionLimit", field = "entityType")
    public Map<TransactionLimit, String> transactionLimitEntityType(List<TransactionLimit> transactionLimits) {
        return MappingUtil.itemsToCategoriesMap(transactionLimits, TransactionLimit::getEntityTypeId,
                EntityType::getId, entityTypeRepository, EntityType::getDescription);
    }

    @BatchMapping(typeName = "TransactionLimit", field = "accountType")
    public Map<TransactionLimit, String> transactionLimitAccountType(List<TransactionLimit> transactionLimits) {
        return MappingUtil.itemsToCategoriesMap(transactionLimits, TransactionLimit::getAccountTypeId,
                AccountType::getId, accountTypeRepository, AccountType::getName);
    }

    @BatchMapping(typeName = "TransactionLimit", field = "initiatorType")
    public Map<TransactionLimit, String> transactionLimitInitiatorType(List<TransactionLimit> transactionLimits) {
        return MappingUtil.itemsToCategoriesMap(transactionLimits, TransactionLimit::getInitiatorTypeId,
                InitiatorType::getId, initiatorTypeRepository, InitiatorType::getDescription);
    }
}

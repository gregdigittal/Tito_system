package cash.ice.api.service.impl;

import cash.ice.api.config.property.KenProperties;
import cash.ice.api.dto.moz.AccountTypeKen;
import cash.ice.api.dto.moz.IdTypeKen;
import cash.ice.api.entity.ken.EntityProduct;
import cash.ice.api.entity.ken.Product;
import cash.ice.api.entity.moz.ProductRelationshipType;
import cash.ice.api.errors.Me60Exception;
import cash.ice.api.repository.ken.EntityKenRepository;
import cash.ice.api.repository.ken.EntityProductRepository;
import cash.ice.api.repository.ken.ProductRepository;
import cash.ice.api.service.EntityKenService;
import cash.ice.api.service.NotificationService;
import cash.ice.api.service.PaymentService;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;
import cash.ice.api.dto.moz.PaymentResponseMoz;
import cash.ice.common.dto.ResponseStatus;
import cash.ice.common.error.ApiValidationException;
import cash.ice.common.error.ICEcashException;
import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.EntityMsisdn;
import cash.ice.sqldb.entity.EntityType;
import cash.ice.sqldb.entity.moz.Device;
import cash.ice.sqldb.entity.moz.DeviceStatus;
import cash.ice.sqldb.repository.AccountRepository;
import cash.ice.sqldb.repository.EntityMsisdnRepository;
import cash.ice.sqldb.repository.EntityTypeRepository;
import cash.ice.sqldb.repository.moz.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import static cash.ice.common.error.ErrorCodes.*;
import static cash.ice.common.utils.Tool.moneyRound;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntityKenServiceImpl implements EntityKenService {
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final EntityKenRepository entityKenRepository;
    private final EntityTypeRepository entityTypeRepository;
    private final EntityMsisdnRepository entityMsisdnRepository;
    private final AccountRepository accountRepository;
    private final DeviceRepository deviceRepository;
    private final ProductRepository productRepository;
    private final EntityProductRepository entityProductRepository;
    private final KenProperties kenProperties;

    @Override
    public Page<EntityClass> entitiesSearchFNDS(AccountTypeKen accountType, IdTypeKen idType, String idNumber, String mobile, PageRequest pageable) {
        EntityType entityType = entityTypeRepository.findByDescription(accountType.getEntityType()).orElseThrow(() ->
                new ApiValidationException("Unknown entityType: " + accountType, EC1011));
        if (Strings.isEmpty(mobile)) {
            return entityKenRepository.findByEntityTypeAndMobileAndId(entityType.getId(), idType != null ? idType.getDbId() : null, idNumber, pageable);
        } else {
            List<EntityMsisdn> msisdnList = entityMsisdnRepository.findByMsisdn(mobile);
            List<Integer> entityIds = msisdnList.stream().map(EntityMsisdn::getEntityId).filter(Objects::nonNull).distinct().toList();
            return entityKenRepository.findByEntityTypeAndMobileAndIdAndEntityIds(entityType.getId(),
                    idType != null ? idType.getDbId() : null, idNumber, entityIds, pageable);
        }
    }

    @Override
    public List<EntityProduct> getDeviceProductsFNDS(String deviceSerialOrCode) {
        Device device = getAndValidateDevice(deviceSerialOrCode);
        Account account = accountRepository.findById(device.getAccountId()).orElseThrow(() ->
                new ICEcashException("Invalid linked account", EC1022));
        EntityClass entity = entityKenRepository.findById(account.getEntityId())
                .orElseThrow(() -> new ICEcashException("Entity with id=" + account.getEntityId() + " does not exist", EC1048));
        return entityProductRepository.findByEntityIdAndRelationshipType(entity.getId(), ProductRelationshipType.DealerStock);
    }

    private Device getAndValidateDevice(String deviceSerialOrCode) {
        Device device = deviceRepository.findBySerial(deviceSerialOrCode).orElseGet(
                () -> deviceRepository.findByCode(deviceSerialOrCode).orElse(null));
        if (device == null) {
            throw new Me60Exception("Device is not registered", deviceSerialOrCode, EC1055);
        } else if (device.getStatus() != DeviceStatus.ACTIVE) {
            throw new Me60Exception("Device is inactive", deviceSerialOrCode, EC1055);
        } else if (device.getAccountId() == null) {
            throw new Me60Exception("No account is linked to device", deviceSerialOrCode, EC1055);
        }
        return device;
    }

    @Override
    public PaymentResponse makePayment(PaymentRequest paymentRequest) {
        return paymentService.makePaymentSynchronous(paymentRequest,
                kenProperties.getPaymentTimeoutDuration(), this::postPaymentAction);
    }

    @Override
    public PaymentResponse makeBulkPayment(List<PaymentRequest> paymentRequestList) {
        List<PaymentResponse> paymentResponses = paymentService.makeBulkPaymentSynchronous(paymentRequestList, true,
                kenProperties.getPaymentTimeoutDuration(), this::postPaymentAction);
        return PaymentResponseMoz.success(paymentResponses.getLast());
    }

    private void postPaymentAction(PaymentRequest paymentRequest, PaymentResponse paymentResponse) {
        if (paymentResponse.getStatus() == ResponseStatus.SUCCESS) {
            if (kenProperties.isPaymentConfirmationSmsEnable() && paymentResponse.getPrimaryMsisdn() != null) {
                notificationService.sendSmsMessage(String.format(kenProperties.getPaymentConfirmationSmsMessageEn(),
                        moneyRound(paymentRequest.getAmount()),
                        paymentRequest.getInitiator(),
                        paymentResponse.getDate() != null ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(paymentResponse.getDate()) : ""
                ), paymentResponse.getPrimaryMsisdn());
            } else {
                log.warn("Cannot send SMS, msisdn is null, request: {}, response: {}", paymentRequest, paymentResponse);
            }
        }
    }

    @Override
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    public EntityProduct updateEntityProductRelationship(Integer entityId, Integer productId, ProductRelationshipType relationshipType, boolean active) {
        entityKenRepository.findById(entityId)
                .orElseThrow(() -> new ICEcashException("Entity with id=" + entityId + " does not exist", EC1048));
        productRepository.findById(productId)
                .orElseThrow(() -> new ICEcashException("Product with id=" + productId + " does not exist", EC1048));
        List<EntityProduct> entityProducts = entityProductRepository.findByEntityIdAndProductIdAndRelationshipType(entityId, productId, relationshipType);
        if (entityProducts.isEmpty()) {
            entityProducts = List.of(new EntityProduct().setEntityId(entityId).setProductId(productId).setRelationshipType(relationshipType).setCreatedDate(Tool.currentDateTime()));
        }
        entityProducts.forEach(entityProduct -> entityProduct.setActive(active).setModifiedDate(Tool.currentDateTime()));
        return entityProductRepository.saveAll(entityProducts).getLast();
    }

    @Override
    public String deleteEntityProductRelationship(Integer entityId, Integer productId, ProductRelationshipType relationshipType) {
        List<EntityProduct> entityProducts = entityProductRepository.findByEntityIdAndProductIdAndRelationshipType(entityId, productId, relationshipType);
        if (!entityProducts.isEmpty()) {
            entityProductRepository.deleteAll(entityProducts);
        }
        return String.format("removed %s relationships", entityProducts.size());
    }

    @Override
    public Product removeProduct(Integer productId) {
        Product product = productRepository.findById(productId).orElseThrow();
        entityProductRepository.deleteAll(entityProductRepository.findByProductId(product.getId()));
        productRepository.delete(product);
        return product;
    }
}

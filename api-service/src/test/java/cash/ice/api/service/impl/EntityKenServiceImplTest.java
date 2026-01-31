package cash.ice.api.service.impl;

import cash.ice.api.config.property.KenProperties;
import cash.ice.api.dto.moz.AccountTypeKen;
import cash.ice.api.dto.moz.IdTypeKen;
import cash.ice.api.entity.ken.EntityProduct;
import cash.ice.api.entity.moz.ProductRelationshipType;
import cash.ice.api.repository.ken.EntityKenRepository;
import cash.ice.api.repository.ken.EntityProductRepository;
import cash.ice.api.repository.ken.ProductRepository;
import cash.ice.api.service.NotificationService;
import cash.ice.api.service.PaymentService;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;
import cash.ice.common.dto.ResponseStatus;
import cash.ice.common.error.ICEcashException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static cash.ice.common.error.ErrorCodes.EC1055;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityKenServiceImplTest {
    private static final IdTypeKen IT_TYPE = IdTypeKen.NationalID;
    private static final String ID_NUMBER = "12345678";
    private static final String MOBILE = "000000000000";
    private static final String TRANSACTION_ID = "12";
    private static final BigDecimal BALANCE = new BigDecimal("100");
    private static final Duration MAX_WAIT = Duration.ofMinutes(1);
    private static final int ENTITY_TYPE_ID = 14;
    private static final int ACCOUNT_ID = 1;
    private static final int ENTITY_ID = 2;
    private static final String VENDOR_REF = "Test VendorRef";
    private static final String DEVICE_SERIAL = "serial";

    @Mock
    private NotificationService notificationService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private EntityKenRepository entityKenRepository;
    @Mock
    private EntityTypeRepository entityTypeRepository;
    @Mock
    private EntityMsisdnRepository entityMsisdnRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private EntityProductRepository entityProductRepository;
    @Mock
    private KenProperties kenProperties;
    @InjectMocks
    private EntityKenServiceImpl service;

    @Test
    void testEntitiesSearchFNDS() {
        PageRequest pageable = PageRequest.of(0, 10);
        List<EntityClass> entities = List.of(new EntityClass().setId(ENTITY_ID));

        when(entityTypeRepository.findByDescription(AccountTypeKen.Farmer.getEntityType())).thenReturn(Optional.of(
                new EntityType().setId(ENTITY_TYPE_ID)));
        when(entityMsisdnRepository.findByMsisdn(MOBILE)).thenReturn(List.of(new EntityMsisdn().setEntityId(ENTITY_ID)));
        when(entityKenRepository.findByEntityTypeAndMobileAndIdAndEntityIds(ENTITY_TYPE_ID, IT_TYPE.getDbId(),
                ID_NUMBER, List.of(ENTITY_ID), pageable)).thenReturn(new PageImpl<>(entities, pageable, entities.size()));

        Page<EntityClass> result = service.entitiesSearchFNDS(AccountTypeKen.Farmer, IT_TYPE, ID_NUMBER, MOBILE, pageable);
        assertEquals(result.getTotalElements(), entities.size());
        assertEquals(result.getContent(), entities);
    }

    @Test
    void testEntitiesSearchFNDSWithNoMobile() {
        PageRequest pageable = PageRequest.of(0, 10);
        List<EntityClass> entities = List.of(new EntityClass().setId(ENTITY_ID));

        when(entityTypeRepository.findByDescription(AccountTypeKen.Farmer.getEntityType())).thenReturn(Optional.of(
                new EntityType().setId(ENTITY_TYPE_ID)));
        when(entityKenRepository.findByEntityTypeAndMobileAndId(ENTITY_TYPE_ID, IT_TYPE.getDbId(),
                ID_NUMBER, pageable)).thenReturn(new PageImpl<>(entities, pageable, entities.size()));

        Page<EntityClass> result = service.entitiesSearchFNDS(AccountTypeKen.Farmer, IT_TYPE, ID_NUMBER, null, pageable);
        assertEquals(result.getTotalElements(), entities.size());
        assertEquals(result.getContent(), entities);
    }

    @Test
    void testGetDeviceProductsFNDS() {
        List<EntityProduct> entityProducts = List.of(new EntityProduct());

        when(deviceRepository.findBySerial(DEVICE_SERIAL)).thenReturn(Optional.of(
                new Device().setAccountId(ACCOUNT_ID).setStatus(DeviceStatus.ACTIVE)));
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(new Account().setId(ACCOUNT_ID).setEntityId(ENTITY_ID)));
        when(entityKenRepository.findById(ENTITY_ID)).thenReturn(Optional.of(new EntityClass().setId(ENTITY_ID)));
        when(entityProductRepository.findByEntityIdAndRelationshipType(ENTITY_ID, ProductRelationshipType.DealerStock))
                .thenReturn(entityProducts);

        List<EntityProduct> result = service.getDeviceProductsFNDS(DEVICE_SERIAL);
        assertEquals(entityProducts, result);
    }

    @Test
    void testGetDeviceProductsFNDSWhenDeviceNotFound() {
        when(deviceRepository.findBySerial(any())).thenReturn(Optional.empty());
        ICEcashException exception = assertThrows(ICEcashException.class, () -> service.getDeviceProductsFNDS(DEVICE_SERIAL));
        assertThat(exception.getErrorCode()).isEqualTo(EC1055);
    }

    @Test
    void testMakePayment() {
        PaymentRequest request = new PaymentRequest().setVendorRef(VENDOR_REF);
        PaymentResponse response = PaymentResponse.success(VENDOR_REF, TRANSACTION_ID, BigDecimal.TEN, MOBILE, "en");

        when(kenProperties.getPaymentTimeoutDuration()).thenReturn(MAX_WAIT);
        when(paymentService.makePaymentSynchronous(eq(request), eq(MAX_WAIT), any())).thenReturn(response);

        PaymentResponse actualResponse = service.makePayment(request);
        assertEquals(response, actualResponse);
    }

    @Test
    void testMakeBulkPayment() {
        var paymentRequest = List.of(new PaymentRequest().setVendorRef(VENDOR_REF), new PaymentRequest().setVendorRef("ref2"));
        var paymentResponse = PaymentResponse.success(VENDOR_REF, TRANSACTION_ID, BALANCE, MOBILE, "en");

        when(kenProperties.getPaymentTimeoutDuration()).thenReturn(MAX_WAIT);
        when(paymentService.makeBulkPaymentSynchronous(eq(paymentRequest), eq(true), eq(MAX_WAIT), any()))
                .thenReturn(List.of(paymentResponse));

        var actualResponse = service.makeBulkPayment(paymentRequest);
        assertThat(actualResponse.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(actualResponse.getMessage()).isEqualTo("Transaction offload completed");
        assertThat(actualResponse.getBalance()).isEqualTo(BALANCE);
        assertThat(actualResponse.getDate()).isNotNull();
    }
}
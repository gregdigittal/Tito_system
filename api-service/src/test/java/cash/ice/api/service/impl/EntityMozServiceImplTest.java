package cash.ice.api.service.impl;

import cash.ice.api.config.property.MozProperties;
import cash.ice.api.dto.OtpType;
import cash.ice.api.dto.moz.IdTypeMoz;
import cash.ice.api.dto.moz.LookupEntityType;
import cash.ice.api.dto.moz.MoneyProviderMoz;
import cash.ice.api.service.Me60MozService;
import cash.ice.api.service.OtpService;
import cash.ice.api.service.PermissionsService;
import cash.ice.common.constant.PaymentMetaKey;
import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.PaymentResponse;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.entity.moz.Device;
import cash.ice.sqldb.entity.moz.DeviceStatus;
import cash.ice.sqldb.repository.*;
import cash.ice.sqldb.repository.moz.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static cash.ice.sqldb.entity.AccountType.PREPAID_TRANSPORT;
import static cash.ice.sqldb.entity.AccountType.PRIMARY_ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityMozServiceImplTest {
    private static final String MOBILE = "123456789012";
    private static final String DESCRIPTION = "some description";
    private static final String OTP = "1234";
    private static final int ENTITY_ID = 1;
    private static final int ACCOUNT_ID = 2;
    private static final int ACCOUNT_TYPE_ID = 3;
    private static final int CURRENCY_ID = 12;
    private static final String CURRENCY_CODE = "MZN";
    private static final String MSISDN = "12345678";
    private static final String ACCOUNT_NUMBER = "123456789";
    private static final String ID_NUMBER = "1234567";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final BigDecimal AMOUNT = BigDecimal.TEN;

    @Mock
    private EntityRepository entityRepository;
    @Mock
    private PermissionsService permissionsService;
    @Mock
    private OtpService otpService;
    @Mock
    private Me60MozService me60MozService;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountTypeRepository accountTypeRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private InitiatorRepository initiatorRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionLinesRepository transactionLinesRepository;
    @Mock
    private EntityMsisdnRepository entityMsisdnRepository;
    @Mock
    private MozProperties mozProperties;
    @Captor
    private ArgumentCaptor<PaymentRequest> paymentRequestArgumentCaptor;
    @InjectMocks
    private EntityMozServiceImpl service;

    @Test
    void testGetEntityInitiators() {
        EntityClass entity = new EntityClass().setId(ENTITY_ID);
        Initiator initiator1 = new Initiator().setInitiatorTypeId(4).setInitiatorCategoryId(5).setIdentifier("ident1").setInitiatorStatusId(1);
        Initiator initiator2 = new Initiator().setInitiatorTypeId(14).setInitiatorCategoryId(15).setIdentifier("ident2").setInitiatorStatusId(7);

        when(currencyRepository.findByIsoCode(CURRENCY_CODE)).thenReturn(Optional.of(new Currency().setId(CURRENCY_ID)));
        when(accountTypeRepository.findByNameAndCurrencyId(PREPAID_TRANSPORT, CURRENCY_ID)).thenReturn(Optional.of(new AccountType().setId(ACCOUNT_TYPE_ID)));
        when(accountRepository.findByEntityIdAndAccountTypeId(ENTITY_ID, ACCOUNT_TYPE_ID)).thenReturn(Optional.of(new Account().setId(ACCOUNT_ID)));
        when(initiatorRepository.findByAccountId(ACCOUNT_ID, PageRequest.of(0, 30))).thenReturn(new PageImpl<>(List.of(initiator1, initiator2)));

        Page<Initiator> actualEntityDevices = service.getEntityInitiators(entity, 0, 30, null);
        assertThat(actualEntityDevices).isEqualTo(new PageImpl<>(List.of(initiator1, initiator2)));
    }

    @Test
    void testGetEntityLinkedDevices() {
        EntityClass entity = new EntityClass().setId(ENTITY_ID);
        Device device1 = new Device().setCode("code1").setSerial("serial1").setStatus(DeviceStatus.ACTIVE).setMeta(Map.of("metaField1", "value1"));
        Device device2 = new Device().setCode("code2").setSerial("serial2").setStatus(DeviceStatus.INACTIVE).setMeta(Map.of("metaField2", "value2"));

        when(currencyRepository.findByIsoCode(CURRENCY_CODE)).thenReturn(Optional.of(new Currency().setId(CURRENCY_ID)));
        when(accountTypeRepository.findByNameAndCurrencyId(PRIMARY_ACCOUNT, CURRENCY_ID)).thenReturn(Optional.of(new AccountType().setId(ACCOUNT_TYPE_ID)));
        when(accountRepository.findByEntityIdAndAccountTypeId(ENTITY_ID, ACCOUNT_TYPE_ID)).thenReturn(Optional.of(new Account().setId(ACCOUNT_ID)));
        when(deviceRepository.findByAccountIdAndVehicleIdIsNull(ACCOUNT_ID, PageRequest.of(0, 30))).thenReturn(new PageImpl<>(List.of(device1, device2)));

        Page<Device> actualEntityDevices = service.getEntityDevices(entity, false, 0, 30, null);
        assertThat(actualEntityDevices).isEqualTo(new PageImpl<>(List.of(device1, device2)));
    }

    @Test
    void testGetEntityUnlinkedDevices() {
        EntityClass entity = new EntityClass().setId(ENTITY_ID);
        Device device1 = new Device().setCode("code1").setSerial("serial1").setStatus(DeviceStatus.ACTIVE).setMeta(Map.of("metaField1", "value1"));
        Device device2 = new Device().setCode("code2").setSerial("serial2").setStatus(DeviceStatus.INACTIVE).setMeta(Map.of("metaField2", "value2"));

        when(currencyRepository.findByIsoCode(CURRENCY_CODE)).thenReturn(Optional.of(new Currency().setId(CURRENCY_ID)));
        when(accountTypeRepository.findByNameAndCurrencyId(PRIMARY_ACCOUNT, CURRENCY_ID)).thenReturn(Optional.of(new AccountType().setId(ACCOUNT_TYPE_ID)));
        when(accountRepository.findByEntityIdAndAccountTypeId(ENTITY_ID, ACCOUNT_TYPE_ID)).thenReturn(Optional.of(new Account().setId(ACCOUNT_ID)));
        when(deviceRepository.findByAccountIdAndVehicleIdIsNotNull(ACCOUNT_ID, PageRequest.of(0, 30))).thenReturn(new PageImpl<>(List.of(device1, device2)));

        Page<Device> actualEntityDevices = service.getEntityDevices(entity, true, 0, 30, null);
        assertThat(actualEntityDevices).isEqualTo(new PageImpl<>(List.of(device1, device2)));
    }

    @Test
    void testLookupEntityByMsisdn() {
        when(entityMsisdnRepository.findByMsisdn(MSISDN)).thenReturn(List.of(new EntityMsisdn().setEntityId(ENTITY_ID)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(new EntityClass().setId(ENTITY_ID)
                .setStatus(EntityStatus.ACTIVE).setFirstName(FIRST_NAME).setLastName(LAST_NAME)));
        List<EntityClass> actualEntities = service.lookupEntity(LookupEntityType.MSISDN, null, MSISDN);
        assertThat(actualEntities.size()).isEqualTo(1);
        assertThat(actualEntities.get(0).getId()).isEqualTo(ENTITY_ID);
        assertThat(actualEntities.get(0).getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(actualEntities.get(0).getLastName()).isEqualTo(LAST_NAME);
    }

    @Test
    void testLookupEntityByAccountNumber() {
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(List.of(new Account().setEntityId(ENTITY_ID)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(new EntityClass().setId(ENTITY_ID).setStatus(EntityStatus.ACTIVE).setFirstName(FIRST_NAME).setLastName(LAST_NAME)));
        List<EntityClass> actualEntities = service.lookupEntity(LookupEntityType.ACCOUNT, null, ACCOUNT_NUMBER);
        assertThat(actualEntities.size()).isEqualTo(1);
        assertThat(actualEntities.get(0).getId()).isEqualTo(ENTITY_ID);
        assertThat(actualEntities.get(0).getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(actualEntities.get(0).getLastName()).isEqualTo(LAST_NAME);
    }

    @Test
    void testLookupEntityByIdNumber() {
        IdTypeMoz idType = IdTypeMoz.DIRE;
        when(entityRepository.findByIdNumberAndIdType(ID_NUMBER, idType.getDbId())).thenReturn(List.of(
                new EntityClass().setId(ENTITY_ID).setStatus(EntityStatus.ACTIVE).setFirstName(FIRST_NAME).setLastName(LAST_NAME)));
        List<EntityClass> actualEntities = service.lookupEntity(LookupEntityType.ID, idType, ID_NUMBER);
        assertThat(actualEntities.size()).isEqualTo(1);
        assertThat(actualEntities.get(0).getId()).isEqualTo(ENTITY_ID);
        assertThat(actualEntities.get(0).getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(actualEntities.get(0).getLastName()).isEqualTo(LAST_NAME);
    }

    @Test
    void testUpdatePrimaryMsisdn() {
        EntityClass entity = new EntityClass().setId(ENTITY_ID);
        EntityMsisdn msisdn = new EntityMsisdn();
        when(mozProperties.isLinkTagCheckOtp()).thenReturn(true);
        when(entityMsisdnRepository.findByEntityIdAndPrimaryMsisdn(ENTITY_ID)).thenReturn(Optional.of(msisdn));

        EntityClass actualEntity = service.addOrUpdateMsisdn(entity, MsisdnType.PRIMARY, MOBILE, null, DESCRIPTION, OTP);
        assertThat(actualEntity).isEqualTo(entity);
        assertThat(msisdn.getMsisdn()).isEqualTo(MOBILE);
        assertThat(msisdn.getDescription()).isEqualTo(DESCRIPTION);
        verify(otpService).validateOtp(OtpType.MOZ_MSISDN_UPDATE, MOBILE, OTP);
        verify(entityMsisdnRepository).save(msisdn);
    }

    @Test
    void testUpdateSecondaryMsisdn() {
        EntityClass entity = new EntityClass().setId(ENTITY_ID);
        EntityMsisdn msisdn = new EntityMsisdn().setMsisdn("000");
        when(entityMsisdnRepository.findByEntityIdAndMsisdnType(ENTITY_ID, MsisdnType.SECONDARY)).thenReturn(List.of(new EntityMsisdn().setMsisdn("123"), msisdn));

        EntityClass actualEntity = service.addOrUpdateMsisdn(entity, MsisdnType.SECONDARY, MOBILE, "000", DESCRIPTION, null);
        assertThat(actualEntity).isEqualTo(entity);
        assertThat(msisdn.getMsisdn()).isEqualTo(MOBILE);
        assertThat(msisdn.getDescription()).isEqualTo(DESCRIPTION);
        verify(entityMsisdnRepository).save(msisdn);
    }

    @Test
    void testTopupAccount() {
        EntityClass entity = new EntityClass().setId(ENTITY_ID);
        PaymentResponse response = PaymentResponse.success("vendorRef", "txId", BigDecimal.ONE, null, null);
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(List.of(new Account().setEntityId(ENTITY_ID)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(entity));
        when(me60MozService.makePayment(paymentRequestArgumentCaptor.capture())).thenReturn(response);

        PaymentResponse actualResponse = service.topupAccount(entity, ACCOUNT_NUMBER, MoneyProviderMoz.MPESA, MOBILE, AMOUNT);
        PaymentRequest actualPaymentRequest = paymentRequestArgumentCaptor.getValue();
        assertThat(actualPaymentRequest.getVendorRef()).isNotBlank();
        assertThat(actualPaymentRequest.getTx()).isEqualTo(MoneyProviderMoz.MPESA.getInboundTx());
        assertThat(actualPaymentRequest.getInitiatorType()).isEqualTo(MoneyProviderMoz.MPESA.getInitiatorType());
        assertThat(actualPaymentRequest.getInitiator()).isEqualTo(MOBILE);
        assertThat(actualPaymentRequest.getCurrency()).isEqualTo(Currency.MZN);
        assertThat(actualPaymentRequest.getAmount()).isEqualTo(AMOUNT);
        assertThat(actualPaymentRequest.getDate()).isNotNull();
        assertThat(actualPaymentRequest.getMeta().get(PaymentMetaKey.AccountNumber)).isEqualTo(ACCOUNT_NUMBER);
        assertThat(actualResponse).isEqualTo(response);
    }
}
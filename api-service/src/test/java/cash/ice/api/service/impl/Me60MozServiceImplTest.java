package cash.ice.api.service.impl;

import cash.ice.api.config.property.MozProperties;
import cash.ice.api.dto.moz.*;
import cash.ice.api.service.*;
import cash.ice.common.service.KafkaSender;
import cash.ice.common.utils.Tool;
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
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static cash.ice.sqldb.entity.AccountType.SUBSIDY_ACCOUNT;
import static cash.ice.sqldb.entity.InitiatorType.TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Me60MozServiceImplTest {
    private static final String LOCALE = "pt";
    private static final String SERIAL = "sn1";
    private static final int DEVICE_ID = 1;
    private static final String ACCOUNT_NUMBER = "12345678";
    private static final int SUBSIDY_ACCOUNT_TYPE_ID = 86;
    private static final int SUBSIDY_ACCOUNT_ID = 52;
    private static final int ACCOUNT_ID = 2;
    private static final int ENTITY_ID = 3;
    private static final String MSISDN = "1234";
    private static final String MSISDN2 = "2345";
    private static final String MSISDN3 = "3456";
    private static final int INITIATOR_TYPE_ID = 4;
    private static final int INITIATOR_CATEGORY_ID = 5;
    private static final int ACTIVE_INITIATOR_STATUS_ID = 6;
    private static final int UNASSIGNED_INITIATOR_STATUS_ID = 7;
    private static final String REQUEST_ID = "req";
    private static final String COLLECTION = "collection1";
    private static final String OTP = "1234";
    private static final String OTP_KEY = "otpKey";
    private static final String PVV = "pvv";
    private static final String TEST_TAG = "tag1";
    private static final int INITIATOR_ID = 7;
    private static final int ACCOUNT_TYPE_ID = 8;
    private static final int PREPAID_ACCOUNT_TYPE_ID = 53;
    private static final String ACCOUNT_TYPE_DESCRIPTION = "accTypeDesc";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String VENDOR_REF = "DEV112345678901";
    private static final String VENDOR_REF2 = "DEV112345678902";
    private static final String VENDOR_REF3 = "DEV112345678903";
    private static final String TRANSACTION_ID = "10";
    private static final int CURRENCY_ID = 32;
    private static final BigDecimal PREPAID_ACCOUNT_BALANCE = new BigDecimal("100.0");
    private static final BigDecimal SUBSIDY_ACCOUNT_BALANCE = new BigDecimal("10.0");
    private static final int INITIATOR_ENTITY_ID = 54;

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private AccountBalanceRepository accountBalanceRepository;
    @Mock
    private AccountTypeRepository accountTypeRepository;
    @Mock
    private EntityMsisdnRepository entityMsisdnRepository;
    @Mock
    private InitiatorRepository initiatorRepository;
    @Mock
    private InitiatorTypeRepository initiatorTypeRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private InitiatorStatusRepository initiatorStatusRepository;
    @Mock
    private InitiatorCategoryRepository initiatorCategoryRepository;
    @Mock
    private SecurityPvvService securityPvvService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TicketService ticketService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private LoggerService loggerService;
    @Mock
    private KafkaSender kafkaSender;
    @Mock
    private MozProperties mozProperties;
    @Captor
    private ArgumentCaptor<Device> deviceCaptor;
    @Captor
    private ArgumentCaptor<MozLinkTagData> mozLinkTagDataCaptor;
    @Captor
    private ArgumentCaptor<Initiator> initiatorCaptor;
    @InjectMocks
    private Me60MozServiceImpl service;

    @Test
    void testRegisterDevice() {
        MozAutoRegisterDeviceRequest request = new MozAutoRegisterDeviceRequest().setSerialNumber(SERIAL);
        request.getMetaData().putAll(Map.of("productNumber", "product1", "model", "model1", "bootVersion", "v1",
                "cpuType", "cpu1", "rfidVersion", "rfid1", "osVersion", "os1", "imei", "imei1", "imsi", "imsi1"));
        when(deviceRepository.findBySerial(SERIAL)).thenReturn(Optional.empty());
        when(deviceRepository.save(deviceCaptor.capture())).thenAnswer(invocation -> {
            ((Device) invocation.getArgument(0)).setId(DEVICE_ID);
            return invocation.getArguments()[0];
        });
        service.registerDevice(request);
        Device actualDevice = deviceCaptor.getValue();
        assertThat(actualDevice.getSerial()).isEqualTo(SERIAL);
        assertThat(actualDevice.getAccountId()).isNull();
        assertThat(actualDevice.getStatus()).isEqualTo(DeviceStatus.INACTIVE);
        assertThat(actualDevice.getCreatedDate()).isNotNull();
        assertThat(actualDevice.getModifiedDate()).isNotNull();
    }

    @Test
    void testRegisterDeviceExists() {
        MozAutoRegisterDeviceRequest request = new MozAutoRegisterDeviceRequest().setSerialNumber(SERIAL);
        request.getMetaData().putAll(Map.of("productNumber", "product1", "model", "model1", "bootVersion", "v1",
                "cpuType", "cpu1", "rfidVersion", "rfid1", "osVersion", "os1", "imei", "imei1", "imsi", "imsi1"));
        String deviceCode = "QQQQ";

        when(deviceRepository.findBySerial(SERIAL)).thenReturn(Optional.of(new Device().setCode(deviceCode)));
        String actualCode = service.registerDevice(request);
        assertThat(actualCode).isEqualTo(deviceCode);
    }

    @Test
    void testLinkTag() {
        LinkNfcTagRequest request = new LinkNfcTagRequest().setDeviceSerial(SERIAL).setAccountNumber(ACCOUNT_NUMBER).setDateTime(Tool.currentDateTime());

        when(mozProperties.isLinkTagValidateDevice()).thenReturn(true);
        when(deviceRepository.findBySerial(SERIAL)).thenReturn(Optional.of(new Device().setStatus(DeviceStatus.ACTIVE)));
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(List.of(new Account().setId(ACCOUNT_ID).setAccountTypeId(PREPAID_ACCOUNT_TYPE_ID).setEntityId(ENTITY_ID)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(new EntityClass().setId(ENTITY_ID).setFirstName(FIRST_NAME).setLastName(LAST_NAME)));
        when(entityMsisdnRepository.findByEntityIdAndPrimaryMsisdn(ENTITY_ID)).thenReturn(Optional.of(new EntityMsisdn().setMsisdn(MSISDN)));
        when(initiatorTypeRepository.findByDescription(TAG)).thenReturn(Optional.of(new InitiatorType().setId(INITIATOR_TYPE_ID)));
        when(initiatorCategoryRepository.findByCategory("MZ Transport")).thenReturn(Optional.of(new InitiatorCategory().setId(INITIATOR_CATEGORY_ID)));
        when(initiatorStatusRepository.findByName("Active")).thenReturn(Optional.of(new InitiatorStatus().setId(ACTIVE_INITIATOR_STATUS_ID)));
        when(initiatorStatusRepository.findByName("Unassigned")).thenReturn(Optional.of(new InitiatorStatus().setId(UNASSIGNED_INITIATOR_STATUS_ID)));
        when(accountTypeRepository.findById(PREPAID_ACCOUNT_TYPE_ID)).thenReturn(Optional.of(new AccountType().setName(AccountType.PREPAID_TRANSPORT).setCurrencyId(CURRENCY_ID)));
        when(accountTypeRepository.findByNameAndCurrencyId(SUBSIDY_ACCOUNT, CURRENCY_ID)).thenReturn(Optional.of(new AccountType().setId(SUBSIDY_ACCOUNT_TYPE_ID)));
        when(accountRepository.findByEntityIdAndAccountTypeId(ENTITY_ID, SUBSIDY_ACCOUNT_TYPE_ID)).thenReturn(Optional.of(new Account().setId(SUBSIDY_ACCOUNT_ID)));
        when(mozProperties.getLinkTagOtpDigitsAmount()).thenReturn(4);
        when(mozProperties.getLinkTagDataCollection()).thenReturn(COLLECTION);
        when(mongoTemplate.save(mozLinkTagDataCaptor.capture(), eq(COLLECTION))).thenAnswer(invocation -> {
            ((MozLinkTagData) invocation.getArgument(0)).setRequestId(REQUEST_ID);
            return invocation.getArguments()[0];
        });

        String actualResponse = service.linkTag(request);
        assertThat(actualResponse).isEqualTo(REQUEST_ID);
        MozLinkTagData actualMozLinkTagData = mozLinkTagDataCaptor.getValue();
        assertThat(actualMozLinkTagData.getRequestId()).isEqualTo(REQUEST_ID);
        assertThat(actualMozLinkTagData.getDevice()).isEqualTo(SERIAL);
        assertThat(actualMozLinkTagData.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
        assertThat(actualMozLinkTagData.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(actualMozLinkTagData.getSubsidyAccountId()).isEqualTo(SUBSIDY_ACCOUNT_ID);
        assertThat(actualMozLinkTagData.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(actualMozLinkTagData.getLastName()).isEqualTo(LAST_NAME);
        assertThat(actualMozLinkTagData.getCreatedDate()).isNotNull();
    }

    @Test
    void testLinkTagValidateOtp() {
        LinkNfcTagRequest request = new LinkNfcTagRequest().setRequestId(REQUEST_ID).setOtp(OTP);

        when(mozProperties.getLinkTagDataCollection()).thenReturn(COLLECTION);
        when(mongoTemplate.findOne(any(), eq(MozLinkTagData.class), eq(COLLECTION))).thenReturn(new MozLinkTagData()
                .setOtpKey(OTP_KEY).setOtpPvv(PVV).setRequestId(REQUEST_ID).setAccountId(ACCOUNT_ID)
                .setSubsidyAccountId(SUBSIDY_ACCOUNT_ID).setFirstName(FIRST_NAME).setLastName(LAST_NAME));
        when(securityPvvService.acquirePvv(OTP_KEY, OTP)).thenReturn(PVV);
        when(accountBalanceRepository.findByAccountIdIn(List.of(ACCOUNT_ID, SUBSIDY_ACCOUNT_ID))).thenReturn(List.of(
                new AccountBalance().setAccountId(ACCOUNT_ID).setBalance(PREPAID_ACCOUNT_BALANCE),
                new AccountBalance().setAccountId(SUBSIDY_ACCOUNT_ID).setBalance(SUBSIDY_ACCOUNT_BALANCE)));

        TagLinkResponse actualResponse = service.linkTagValidateOtp(request);
        assertThat(actualResponse).isEqualTo(new TagLinkResponse()
                .setPrepaidBalance(PREPAID_ACCOUNT_BALANCE).setSubsidyBalance(SUBSIDY_ACCOUNT_BALANCE)
                .setFirstName(FIRST_NAME).setLastName(LAST_NAME));
        verify(mongoTemplate).save(mozLinkTagDataCaptor.capture());
        MozLinkTagData actualMozLinkTagData = mozLinkTagDataCaptor.getValue();
        assertThat(actualMozLinkTagData.isOtpValidated()).isTrue();
    }

    @Test
    void testLinkTagRegister() {
        LinkNfcTagRequest request = new LinkNfcTagRequest().setRequestId(REQUEST_ID).setTagNumber(TEST_TAG);

        when(mozProperties.getLinkTagDataCollection()).thenReturn(COLLECTION);
        when(mongoTemplate.findOne(any(), eq(MozLinkTagData.class), eq(COLLECTION))).thenReturn(new MozLinkTagData().setOtpValidated(true)
                .setInitiatorTypeId(INITIATOR_TYPE_ID).setInitiatorCategoryId(INITIATOR_CATEGORY_ID).setAccountId(ACCOUNT_ID)
                .setActiveInitiatorStatusId(ACTIVE_INITIATOR_STATUS_ID).setUnassignedInitiatorStatusId(UNASSIGNED_INITIATOR_STATUS_ID));
        when(initiatorRepository.findByIdentifier(TEST_TAG)).thenReturn(Optional.of(new Initiator().setId(INITIATOR_ID).setInitiatorStatusId(UNASSIGNED_INITIATOR_STATUS_ID)));
        when(initiatorRepository.save(initiatorCaptor.capture())).thenAnswer(invocation -> invocation.getArguments()[0]);

        Initiator actualResponse = service.linkTagRegister(request);
        assertThat(actualResponse.getId()).isEqualTo(INITIATOR_ID);
        Initiator actualInitiator = initiatorCaptor.getValue();
        assertThat(actualInitiator.getIdentifier()).isEqualTo(TEST_TAG);
        assertThat(actualInitiator.getInitiatorTypeId()).isEqualTo(INITIATOR_TYPE_ID);
        assertThat(actualInitiator.getInitiatorCategoryId()).isEqualTo(INITIATOR_CATEGORY_ID);
        assertThat(actualInitiator.getInitiatorStatusId()).isEqualTo(ACTIVE_INITIATOR_STATUS_ID);
        assertThat(actualInitiator.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(actualInitiator.getCreatedDate()).isNotNull();
        assertThat(actualInitiator.getStartDate()).isNotNull();
    }

    @Test
    void testGetAccountInfo() {
        when(deviceRepository.findBySerial(SERIAL)).thenReturn(Optional.of(new Device().setStatus(DeviceStatus.ACTIVE).setAccountId(ACCOUNT_ID)));
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(new Account().setId(ACCOUNT_ID).setAccountTypeId(ACCOUNT_TYPE_ID)
                .setAccountNumber(ACCOUNT_NUMBER).setEntityId(ENTITY_ID)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(new EntityClass().setFirstName(FIRST_NAME).setLastName(LAST_NAME)));
        when(accountTypeRepository.findById(ACCOUNT_TYPE_ID)).thenReturn(Optional.of(new AccountType().setDescription(ACCOUNT_TYPE_DESCRIPTION)));

        MozAccountInfoResponse accountInfo = service.getAccountInfo(SERIAL);
        assertThat(accountInfo.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(accountInfo.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
        assertThat(accountInfo.getAccountType()).isEqualTo(ACCOUNT_TYPE_DESCRIPTION);
        assertThat(accountInfo.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(accountInfo.getLastName()).isEqualTo(LAST_NAME);
        assertThat(accountInfo.getDeviceStatus()).isEqualTo("ACTIVE");
    }
}
package cash.ice.api.service.impl;

import cash.ice.api.config.property.MozProperties;
import cash.ice.api.dto.OtpType;
import cash.ice.api.dto.moz.LinkNfcTagRequest;
import cash.ice.api.dto.moz.TagInfoMoz;
import cash.ice.api.service.EntityMozService;
import cash.ice.api.service.OtpService;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.entity.moz.Device;
import cash.ice.sqldb.entity.moz.DeviceStatus;
import cash.ice.sqldb.entity.moz.Vehicle;
import cash.ice.sqldb.repository.*;
import cash.ice.sqldb.repository.moz.DeviceRepository;
import cash.ice.sqldb.repository.moz.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static cash.ice.sqldb.entity.AccountType.PREPAID_TRANSPORT;
import static cash.ice.sqldb.entity.InitiatorType.TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceLinkMozServiceImplTest {
    private static final String POS_DEVICE_SERIAL = "serial1";
    private static final int ENTITY_ID = 1;
    private static final String OTP = "1234";
    private static final int PRIMARY_ACCOUNT_ID = 4;
    private static final int VEHICLE_ID = 5;
    private static final int PREPAID_ACCOUNT_ID = 3;
    private static final int PREPAID_ACCOUNT_TYPE_ID = 4;
    private static final int INITIATOR_TYPE_ID = 6;
    private static final int ACTIVE_INITIATOR_STATUS_ID = 7;
    private static final int UNASSIGNED_INITIATOR_STATUS_ID = 8;
    private static final int INITIATOR_CATEGORY_ID = 9;
    private static final String DEVICE_SERIAL = "device1";
    private static final String ACCOUNT_NUMBER = "31234567890";
    private static final String TAG_NUMBER = "tag1";

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountTypeRepository accountTypeRepository;
    @Mock
    private InitiatorTypeRepository initiatorTypeRepository;
    @Mock
    private InitiatorCategoryRepository initiatorCategoryRepository;
    @Mock
    private InitiatorStatusRepository initiatorStatusRepository;
    @Mock
    private InitiatorRepository initiatorRepository;
    @Mock
    private EntityMozService entityMozService;
    @Mock
    private OtpService otpService;
    @Mock
    private MozProperties mozProperties;
    @InjectMocks
    private DeviceLinkMozServiceImpl service;

    @Test
    void testLinkPosDevice() {
        EntityClass entity = new EntityClass().setId(ENTITY_ID);

        when(mozProperties.isLinkPosCheckOtp()).thenReturn(true);
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(entity));
        when(deviceRepository.findBySerial(POS_DEVICE_SERIAL)).thenReturn(Optional.of(new Device()));
        when(entityMozService.getAccount(entity, AccountType.PRIMARY_ACCOUNT, Currency.MZN)).thenReturn(new Account().setId(PRIMARY_ACCOUNT_ID));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

        Device actualDevice = service.linkPosDevice(POS_DEVICE_SERIAL, ENTITY_ID, OTP);
        assertThat(actualDevice.getAccountId()).isEqualTo(PRIMARY_ACCOUNT_ID);
        verify(otpService).validateOtp(OtpType.MOZ_POS_LINK, ENTITY_ID, OTP);
    }

    @Test
    void testLinkPosDeviceToVehicle() {
        when(deviceRepository.findBySerial(POS_DEVICE_SERIAL)).thenReturn(Optional.of(new Device()));
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(new Vehicle().setId(VEHICLE_ID).setAccountId(PRIMARY_ACCOUNT_ID)));
        when(accountRepository.findById(PRIMARY_ACCOUNT_ID)).thenReturn(Optional.of(new Account().setId(PRIMARY_ACCOUNT_ID).setEntityId(ENTITY_ID)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(new EntityClass().setId(ENTITY_ID)));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

        Device actualDevice = service.linkPosDeviceToVehicle(POS_DEVICE_SERIAL, ENTITY_ID, VEHICLE_ID);
        assertThat(actualDevice.getVehicleId()).isEqualTo(VEHICLE_ID);
        assertThat(actualDevice.getStatus()).isEqualTo(DeviceStatus.ACTIVE);
    }

    @Test
    void testLinkNfcTag() {
        LinkNfcTagRequest request = new LinkNfcTagRequest().setDevice(DEVICE_SERIAL).setAccountNumber(ACCOUNT_NUMBER).setTagNumber(TAG_NUMBER);
        Initiator initiator = new Initiator().setInitiatorStatusId(UNASSIGNED_INITIATOR_STATUS_ID);
        TagInfoMoz tagInfo = new TagInfoMoz().setAccountNumber(ACCOUNT_NUMBER);

        when(mozProperties.isLinkTagValidateDevice()).thenReturn(true);
        when(deviceRepository.findBySerial(DEVICE_SERIAL)).thenReturn(Optional.of(new Device().setStatus(DeviceStatus.ACTIVE)));
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(List.of(new Account().setId(PREPAID_ACCOUNT_ID)
                .setAccountTypeId(PREPAID_ACCOUNT_TYPE_ID).setEntityId(ENTITY_ID)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(new EntityClass().setId(ENTITY_ID)));
        when(mozProperties.isLinkTagCheckOtp()).thenReturn(true);
        when(initiatorTypeRepository.findByDescription(TAG)).thenReturn(Optional.of(new InitiatorType().setId(INITIATOR_TYPE_ID)));
        when(initiatorStatusRepository.findByName("Active")).thenReturn(Optional.of(new InitiatorStatus().setId(ACTIVE_INITIATOR_STATUS_ID)));
        when(initiatorStatusRepository.findByName("Unassigned")).thenReturn(Optional.of(new InitiatorStatus().setId(UNASSIGNED_INITIATOR_STATUS_ID)));
        when(initiatorCategoryRepository.findByCategory("MZ Transport")).thenReturn(Optional.of(new InitiatorCategory().setId(INITIATOR_CATEGORY_ID)));
        when(accountTypeRepository.findById(PREPAID_ACCOUNT_TYPE_ID)).thenReturn(Optional.of(new AccountType().setId(PREPAID_ACCOUNT_TYPE_ID).setName(PREPAID_TRANSPORT)));
        when(initiatorRepository.findByIdentifier(TAG_NUMBER)).thenReturn(Optional.of(initiator));
        when(initiatorRepository.save(any(Initiator.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
        when(entityMozService.getTagInfo(TAG_NUMBER)).thenReturn(tagInfo);

        TagInfoMoz actualTagInfo = service.linkNfcTag(request, OTP);
        assertThat(actualTagInfo).isEqualTo(tagInfo);
        verify(otpService).validateOtp(OtpType.MOZ_TAG_LINK, ENTITY_ID, OTP);
    }
}
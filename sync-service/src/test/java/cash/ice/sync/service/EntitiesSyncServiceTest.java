package cash.ice.sync.service;

import cash.ice.common.utils.Tool;
import cash.ice.sqldb.entity.*;
import cash.ice.sqldb.repository.*;
import cash.ice.sync.component.DateTimeParser;
import cash.ice.sync.dto.ChangeAction;
import cash.ice.sync.dto.DataChange;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntitiesSyncServiceTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int ENTITY_TYPE_ID = 15;
    private static final int ENTITY_ID_TYPE = 1;

    @Mock
    private EntityTypeGroupsSyncService entityTypeGroupsSyncService;
    @Mock
    private EntityTypesSyncService entityTypesSyncService;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private EntityIdTypeRepository entityIdTypeRepository;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private EntityMsisdnRepository entityMsisdnRepository;
    @Mock
    private EntityTypeRepository entityTypeRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Captor
    private ArgumentCaptor<EntityClass> entityCaptor;
    @Captor
    private ArgumentCaptor<Address> addressCaptor;
    @Captor
    private ArgumentCaptor<EntityMsisdn> msisdnCaptor;

    private EntitiesSyncService service;

    @BeforeEach
    void init() {
        service = new EntitiesSyncService(entityTypeGroupsSyncService, entityTypesSyncService, new DateTimeParser(),
                countryRepository, entityIdTypeRepository, entityRepository, addressRepository, entityMsisdnRepository,
                entityTypeRepository, jdbcTemplate);
    }

    @Test
    void testEntityCreate() throws URISyntaxException, IOException {
        DataChange dataChange = objectMapper.readValue(
                Tool.readResourceAsString("update/json/entityRequest.json"), DataChange.class);
        when(entityIdTypeRepository.findByDescription("South African National ID")).thenReturn(
                Optional.of(new EntityIdType().setId(ENTITY_ID_TYPE)));
        when(entityTypeRepository.findByDescription("Farmer")).thenReturn(
                Optional.of(new EntityType().setId(ENTITY_TYPE_ID)));
        when(countryRepository.findByIsoCode("ZIM")).thenReturn(Optional.of(new Country().setId(1)));
        when(countryRepository.findByIsoCode("RSA")).thenReturn(Optional.of(new Country().setId(2)));
        when(entityRepository.save(any(EntityClass.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

        service.update(dataChange);
        checkEntity();
        checkAddress();
        checkMsisdns();
    }

    private void checkEntity() {
        verify(entityRepository).save(entityCaptor.capture());
        EntityClass actualEntity = entityCaptor.getValue();
        assertThat(actualEntity.getLegacyAccountId()).isEqualTo(200);
        assertThat(actualEntity.getInternalId()).isEqualTo("23456789");
        assertThat(actualEntity.getPvv()).isEqualTo("8D1584B02A9AE020");
        assertThat(actualEntity.getIdNumber()).isEqualTo("12345678");
        assertThat(actualEntity.getIdType()).isEqualTo(ENTITY_ID_TYPE);
        assertThat(actualEntity.getEntityTypeId()).isEqualTo(ENTITY_TYPE_ID);
        assertThat(actualEntity.getCreatedDate().toString()).isEqualTo("2018-04-16T15:43:45.123");
        assertThat(actualEntity.getStatus()).isEqualTo(EntityStatus.ACTIVE);
        assertThat(actualEntity.getFirstName()).isEqualTo("First1");
        assertThat(actualEntity.getLastName()).isEqualTo("Last1");
        assertThat(actualEntity.getCitizenshipCountryId()).isEqualTo(2);
        assertThat(actualEntity.getBirthDate()).isEqualTo("1998-03-20");
        assertThat(actualEntity.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(actualEntity.getEmail()).isEqualTo("test@test.com");
        assertThat(actualEntity.getKycStatusId()).isEqualTo(1);
    }

    private void checkAddress() {
        verify(addressRepository).save(addressCaptor.capture());
        Address actualAddress = addressCaptor.getValue();
        assertThat(actualAddress.getAddressType()).isEqualTo(AddressType.PRIMARY);
        assertThat(actualAddress.getCountryId()).isEqualTo(1);
        assertThat(actualAddress.getCity()).isEqualTo("Some city");
        assertThat(actualAddress.getPostalCode()).isEqualTo("123456");
        assertThat(actualAddress.getAddress1()).isEqualTo("Some address1");
        assertThat(actualAddress.getAddress2()).isEqualTo("Some address2");
    }

    private void checkMsisdns() {
        EntityClass actualEntity = entityCaptor.getValue();
        verify(entityMsisdnRepository, times(2)).save(msisdnCaptor.capture());
        List<EntityMsisdn> actualMsisdnList = msisdnCaptor.getAllValues();
        EntityMsisdn actualMsisdn = actualMsisdnList.get(0);
        assertThat(actualMsisdn.getEntityId()).isEqualTo(actualEntity.getId());
        assertThat(actualMsisdn.getMsisdnType()).isEqualTo(MsisdnType.PRIMARY);
        assertThat(actualMsisdn.getMsisdn()).isEqualTo("+2631234567");
        assertThat(actualMsisdn.getDescription()).isEqualTo("+2631234567");

        EntityMsisdn actualMsisdn2 = actualMsisdnList.get(1);
        assertThat(actualMsisdn2.getEntityId()).isEqualTo(actualEntity.getId());
        assertThat(actualMsisdn2.getMsisdnType()).isEqualTo(MsisdnType.SECONDARY);
        assertThat(actualMsisdn2.getMsisdn()).isEqualTo("+2631234568");
        assertThat(actualMsisdn2.getDescription()).isEqualTo("Msisdn description");
    }

    @Test
    void testInitiatorStatusUpdate() throws URISyntaxException, IOException {
        Integer entityId = 100;
        DataChange dataChange = objectMapper.readValue(
                Tool.readResourceAsString("update/json/entityRequest.json"), DataChange.class);
        int legacyEntityId = Integer.parseInt(dataChange.getIdentifier());
        EntityClass entity = new EntityClass().setId(entityId).setLegacyAccountId(legacyEntityId);
        when(entityRepository.findByLegacyAccountId(legacyEntityId)).thenReturn(Optional.of(entity));
        when(entityIdTypeRepository.findByDescription("South African National ID")).thenReturn(
                Optional.of(new EntityIdType().setId(ENTITY_ID_TYPE)));
        when(entityTypeRepository.findByDescription("Farmer")).thenReturn(
                Optional.of(new EntityType().setId(ENTITY_TYPE_ID)));
        when(countryRepository.findByIsoCode("ZIM")).thenReturn(Optional.of(new Country().setId(1)));
        when(countryRepository.findByIsoCode("RSA")).thenReturn(Optional.of(new Country().setId(2)));
        when(addressRepository.findByEntityIdAndAddressType(entityId, AddressType.PRIMARY)).thenReturn(
                Optional.of(new Address().setAddressType(AddressType.PRIMARY).setCountryId(1)));
        when(entityMsisdnRepository.findByEntityIdAndPrimaryMsisdn(entityId)).thenReturn(
                Optional.of(new EntityMsisdn().setEntityId(entityId).setMsisdnType(MsisdnType.PRIMARY)));
        when(entityMsisdnRepository.findByEntityIdAndMsisdnType(entityId, MsisdnType.SECONDARY)).thenReturn(
                List.of(new EntityMsisdn().setEntityId(entityId).setMsisdnType(MsisdnType.SECONDARY)));
        when(entityRepository.save(entity)).thenReturn(entity);

        service.update(dataChange);
        checkEntity();
        checkAddress();
        checkMsisdns();
    }

    @Test
    void testInitiatorStatusDelete() {
        DataChange dataChange = new DataChange().setAction(ChangeAction.DELETE).setIdentifier("200");
        EntityClass entity = new EntityClass().setLegacyAccountId(200);
        when(entityRepository.findByLegacyAccountId(200)).thenReturn(Optional.of(entity));
        service.update(dataChange);
        verify(entityRepository).delete(entity);
    }
}
package cash.ice.api.service.impl;

import cash.ice.api.dto.backoffice.EntitiesSearchCriteria;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.EntityMsisdn;
import cash.ice.sqldb.entity.Initiator;
import cash.ice.sqldb.repository.AccountRepository;
import cash.ice.sqldb.repository.EntityMsisdnRepository;
import cash.ice.sqldb.repository.EntityRepository;
import cash.ice.sqldb.repository.InitiatorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntitySearchServiceImplTest {
    private static final int ENTITY_ID = 2;
    private static final int ACCOUNT_ID = 3;

    @Mock
    private EntityRepository entityRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private EntityMsisdnRepository entityMsisdnRepository;
    @Mock
    private InitiatorRepository initiatorRepository;
    @InjectMocks
    private EntitySearchServiceImpl service;

    @Test
    void testSearchEntitiesExactByEntityId() {
        EntityClass entity = new EntityClass();

        when(entityRepository.findById(1)).thenReturn(Optional.of(entity));
        Page<EntityClass> actualResult = service.searchEntities(
                EntitiesSearchCriteria.ENTITY_ID, "1", true, 0, 30, null);
        assertThat(actualResult.getContent()).asList().isNotNull().hasSize(1).containsExactly(entity);
    }

    @Test
    void testSearchEntitiesPartialByEntityId() {
        EntityClass entity = new EntityClass();

        when(entityRepository.findPartialById("1", PageRequest.of(0, 30))).thenReturn(page(List.of(entity)));
        Page<EntityClass> actualResult = service.searchEntities(
                EntitiesSearchCriteria.ENTITY_ID, "1", false, 0, 30, null);
        assertThat(actualResult.getContent()).asList().isNotNull().hasSize(1).containsExactly(entity);
    }

    @Test
    void testSearchEntitiesExactByAccountNumber() {
        String number = "1234";
        EntityClass entity = new EntityClass().setId(ENTITY_ID);

        when(accountRepository.findByAccountNumber(number)).thenReturn(List.of(
                new Account().setAccountNumber(number).setEntityId(ENTITY_ID)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(entity));
        Page<EntityClass> actualResult = service.searchEntities(
                EntitiesSearchCriteria.ACCOUNT_NUMBER, number, true, 0, 30, null);
        assertThat(actualResult.getContent()).asList().isNotNull().hasSize(1).containsExactly(entity);
    }

    @Test
    void testSearchEntitiesPartialByAccountNumber() {
        String number = "1234";
        EntityClass entity = new EntityClass().setId(ENTITY_ID);

        when(accountRepository.findPartialByAccountNumber(number, PageRequest.of(0, 30)))
                .thenReturn(page(List.of(new Account().setAccountNumber(number).setEntityId(ENTITY_ID))));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(entity));
        Page<EntityClass> actualResult = service.searchEntities(
                EntitiesSearchCriteria.ACCOUNT_NUMBER, number, false, 0, 30, null);
        assertThat(actualResult.getContent()).asList().isNotNull().hasSize(1).containsExactly(entity);
    }

    @Test
    void testSearchEntitiesExactByNames() {
        String firstName = "first";
        String lastName = "last";
        EntityClass entity = new EntityClass();

        when(entityRepository.findByFirstNameAndLastName(firstName, lastName, PageRequest.of(0, 30))).thenReturn(page(List.of(entity)));
        Page<EntityClass> actualResult = service.searchEntities(
                EntitiesSearchCriteria.NAMES, firstName + " " + lastName, true, 0, 30, null);
        assertThat(actualResult.getContent()).asList().isNotNull().hasSize(1).containsExactly(entity);
    }

    @Test
    void testSearchEntitiesPartialByNames() {
        String firstName = "first";
        String lastName = "last";
        EntityClass entity = new EntityClass();

        when(entityRepository.findPartialByFirstNameAndLastName(firstName, lastName, PageRequest.of(0, 30))).thenReturn(page(List.of(entity)));
        Page<EntityClass> actualResult = service.searchEntities(
                EntitiesSearchCriteria.NAMES, firstName + " " + lastName, false, 0, 30, null);
        assertThat(actualResult.getContent()).asList().isNotNull().hasSize(1).containsExactly(entity);
    }

    @Test
    void testSearchEntitiesExactByInitiator() {
        String initiatorIdentifier = "3456";
        Initiator initiator = new Initiator().setIdentifier(initiatorIdentifier).setAccountId(ACCOUNT_ID);
        EntityClass entity = new EntityClass().setId(2);

        when(initiatorRepository.findByIdentifier(initiatorIdentifier)).thenReturn(Optional.of(initiator));
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(new Account().setId(ACCOUNT_ID).setEntityId(ENTITY_ID)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(entity));
        Page<EntityClass> actualResult = service.searchEntities(
                EntitiesSearchCriteria.INITIATOR_NUMBER, initiatorIdentifier, true, 0, 30, null);
        assertThat(actualResult.getContent()).asList().isNotNull().hasSize(1).containsExactly(entity);
    }

    @Test
    void testSearchEntitiesPartialByInitiator() {
        String initiatorIdentifier = "3456";
        Initiator initiator = new Initiator().setIdentifier(initiatorIdentifier).setAccountId(ACCOUNT_ID);
        EntityClass entity = new EntityClass().setId(2);

        when(initiatorRepository.findPartialByIdentifier(initiatorIdentifier, PageRequest.of(0, 30)))
                .thenReturn(page(List.of(new Initiator(), initiator)));
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(new Account().setId(ACCOUNT_ID).setEntityId(ENTITY_ID)));
        when(entityRepository.findById(ENTITY_ID)).thenReturn(Optional.of(entity));
        Page<EntityClass> actualResult = service.searchEntities(
                EntitiesSearchCriteria.INITIATOR_NUMBER, initiatorIdentifier, false, 0, 30, null);
        assertThat(actualResult.getContent()).asList().isNotNull().hasSize(1).containsExactly(entity);
    }

    @Test
    void testSearchEntitiesExactByIdNumber() {
        String idNumber = "2345";
        EntityClass entity = new EntityClass().setIdNumber(idNumber);

        when(entityRepository.findByIdNumber(idNumber)).thenReturn(List.of(entity));
        Page<EntityClass> actualResult = service.searchEntities(
                EntitiesSearchCriteria.ID_NUMBER, idNumber, true, 0, 30, null);
        assertThat(actualResult.getContent()).asList().isNotNull().hasSize(1).containsExactly(entity);
    }

    @Test
    void testSearchEntitiesPartialByIdNumber() {
        String idNumber = "2345";
        EntityClass entity = new EntityClass().setIdNumber(idNumber);

        when(entityRepository.findPartialByIdNumber(idNumber, PageRequest.of(0, 30))).thenReturn(page(List.of(entity)));
        Page<EntityClass> actualResult = service.searchEntities(
                EntitiesSearchCriteria.ID_NUMBER, idNumber, false, 0, 30, null);
        assertThat(actualResult.getContent()).asList().isNotNull().hasSize(1).containsExactly(entity);
    }

    @Test
    void testSearchEntitiesExactByMobileNumber() {
        String mobileNumber = "123456789";
        int entityId = 4;
        EntityMsisdn msisdn = new EntityMsisdn().setMsisdn(mobileNumber).setEntityId(entityId);
        EntityClass entity = new EntityClass().setId(5);

        when(entityMsisdnRepository.findByMsisdn(mobileNumber)).thenReturn(List.of(msisdn));
        when(entityRepository.findById(entityId)).thenReturn(Optional.of(entity));
        Page<EntityClass> actualResult = service.searchEntities(
                EntitiesSearchCriteria.MOBILE_NUMBER, mobileNumber, true, 0, 30, null);
        assertThat(actualResult.getContent()).asList().isNotNull().hasSize(1).containsExactly(entity);
    }

    @Test
    void testSearchEntitiesPartialByMobileNumber() {
        String mobileNumber = "123456789";
        int entityId = 4;
        EntityMsisdn msisdn = new EntityMsisdn().setMsisdn(mobileNumber).setEntityId(entityId);
        EntityClass entity = new EntityClass().setId(5);

        when(entityMsisdnRepository.findPartialByMsisdn(mobileNumber, PageRequest.of(0, 30))).thenReturn(page(List.of(new EntityMsisdn(), msisdn)));
        when(entityRepository.findById(entityId)).thenReturn(Optional.of(entity));
        Page<EntityClass> actualResult = service.searchEntities(
                EntitiesSearchCriteria.MOBILE_NUMBER, mobileNumber, false, 0, 30, null);
        assertThat(actualResult.getContent()).asList().isNotNull().hasSize(1).containsExactly(entity);
    }

    private <T> Page<T> page(List<T> items) {
        return new PageImpl<>(items, PageRequest.of(0, 30), items.size());
    }
}
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitiatorsSyncServiceTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int ACCOUNT_ID = 20;
    private static final int INITIATOR_TYPE_ID = 12;
    private static final int INITIATOR_CATEGORY_ID = 14;
    private static final int INITIATOR_STATUS_ID = 16;
    private static final int ENTITY_ID = 200;
    private static final int ACCOUNT_TYPE_ID = 18;

    @Mock
    private InitiatorCategoriesSyncService initiatorCategoriesSyncService;
    @Mock
    private InitiatorStatusesSyncService initiatorStatusesSyncService;
    @Mock
    private InitiatorTypeRepository initiatorTypeRepository;
    @Mock
    private InitiatorCategoryRepository initiatorCategoryRepository;
    @Mock
    private InitiatorStatusRepository initiatorStatusRepository;
    @Mock
    private InitiatorRepository initiatorRepository;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountTypeRepository accountTypeRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private JdbcTemplate newJdbcTemplate;
    @Captor
    private ArgumentCaptor<Initiator> initiatorCaptor;
    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    private InitiatorsSyncService service;

    @BeforeEach
    void init() {
        service = new InitiatorsSyncService(initiatorCategoriesSyncService, initiatorStatusesSyncService,
                initiatorTypeRepository, initiatorCategoryRepository, initiatorStatusRepository, initiatorRepository,
                entityRepository, accountRepository, accountTypeRepository, jdbcTemplate, newJdbcTemplate, new DateTimeParser());
    }

    @Test
    void testInitiatorCreate() throws URISyntaxException, IOException {
        EntityClass entity = new EntityClass().setId(ENTITY_ID).setStatus(EntityStatus.ACTIVE);
        DataChange dataChange = objectMapper.readValue(
                Tool.readResourceAsString("update/json/initiatorRequest.json"), DataChange.class);
        when(initiatorTypeRepository.findByDescription("card")).thenReturn(
                Optional.of(new InitiatorType().setId(INITIATOR_TYPE_ID)));
        when(initiatorCategoryRepository.findByCategory("ICECash")).thenReturn(
                Optional.of(new InitiatorCategory().setId(INITIATOR_CATEGORY_ID)));
        when(initiatorStatusRepository.findByName("Number Created")).thenReturn(
                Optional.of(new InitiatorStatus().setId(INITIATOR_STATUS_ID)));
        when(entityRepository.findByLegacyAccountId(ENTITY_ID)).thenReturn(Optional.of(entity));
        when(accountTypeRepository.findByLegacyWalletId("1")).thenReturn(
                Optional.of(new AccountType().setId(ACCOUNT_TYPE_ID)));

        service.update(dataChange);
        checkInitiator();
        checkAddress(entity);
    }

    private void checkInitiator() {
        verify(initiatorRepository).save(initiatorCaptor.capture());
        Initiator actualInitiator = initiatorCaptor.getValue();
        assertThat(actualInitiator.getInitiatorTypeId()).isEqualTo(INITIATOR_TYPE_ID);
        assertThat(actualInitiator.getInitiatorCategoryId()).isEqualTo(INITIATOR_CATEGORY_ID);
        assertThat(actualInitiator.getInitiatorStatusId()).isEqualTo(INITIATOR_STATUS_ID);
        assertThat(actualInitiator.getPvv()).isEqualTo("8DB4834CF252E5C8");
        assertThat(actualInitiator.getNotes()).isEqualTo("Some notes");
        assertThat(actualInitiator.getCreatedDate().toString()).isEqualTo("2018-04-16T15:43:45");
    }

    private void checkAddress(EntityClass entity) {
        verify(accountRepository).save(accountCaptor.capture());
        Account actualAccount = accountCaptor.getValue();
        assertThat(actualAccount.getEntityId()).isEqualTo(entity.getId());
        assertThat(actualAccount.getAccountTypeId()).isEqualTo(ACCOUNT_TYPE_ID);
        assertThat(actualAccount.getAccountNumber()).isEqualTo("200001");
        assertThat(actualAccount.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void testInitiatorUpdate() throws URISyntaxException, IOException {
        EntityClass entity = new EntityClass().setId(ENTITY_ID).setStatus(EntityStatus.ACTIVE);
        DataChange dataChange = objectMapper.readValue(
                Tool.readResourceAsString("update/json/initiatorRequest.json"), DataChange.class);
        when(initiatorRepository.findByIdentifier(dataChange.getIdentifier())).thenReturn(
                Optional.of(new Initiator().setIdentifier(dataChange.getIdentifier())));
        when(initiatorTypeRepository.findByDescription("card")).thenReturn(
                Optional.of(new InitiatorType().setId(INITIATOR_TYPE_ID)));
        when(initiatorCategoryRepository.findByCategory("ICECash")).thenReturn(
                Optional.of(new InitiatorCategory().setId(INITIATOR_CATEGORY_ID)));
        when(initiatorStatusRepository.findByName("Number Created")).thenReturn(
                Optional.of(new InitiatorStatus().setId(INITIATOR_STATUS_ID)));
        when(entityRepository.findByLegacyAccountId(ENTITY_ID)).thenReturn(Optional.of(entity));
        when(accountTypeRepository.findByLegacyWalletId("1")).thenReturn(
                Optional.of(new AccountType().setId(ACCOUNT_TYPE_ID)));
        when(accountRepository.findByEntityIdAndAccountTypeId(ENTITY_ID, ACCOUNT_TYPE_ID)).thenReturn(
                Optional.of(new Account().setId(ACCOUNT_ID)));

        service.update(dataChange);
        checkInitiator();
        assertThat(initiatorCaptor.getValue().getAccountId()).isEqualTo(ACCOUNT_ID);
    }

    @Test
    void testInitiatorDelete() {
        DataChange dataChange = new DataChange().setAction(ChangeAction.DELETE).setIdentifier("200");
        Initiator initiator = new Initiator().setId(200);
        when(initiatorRepository.findByIdentifier("200")).thenReturn(Optional.of(initiator));
        service.update(dataChange);
        verify(initiatorRepository).delete(initiator);
    }
}
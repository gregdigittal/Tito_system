package cash.ice.sync.service;

import cash.ice.sqldb.entity.EntityClass;
import cash.ice.sqldb.entity.InitiatorCategory;
import cash.ice.sqldb.repository.EntityRepository;
import cash.ice.sqldb.repository.InitiatorCategoryRepository;
import cash.ice.common.utils.Tool;
import cash.ice.sync.dto.ChangeAction;
import cash.ice.sync.dto.DataChange;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitiatorCategoriesSyncServiceTest {
    private static final int ENTITY_ID = 10;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private EntityRepository entityRepository;
    @Mock
    private InitiatorCategoryRepository initiatorCategoryRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private InitiatorCategoriesSyncService service;

    @BeforeEach
    void init() {
        service = new InitiatorCategoriesSyncService(entityRepository, initiatorCategoryRepository, jdbcTemplate);
    }

    @Test
    void testInitiatorCategoryCreate() throws URISyntaxException, IOException {
        DataChange dataChange = objectMapper.readValue(
                Tool.readResourceAsString("update/json/initiatorCategoryRequest.json"), DataChange.class);
        when(entityRepository.findByLegacyAccountId(100)).thenReturn(Optional.of(new EntityClass().setId(ENTITY_ID)));
        service.update(dataChange);
        verify(initiatorCategoryRepository).save(new InitiatorCategory().setCategory("NewCategory").setEntityId(ENTITY_ID));
    }

    @Test
    void testInitiatorCategoryUpdate() throws URISyntaxException, IOException {
        DataChange dataChange = objectMapper.readValue(
                Tool.readResourceAsString("update/json/initiatorCategoryRequest.json"), DataChange.class);
        when(entityRepository.findByLegacyAccountId(100)).thenReturn(Optional.of(new EntityClass().setId(ENTITY_ID)));
        when(initiatorCategoryRepository.findByCategory("NewCategory")).thenReturn(
                Optional.of(new InitiatorCategory().setCategory("NewCategory")));
        service.update(dataChange);
        verify(initiatorCategoryRepository).save(new InitiatorCategory().setCategory("NewCategory").setEntityId(ENTITY_ID));
    }

    @Test
    void testInitiatorCategoryDelete() {
        DataChange dataChange = new DataChange().setAction(ChangeAction.DELETE).setIdentifier("NewCategory");
        InitiatorCategory initiatorCategory = new InitiatorCategory().setCategory(dataChange.getIdentifier());
        when(initiatorCategoryRepository.findByCategory(dataChange.getIdentifier())).thenReturn(Optional.of(initiatorCategory));
        service.update(dataChange);
        verify(initiatorCategoryRepository).delete(initiatorCategory);
    }
}
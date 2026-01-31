package cash.ice.sync.service;

import cash.ice.sqldb.entity.EntityType;
import cash.ice.sqldb.entity.EntityTypeGroup;
import cash.ice.sqldb.repository.EntityTypeGroupRepository;
import cash.ice.sqldb.repository.EntityTypeRepository;
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
class EntityTypesSyncServiceTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private EntityTypeRepository entityTypeRepository;
    @Mock
    private EntityTypeGroupRepository entityTypeGroupRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private EntityTypesSyncService service;

    @BeforeEach
    void init() {
        service = new EntityTypesSyncService(entityTypeRepository, entityTypeGroupRepository, jdbcTemplate);
    }

    @Test
    void testEntityTypeCreate() throws URISyntaxException, IOException {
        DataChange dataChange = objectMapper.readValue(
                Tool.readResourceAsString("update/json/entityTypeRequest.json"), DataChange.class);
        when(entityTypeGroupRepository.findByDescription("ICEcash")).thenReturn(Optional.of(new EntityTypeGroup().setId(1)));
        service.update(dataChange);
        verify(entityTypeRepository).save(new EntityType().setDescription("NewType").setEntityTypeGroupId(1));
    }

    @Test
    void testEntityTypeDelete() {
        DataChange dataChange = new DataChange().setAction(ChangeAction.DELETE).setIdentifier("NewType");
        EntityType entityType = new EntityType().setDescription(dataChange.getIdentifier());
        when(entityTypeRepository.findByDescription(dataChange.getIdentifier())).thenReturn(Optional.of(entityType));
        service.update(dataChange);
        verify(entityTypeRepository).delete(entityType);
    }
}
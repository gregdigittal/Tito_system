package cash.ice.sync.service;

import cash.ice.sqldb.entity.EntityTypeGroup;
import cash.ice.sqldb.repository.EntityTypeGroupRepository;
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
class EntityTypeGroupsSyncServiceTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private EntityTypeGroupRepository entityTypeGroupRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private EntityTypeGroupsSyncService service;

    @BeforeEach
    void init() {
        service = new EntityTypeGroupsSyncService(entityTypeGroupRepository, jdbcTemplate);
    }

    @Test
    void testEntityTypeGroupCreate() throws URISyntaxException, IOException {
        DataChange dataChange = objectMapper.readValue(
                Tool.readResourceAsString("update/json/entityTypeGroupRequest.json"), DataChange.class);
        service.update(dataChange);
        verify(entityTypeGroupRepository).save(new EntityTypeGroup().setDescription("NewGroup"));
    }

    @Test
    void testEntityTypeGroupDelete() {
        DataChange dataChange = new DataChange().setAction(ChangeAction.DELETE).setIdentifier("AAA");
        EntityTypeGroup entityTypeGroup = new EntityTypeGroup().setDescription(dataChange.getIdentifier());
        when(entityTypeGroupRepository.findByDescription(dataChange.getIdentifier())).thenReturn(Optional.of(entityTypeGroup));
        service.update(dataChange);
        verify(entityTypeGroupRepository).delete(entityTypeGroup);
    }
}
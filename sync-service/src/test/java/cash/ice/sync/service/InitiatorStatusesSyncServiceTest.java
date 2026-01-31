package cash.ice.sync.service;

import cash.ice.sqldb.entity.InitiatorStatus;
import cash.ice.sqldb.repository.InitiatorStatusRepository;
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
class InitiatorStatusesSyncServiceTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private InitiatorStatusRepository initiatorStatusRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private InitiatorStatusesSyncService service;

    @BeforeEach
    void init() {
        service = new InitiatorStatusesSyncService(initiatorStatusRepository, jdbcTemplate);
    }

    @Test
    void testInitiatorStatusCreate() throws URISyntaxException, IOException {
        DataChange dataChange = objectMapper.readValue(
                Tool.readResourceAsString("update/json/initiatorStatusRequest.json"), DataChange.class);
        service.update(dataChange);
        verify(initiatorStatusRepository).save(new InitiatorStatus().setName("NewStatus").setActive(true).setPermitTransaction(true));
    }

    @Test
    void testInitiatorStatusUpdate() throws URISyntaxException, IOException {
        DataChange dataChange = objectMapper.readValue(
                Tool.readResourceAsString("update/json/initiatorStatusRequest.json"), DataChange.class);
        when(initiatorStatusRepository.findByName("NewStatus")).thenReturn(
                Optional.of(new InitiatorStatus().setName("NewStatus")));
        service.update(dataChange);
        verify(initiatorStatusRepository).save(new InitiatorStatus().setName("NewStatus").setActive(true).setPermitTransaction(true));
    }

    @Test
    void testInitiatorStatusDelete() {
        DataChange dataChange = new DataChange().setAction(ChangeAction.DELETE).setIdentifier("NewStatus");
        InitiatorStatus initiatorStatus = new InitiatorStatus().setName(dataChange.getIdentifier());
        when(initiatorStatusRepository.findByName(dataChange.getIdentifier())).thenReturn(Optional.of(initiatorStatus));
        service.update(dataChange);
        verify(initiatorStatusRepository).delete(initiatorStatus);
    }
}
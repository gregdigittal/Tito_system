package cash.ice.sync.service;

import cash.ice.sqldb.entity.Channel;
import cash.ice.sqldb.repository.ChannelRepository;
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
class ChannelsSyncServiceTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private ChannelsSyncService service;

    @BeforeEach
    void init() {
        service = new ChannelsSyncService(jdbcTemplate, channelRepository);
    }

    @Test
    void testChannelCreate() throws URISyntaxException, IOException {
        DataChange dataChange = objectMapper.readValue(
                Tool.readResourceAsString("update/json/channelRequest.json"), DataChange.class);
        service.update(dataChange);
        verify(channelRepository).save(new Channel().setCode("AAA").setDescription("New description"));
    }

    @Test
    void testChannelUpdate() throws URISyntaxException, IOException {
        DataChange dataChange = objectMapper.readValue(
                Tool.readResourceAsString("update/json/channelRequest.json"), DataChange.class);
        when(channelRepository.findByCode(dataChange.getIdentifier())).thenReturn(
                Optional.of(new Channel().setCode(dataChange.getIdentifier()).setDescription("Old description")));
        service.update(dataChange);
        verify(channelRepository).save(new Channel().setCode("AAA").setDescription("New description"));
    }

    @Test
    void testChannelDelete() {
        DataChange dataChange = new DataChange().setAction(ChangeAction.DELETE).setIdentifier("AAA");
        Channel channel = new Channel().setCode(dataChange.getIdentifier());
        when(channelRepository.findByCode(dataChange.getIdentifier())).thenReturn(Optional.of(channel));
        service.update(dataChange);
        verify(channelRepository).delete(channel);
    }
}
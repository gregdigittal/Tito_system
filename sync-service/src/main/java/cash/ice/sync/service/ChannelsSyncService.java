package cash.ice.sync.service;

import cash.ice.sqldb.entity.Channel;
import cash.ice.sqldb.repository.ChannelRepository;
import cash.ice.sync.dto.ChangeAction;
import cash.ice.sync.dto.DataChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChannelsSyncService implements DataMigrator {
    @SuppressWarnings("SqlResolve")
    private static final String MIGRATE_SQL = "select * from dbo.Channels";
    private static final String DESCRIPTION = "Description";

    private final JdbcTemplate jdbcTemplate;
    private final ChannelRepository channelRepository;

    @Transactional
    @Override
    public void migrateData() {
        log.debug("Start migrating Channels");
        List<Channel> channels = jdbcTemplate.query(MIGRATE_SQL, (rs, rowNum) ->
                new Channel()
                        .setCode(rs.getString("Channel"))
                        .setDescription(rs.getString(DESCRIPTION)));
        channelRepository.saveAll(channels);
        log.info("Finished migrating Channels: {} processed, {} total", channels.size(), channelRepository.count());
    }

    public void update(DataChange dataChange) {
        Channel channel = channelRepository.findByCode(dataChange.getIdentifier()).orElse(null);
        if (dataChange.getAction() == ChangeAction.DELETE) {
            if (channel != null) {
                channelRepository.delete(channel);
            } else {
                log.warn("Cannot delete Channel with code: {}, it is absent", dataChange.getIdentifier());
            }
        } else {                // update
            if (channel == null) {
                channel = new Channel().setCode(dataChange.getIdentifier());
            }
            if (dataChange.getData().containsKey(DESCRIPTION)) {
                channel.setDescription((String) dataChange.getData().get(DESCRIPTION));
            }
            channelRepository.save(channel);
        }
    }
}

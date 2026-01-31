package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Integer> {

    Optional<Channel> findByCode(String code);
}
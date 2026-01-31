package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.IpRange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IpRangeRepository extends JpaRepository<IpRange, Integer> {

    List<IpRange> findByActive(boolean active);

    Optional<IpRange> findByAddress(String address);
}

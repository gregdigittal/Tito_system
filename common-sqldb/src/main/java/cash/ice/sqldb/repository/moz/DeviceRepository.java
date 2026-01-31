package cash.ice.sqldb.repository.moz;

import cash.ice.sqldb.entity.moz.Device;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Integer> {

    boolean existsByCode(String code);

    boolean existsBySerial(String serial);

    Optional<Device> findByCode(String code);

    List<Device> findByAccountIdIn(List<Integer> accountIds);

    Optional<Device> findBySerial(String serial);

    Page<Device> findByAccountIdAndVehicleIdIsNull(Integer accountId, Pageable pageable);

    Page<Device> findByAccountIdAndVehicleIdIsNotNull(Integer accountId, Pageable pageable);
}

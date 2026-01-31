package cash.ice.sqldb.repository.moz;

import cash.ice.sqldb.entity.moz.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {

    Page<Vehicle> findByEntityId(Integer entityId, Pageable pageable);
}
package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.InitiatorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InitiatorStatusRepository extends JpaRepository<InitiatorStatus, Integer> {

    Optional<InitiatorStatus> findByName(String name);
}

package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.InitiatorType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InitiatorTypeRepository extends JpaRepository<InitiatorType, Integer> {

    Optional<InitiatorType> findByDescription(String description);
}
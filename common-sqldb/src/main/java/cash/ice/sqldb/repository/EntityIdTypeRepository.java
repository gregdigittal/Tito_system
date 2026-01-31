package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.EntityIdType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntityIdTypeRepository extends JpaRepository<EntityIdType, Integer> {

    Optional<EntityIdType> findByDescription(String description);
}

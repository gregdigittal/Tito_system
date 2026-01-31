package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.EntityTypeGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntityTypeGroupRepository extends JpaRepository<EntityTypeGroup, Integer> {

    Optional<EntityTypeGroup> findByDescription(String description);
}
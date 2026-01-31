package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntityTypeRepository extends JpaRepository<EntityType, Integer> {

    Optional<EntityType> findByDescription(String description);

    List<EntityType> findByEntityTypeGroupIdIn(List<Integer> entityTypeGroups);
}
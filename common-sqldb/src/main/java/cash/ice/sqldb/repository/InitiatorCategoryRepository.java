package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.InitiatorCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InitiatorCategoryRepository extends JpaRepository<InitiatorCategory, Integer> {

    Optional<InitiatorCategory> findByCategory(String category);
}

package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.MetaData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MetaDataRepository extends JpaRepository<MetaData, Integer> {

    Optional<MetaData> findByName(String name);

    boolean existsByName(String name);
}

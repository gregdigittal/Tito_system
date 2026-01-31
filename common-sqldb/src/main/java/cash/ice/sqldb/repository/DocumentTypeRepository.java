package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, Integer> {

    List<DocumentType> findByCountryIdAndActive(Integer countryId, boolean active);

    Optional<DocumentType> findByName(String name);

    Optional<DocumentType> findByNameAndCountryIdAndActive(String name, Integer countryId, boolean active);
}

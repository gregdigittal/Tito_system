package cash.ice.sqldb.repository.zim;

import cash.ice.sqldb.entity.zim.TaxDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaxDeclarationRepository extends JpaRepository<TaxDeclaration, Integer> {

    Optional<TaxDeclaration> findByDescription(String description);
}

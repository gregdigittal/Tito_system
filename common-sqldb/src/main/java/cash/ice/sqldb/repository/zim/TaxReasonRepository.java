package cash.ice.sqldb.repository.zim;

import cash.ice.sqldb.entity.zim.TaxReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaxReasonRepository extends JpaRepository<TaxReason, Integer> {

    Optional<TaxReason> findByDescription(String description);
}

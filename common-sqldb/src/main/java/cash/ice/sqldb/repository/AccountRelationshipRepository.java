package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.AccountRelationship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRelationshipRepository extends JpaRepository<AccountRelationship, Integer> {

    Optional<AccountRelationship> findByEntityIdAndPartnerAccountId(Integer entityId, Integer partnerAccountId);

    List<AccountRelationship> findByEntityIdIn(List<Integer> entityIds);
}
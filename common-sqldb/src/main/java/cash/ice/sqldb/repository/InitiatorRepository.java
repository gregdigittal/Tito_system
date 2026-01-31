package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.Initiator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InitiatorRepository extends JpaRepository<Initiator, Integer> {

    Optional<Initiator> findByIdentifier(String identifier);

    Optional<Initiator> findByIdentifierAndInitiatorTypeId(String identifier, Integer initiatorTypeId);

    @Query(nativeQuery = true, value = "select * from initiator where identifier = :identifier " +
            "union select * from initiator where identifier like concat(:identifier,'%')" +
            "union select * from initiator where identifier like concat('%',:identifier)" +
            "union select * from initiator where identifier like concat('%',:identifier,'%')",
            countQuery = "select count(i.id) from (select * from initiator where identifier = :identifier " +
                    "union select * from initiator where identifier like concat(:identifier,'%')" +
                    "union select * from initiator where identifier like concat('%',:identifier)" +
                    "union select * from initiator where identifier like concat('%',:identifier,'%')) as i")
    Page<Initiator> findPartialByIdentifier(@Param("identifier") String identifier, Pageable pageable);

    List<Initiator> findByAccountIdIn(List<Integer> accountIds);

    Page<Initiator> findByAccountId(Integer accountId, Pageable pageable);
}

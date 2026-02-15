package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.EntityMsisdn;
import cash.ice.sqldb.entity.MsisdnType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EntityMsisdnRepository extends JpaRepository<EntityMsisdn, Integer> {

    boolean existsByMsisdn(String msisdn);

    boolean existsByMsisdnAndMsisdnType(String msisdn, MsisdnType msisdnType);

    @Query("from EntityMsisdn e where e.msisdnType = 'PRIMARY' and e.entityId = :entityId")
    Optional<EntityMsisdn> findByEntityIdAndPrimaryMsisdn(@Param("entityId") Integer entityId);

    List<EntityMsisdn> findByEntityIdAndMsisdnType(Integer entityId, MsisdnType msisdnType);

    List<EntityMsisdn> findByEntityIdIn(List<Integer> entityIds);

    List<EntityMsisdn> findByMsisdn(String msisdn);

    @Query(nativeQuery = true, value = "select * from entity_msisdn where msisdn = :msisdn " +
            "union select * from entity_msisdn where msisdn like concat(:msisdn,'%') escape '\\\\' " +
            "union select * from entity_msisdn where msisdn like concat('%',:msisdn) escape '\\\\' " +
            "union select * from entity_msisdn where msisdn like concat('%',:msisdn,'%') escape '\\\\'",
            countQuery = "select count(m.id) from (select * from entity_msisdn where msisdn = :msisdn " +
                    "union select * from entity_msisdn where msisdn like concat(:msisdn,'%') escape '\\\\' " +
                    "union select * from entity_msisdn where msisdn like concat('%',:msisdn) escape '\\\\' " +
                    "union select * from entity_msisdn where msisdn like concat('%',:msisdn,'%') escape '\\\\') as m")
    Page<EntityMsisdn> findPartialByMsisdn(@Param("msisdn") String msisdn, Pageable pageable);
}

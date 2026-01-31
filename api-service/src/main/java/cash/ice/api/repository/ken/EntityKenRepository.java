package cash.ice.api.repository.ken;

import cash.ice.sqldb.entity.EntityClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EntityKenRepository extends JpaRepository<EntityClass, Integer> {

    @Query("select e from EntityClass e where e.entityTypeId = :entityTypeId and (:idTypeId is null or e.idType = :idTypeId) and (:idNumber is null or e.idNumber like :idNumber||'%') and (e.id in :entityIds) ")
    Page<EntityClass> findByEntityTypeAndMobileAndIdAndEntityIds(@Param("entityTypeId") Integer entityTypeId, @Param("idTypeId") Integer idTypeId, @Param("idNumber") String idNumber, @Param("entityIds") List<Integer> entityIds, Pageable pageable);

    @Query("select e from EntityClass e where e.entityTypeId = :entityTypeId and (:idTypeId is null or e.idType = :idTypeId) and (:idNumber is null or e.idNumber like :idNumber||'%') ")
    Page<EntityClass> findByEntityTypeAndMobileAndId(@Param("entityTypeId") Integer entityTypeId, @Param("idTypeId") Integer idTypeId, @Param("idNumber") String idNumber, Pageable pageable);
}
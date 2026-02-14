package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.EntityClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EntityRepository extends JpaRepository<EntityClass, Integer> {

    boolean existsAccountByFirstName(String firstName);

    boolean existsAccountByInternalId(String internalId);

    boolean existsAccountByEmail(String email);

    boolean existsAccountByIdNumberAndIdType(String idNumber, Integer idType);

    @Query(nativeQuery = true, value = "select * from entity where id = :id " +
            "union select * from entity where id like concat(:id,'%') escape '\\\\' " +
            "union select * from entity where id like concat('%',:id) escape '\\\\' " +
            "union select * from entity where id like concat('%',:id,'%') escape '\\\\'",
            countQuery = "select count(e.id) from (select * from entity where id = :id " +
                    "union select * from entity where id like concat(:id,'%') escape '\\\\' " +
                    "union select * from entity where id like concat('%',:id) escape '\\\\' " +
                    "union select * from entity where id like concat('%',:id,'%') escape '\\\\') as e")
    Page<EntityClass> findPartialById(@Param("id") String id, Pageable pageable);

    List<EntityClass> findByIdNumber(String idNumber);

    List<EntityClass> findByIdNumberAndIdType(String idNumber, Integer idType);

    @Query(nativeQuery = true, value = "select * from entity where id_number = :idNumber " +
            "union select * from entity where id_number like concat(:idNumber,'%') escape '\\\\' " +
            "union select * from entity where id_number like concat('%',:idNumber) escape '\\\\' " +
            "union select * from entity where id_number like concat('%',:idNumber,'%') escape '\\\\'",
            countQuery = "select count(e.id) from (select * from entity where id_number = :idNumber " +
                    "union select * from entity where id_number like concat(:idNumber,'%') escape '\\\\' " +
                    "union select * from entity where id_number like concat('%',:idNumber) escape '\\\\' " +
                    "union select * from entity where id_number like concat('%',:idNumber,'%') escape '\\\\') as e")
    Page<EntityClass> findPartialByIdNumber(@Param("idNumber") String idNumber, Pageable pageable);

    Optional<EntityClass> findByLegacyAccountId(int legacyAccountId);

    Optional<EntityClass> findByKeycloakId(String keycloakId);

    List<EntityClass> findByEntityTypeIdIn(List<Integer> entityTypes);

    Optional<EntityClass> findByEmail(String email);

    Page<EntityClass> findByFirstNameAndLastName(String firstName, String lastName, Pageable pageable);

    List<EntityClass> findByFirstNameIn(List<String> firstName);

    @Query(nativeQuery = true, value = "select * from entity where first_name = :firstName and last_name = :lastName " +
            "union select * from entity where first_name = :firstName or last_name = :firstName or (:lastName != '' and (last_name = :lastName or first_name = :lastName)) " +
            "union select * from entity where concat(coalesce(first_name, ''), ' ', coalesce(last_name, '')) like concat(:firstName,'%',:lastName,'%') escape '\\\\' " +
            "union select * from entity where concat(coalesce(first_name, ''), ' ', coalesce(last_name, '')) like concat('%',:firstName,'%',:lastName) escape '\\\\' " +
            "union select * from entity where concat(coalesce(first_name, ''), ' ', coalesce(last_name, '')) like concat('%',:firstName,'%',:lastName,'%') escape '\\\\' ",
            countQuery = "select count(e.id) from (select * from entity where first_name = :firstName and last_name = :lastName " +
                    "union select * from entity where first_name = :firstName or last_name = :firstName or (:lastName != '' and (last_name = :lastName or first_name = :lastName)) " +
                    "union select * from entity where concat(coalesce(first_name, ''), ' ', coalesce(last_name, '')) like concat(:firstName,'%',:lastName,'%') escape '\\\\' " +
                    "union select * from entity where concat(coalesce(first_name, ''), ' ', coalesce(last_name, '')) like concat('%',:firstName,'%',:lastName) escape '\\\\' " +
                    "union select * from entity where concat(coalesce(first_name, ''), ' ', coalesce(last_name, '')) like concat('%',:firstName,'%',:lastName,'%') escape '\\\\') as e")
    Page<EntityClass> findPartialByFirstNameAndLastName(@Param("firstName") String firstName, @Param("lastName") String lastName, Pageable pageable);
}
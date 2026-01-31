package cash.ice.api.repository.backoffice;

import cash.ice.api.entity.backoffice.StaffMember;
import cash.ice.sqldb.entity.LoginStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffMemberRepository extends JpaRepository<StaffMember, Integer> {

    boolean existsAccountByPinKey(String pinKey);

    Optional<StaffMember> findStaffMemberByEmail(String email);

    Optional<StaffMember> findByKeycloakId(String keycloakId);

    boolean existsStaffMemberByEmail(String email);

    boolean existsStaffMemberByIdNumberTypeAndIdNumber(Integer idNumberType, String idNumber);

    Page<StaffMember> findByLoginStatus(LoginStatus loginStatus, Pageable pageable);

    @Query(nativeQuery = true, value = "select * from staff where (:status is null or login_status = :status) and (id = :number or first_name = :number or last_name = :number or email = :number) " +
            "union select * from staff where (:status is null or login_status = :status) and (id like concat(:number,'%') or first_name like concat(:number,'%')  or last_name like concat(:number,'%') or email like concat(:number,'%')) " +
            "union select * from staff where (:status is null or login_status = :status) and (id like concat('%',:number,'%') or first_name like concat('%',:number,'%')  or last_name like concat('%',:number,'%') or email like concat('%',:number,'%')) ")
    List<StaffMember> findByIdOrNamesOrEmail(@Param("status") String status, @Param("number") String number, Pageable pageable);

    @Query(nativeQuery = true, value = "select * from staff where (:status is null or login_status = :status) and (first_name = :firstWord and last_name = :secondWord or email = :firstWord) " +
            "union select * from staff where (:status is null or login_status = :status) and (first_name = :firstWord or last_name = :firstWord or email like concat(:firstWord,'@%') or (:secondWord != '' and (last_name = :secondWord or first_name = :secondWord or email like concat('%@', :secondWord)))) " +
            "union select * from staff where (:status is null or login_status = :status) and (concat(coalesce(first_name, ''), ' ', coalesce(last_name, '')) like concat(:firstWord,'%',:secondWord,'%') or email like concat(:firstWord,'%',:secondWord,'%')) " +
            "union select * from staff where (:status is null or login_status = :status) and (concat(coalesce(first_name, ''), ' ', coalesce(last_name, '')) like concat('%',:firstWord,'%',:secondWord,'%') or email like concat('%',:firstWord,'%',:secondWord,'%')) ")
    List<StaffMember> findByNamesOrEmail(@Param("status") String status, @Param("firstWord") String firstWord, @Param("secondWord") String secondWord, Pageable pageable);
}
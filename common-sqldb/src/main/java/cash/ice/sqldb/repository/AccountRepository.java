package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {

    boolean existsAccountByAccountNumber(String accountNumber);

    List<Account> findByAccountNumber(String accountNumber);

    List<Account> findByAccountNumberIn(List<String> accountNumbers);

    @Query(nativeQuery = true, value = "select * from account where account_number = :accountNumber " +
            "union select * from account where account_number like concat(:accountNumber,'%') escape '\\\\' " +
            "union select * from account where account_number like concat('%',:accountNumber) escape '\\\\' " +
            "union select * from account where account_number like concat('%',:accountNumber,'%') escape '\\\\'",
            countQuery = "select count(a.id) from (select * from account where account_number = :accountNumber " +
                    "union select * from account where account_number like concat(:accountNumber,'%') escape '\\\\' " +
                    "union select * from account where account_number like concat('%',:accountNumber) escape '\\\\' " +
                    "union select * from account where account_number like concat('%',:accountNumber,'%') escape '\\\\') as a")
    Page<Account> findPartialByAccountNumber(@Param("accountNumber") String accountNumber, Pageable pageable);

    Optional<Account> findByEntityIdAndAccountTypeId(Integer entityId, Integer accountTypeId);

    List<Account> findByEntityId(Integer entityId);

    List<Account> findByEntityId(Integer entityId, Pageable pageable);

    List<Account> findByEntityIdIn(List<Integer> entityId);

    List<Account> findByAccountTypeIdAndEntityIdIn(Integer accountTypeId, List<Integer> entityId);
}
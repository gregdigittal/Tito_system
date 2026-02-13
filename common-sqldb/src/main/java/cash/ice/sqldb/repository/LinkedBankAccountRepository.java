package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.LinkedBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LinkedBankAccountRepository extends JpaRepository<LinkedBankAccount, Integer> {

    List<LinkedBankAccount> findByEntityIdOrderByCreatedDateDesc(Integer entityId);

    Optional<LinkedBankAccount> findByIdAndEntityId(Integer id, Integer entityId);
}

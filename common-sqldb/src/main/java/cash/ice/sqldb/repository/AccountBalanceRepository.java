package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, Integer> {

    Optional<AccountBalance> findByAccountId(Integer accountId);

    List<AccountBalance> findByAccountIdIn(List<Integer> accounts);
}

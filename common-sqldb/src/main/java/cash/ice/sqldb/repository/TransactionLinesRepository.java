package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.TransactionLines;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionLinesRepository extends JpaRepository<TransactionLines, Integer> {

    @Query("select sum(t.amount) from #{#entityName} t where t.entityAccountId = ?1")
    BigDecimal getBalance(int entityAccountId);

    List<TransactionLines> findByEntityAccountIdIn(List<Integer> entityAccountIds);

    List<TransactionLines> findByTransactionIdIn(List<Integer> transactionIds);

    List<TransactionLines> findByEntityAccountIdAndTransactionIdIn(int entityAccountId, List<Integer> transactionIds);
}

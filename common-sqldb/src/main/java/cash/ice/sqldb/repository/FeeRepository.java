package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.Fee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeeRepository extends JpaRepository<Fee, Integer> {

    List<Fee> findByTransactionCodeIdAndCurrencyIdOrderByProcessOrder(int transactionCodeId, int currencyId);

    @Query(nativeQuery = true, value = "select * from fee where transaction_code_id in :transactionCodeIds and charge_type != 'ORIGINAL'",
            countQuery = "select count(distinct id) from fee where transaction_code_id in :transactionCodeIds and charge_type != 'ORIGINAL'")
    List<Fee> findNonOriginalByTransactionCodeIdIn(@Param("transactionCodeIds") List<Integer> transactionCodeIds);
}

package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.TransactionCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransactionCodeRepository extends JpaRepository<TransactionCode, Integer> {

    Optional<TransactionCode> getTransactionCodeByCode(String code);

    @Query(nativeQuery = true, value = "select distinct t.* from transaction_code t join fee f on f.transaction_code_id = t.id and f.currency_id = :currencyId",
            countQuery = "select count(distinct t.id) from transaction_code t join fee f on f.transaction_code_id = t.id and f.currency_id = :currencyId")
    Page<TransactionCode> getTransactionCodeByCurrencyId(@Param("currencyId") Integer currencyId, Pageable pageable);
}

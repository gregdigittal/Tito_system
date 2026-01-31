package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findBySessionId(String sessionId);

    List<Transaction> findByTransactionCodeIdAndCurrencyIdAndStatementDateAfter(Integer transactionCodeId, Integer currencyId, LocalDateTime statementDate);

    List<Transaction> findBySessionIdIn(List<String> sessionIds);

    List<Transaction> findByStatementDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query(nativeQuery = true, value = "select distinct t.* from transaction_lines l join transaction t on l.transaction_id = t.id " +
            "where l.entity_account_id = :accountId and (:vrnId is null or t.meta_data->'$.vrn' = :vrnId) and (:tagId is null or t.meta_data->'$.tag' = :tagId) " +
            "and (:transactionCodeId is null or t.transaction_code_id = :transactionCodeId) and (:description is null or l.description like concat('%',:description,'%'))",
            countQuery = "select count(distinct t.id) from transaction_lines l join transaction t on l.transaction_id = t.id " +
                    "where l.entity_account_id = :accountId and (:vrnId is null or t.meta_data->'$.vrn' = :vrnId) and (:tagId is null or t.meta_data->'$.tag' = :tagId) " +
                    "and (:transactionCodeId is null or t.transaction_code_id = :transactionCodeId) and (:description is null or l.description like concat('%',:description,'%'))")
    Page<Transaction> findTransactionsByAccount(@Param("accountId") Integer accountId, @Param("vrnId") Integer vrnId, @Param("tagId") Integer tagId,
                                                @Param("transactionCodeId") Integer transactionCodeId, @Param("description") String description, Pageable pageable);

    default Map<String, String> getFieldsRewriterMap() {
        return Map.of("id", "t.id",
                "transactionCodeId", "t.transaction_code_id",
                "createdDate", "t.created_date",
                "sessionId", "t.session_id",
                "channelId", "t.channel_id",
                "statementDate", "t.statement_date",
                "currencyId", "t.currency_id",
                "initiatorId", "t.initiator_id",
                "amount", "l.");
    }
}

package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.TransactionLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionLimitRepository extends JpaRepository<TransactionLimit, Integer> {

    @Query(nativeQuery = true, value = "select * from transaction_limit where active = true " +
            "and currency_id = :currencyId and direction = :direction " +
            "and (transaction_code_id = :transactionCodeId or transaction_code_id is null) " +
            "and (kyc_status_id = :kycStatusId or kyc_status_id is null) " +
            "and (entity_type_id = :entityTypeId or entity_type_id is null) " +
            "and (account_type_id = :accountTypeId or account_type_id is null) " +
            "and (initiator_type_id = :initiatorTypeId or initiator_type_id is null) " +
            "and (tier = :tier or tier is null) " +
            "and (authorisation_type = :authorisationType or authorisation_type is null)")
    List<TransactionLimit> findTransactionLimits(@Param("currencyId") Integer currencyId, @Param("direction") String direction,
                                                 @Param("transactionCodeId") Integer transactionCodeId, @Param("kycStatusId") Integer kycStatusId,
                                                 @Param("entityTypeId") Integer entityTypeId, @Param("accountTypeId") Integer accountTypeId,
                                                 @Param("initiatorTypeId") Integer initiatorTypeId, @Param("tier") String tier,
                                                 @Param("authorisationType") String authorisationType);
}
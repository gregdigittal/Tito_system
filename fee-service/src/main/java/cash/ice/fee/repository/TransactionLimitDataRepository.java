package cash.ice.fee.repository;

import cash.ice.fee.dto.TransactionLimitData;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TransactionLimitDataRepository extends MongoRepository<TransactionLimitData, String> {

    Optional<TransactionLimitData> findByTransactionLimitId(Integer transactionLimitId);
}

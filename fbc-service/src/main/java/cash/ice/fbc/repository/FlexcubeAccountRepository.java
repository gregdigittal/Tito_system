package cash.ice.fbc.repository;

import cash.ice.fbc.entity.FlexcubeAccount;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FlexcubeAccountRepository extends MongoRepository<FlexcubeAccount, String> {

    List<FlexcubeAccount> findByTransactionCodesIn(String transactionCodes);
}

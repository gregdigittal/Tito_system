package cash.ice.paygo.repository;

import cash.ice.paygo.entity.PaygoMerchant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PaygoMerchantRepository extends MongoRepository<PaygoMerchant, String> {

    Optional<PaygoMerchant> findByMerchantId(String merchantId);

    Optional<PaygoMerchant> findByMerchantTransactionCode(String transactionCode);

    @Query(value = "{ 'credentials._id' : ?0 }")
    List<PaygoMerchant> extractByCredentialId(String credentialId);
}

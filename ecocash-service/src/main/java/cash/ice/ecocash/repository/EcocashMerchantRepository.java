package cash.ice.ecocash.repository;

import cash.ice.ecocash.entity.EcocashMerchant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EcocashMerchantRepository extends MongoRepository<EcocashMerchant, String> {

    List<EcocashMerchant> findByTransactionCodesIn(String transactionCodes);

    Optional<EcocashMerchant> findByGeneral(Boolean general);
}

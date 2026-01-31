package cash.ice.ecocash.repository;

import cash.ice.ecocash.entity.EcocashPayment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EcocashPaymentRepository extends MongoRepository<EcocashPayment, String> {

    List<EcocashPayment> findByVendorRef(String vendorRef);

    Optional<EcocashPayment> findByClientCorrelator(String clientCorrelator);

    List<EcocashPayment> findByFinishedPayment(boolean finishedPayment);

    List<EcocashPayment> findByRecheck(Boolean recheck);

    List<EcocashPayment> findAllByCreatedTimeIsBefore(Instant createdTime);
}

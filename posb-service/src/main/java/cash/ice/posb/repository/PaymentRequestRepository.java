package cash.ice.posb.repository;

import cash.ice.posb.dto.PaymentRequest;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.mongodb.annotation.MongoRepository;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;

@MongoRepository
public interface PaymentRequestRepository extends CrudRepository<PaymentRequest, String> {

    @NonNull
    Optional<PaymentRequest> findByVendorRef(@NonNull String vendorRef);
}
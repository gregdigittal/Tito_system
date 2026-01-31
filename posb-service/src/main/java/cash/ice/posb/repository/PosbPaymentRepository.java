package cash.ice.posb.repository;

import cash.ice.posb.dto.PosbPayment;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.mongodb.annotation.MongoRepository;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;

@MongoRepository
public interface PosbPaymentRepository extends CrudRepository<PosbPayment, String> {

    @NonNull
    Optional<PosbPayment> findByVendorRef(@NonNull String vendorRef);
}
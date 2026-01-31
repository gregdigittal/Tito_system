package cash.ice.paygo.repository;

import cash.ice.paygo.entity.PaygoPayment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PaygoPaymentRepository extends MongoRepository<PaygoPayment, String> {

    boolean existsPaygoPaymentByPayGoId(String payGoId);

    Optional<PaygoPayment> findByPayGoId(String payGoId);
}

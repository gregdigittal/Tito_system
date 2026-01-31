package cash.ice.fbc.repository;

import cash.ice.fbc.entity.FbcPayment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FbcPaymentRepository extends MongoRepository<FbcPayment, String> {

    List<FbcPayment> findByVendorRef(String vendorRef);
}

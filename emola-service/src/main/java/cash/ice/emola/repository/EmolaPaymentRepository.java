package cash.ice.emola.repository;

import cash.ice.emola.entity.EmolaPayment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EmolaPaymentRepository extends MongoRepository<EmolaPayment, String> {

    List<EmolaPayment> findByVendorRef(String vendorRef);
}

package cash.ice.fbc.repository;

import cash.ice.fbc.entity.FlexcubePayment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FlexcubePaymentRepository extends MongoRepository<FlexcubePayment, String> {

    List<FlexcubePayment> findByVendorRef(String vendorRef);

    List<FlexcubePayment> findByFinishedPayment(boolean finishedPayment);
}

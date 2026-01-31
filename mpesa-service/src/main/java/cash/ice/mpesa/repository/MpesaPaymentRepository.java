package cash.ice.mpesa.repository;

import cash.ice.mpesa.entity.MpesaPayment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MpesaPaymentRepository extends MongoRepository<MpesaPayment, String> {

    List<MpesaPayment> findByVendorRef(String vendorRef);

    List<MpesaPayment> findByTransactionId(String transactionId);
}

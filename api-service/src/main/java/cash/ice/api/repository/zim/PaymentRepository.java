package cash.ice.api.repository.zim;

import cash.ice.api.entity.zim.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    List<Payment> findByPaymentCollectionId(Integer paymentCollectionId);

    List<Payment> findByAccountIdIn(List<Integer> accountIds);
}

package cash.ice.sqldb.repository;

import cash.ice.sqldb.entity.PaymentLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentLineRepository extends JpaRepository<PaymentLine, Integer> {

    List<PaymentLine> findByPaymentId(Integer paymentId);
}

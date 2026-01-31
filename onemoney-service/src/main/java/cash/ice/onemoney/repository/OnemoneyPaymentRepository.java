package cash.ice.onemoney.repository;

import cash.ice.onemoney.entity.OnemoneyPayment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OnemoneyPaymentRepository extends MongoRepository<OnemoneyPayment, String> {

    List<OnemoneyPayment> findByVendorRef(String vendorRef);

    Optional<OnemoneyPayment> findByOriginatorConversationId(String originatorConversationId);

    Optional<OnemoneyPayment> findByRefundOriginatorConversationId(String refundOriginatorConversationId);

    List<OnemoneyPayment> findByNeedCheckStatus(Boolean needCheckStatus);

    List<OnemoneyPayment> findByNeedRecheckStatus(Boolean needRecheckStatus);

    List<OnemoneyPayment> findAllByCreatedTimeIsBefore(Instant createdTime);
}

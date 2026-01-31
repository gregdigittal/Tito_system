package cash.ice.paygo.repository;

import cash.ice.paygo.entity.PaygoCallbackLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaygoCallbackLogRepository extends MongoRepository<PaygoCallbackLog, String> {
}

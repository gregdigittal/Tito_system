package cash.ice.api.service;

import cash.ice.api.dto.TransactionLimitView;
import cash.ice.sqldb.entity.TransactionLimit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionLimitService {

    Page<TransactionLimit> get(TransactionLimitView filter, Pageable pageable);

    TransactionLimit addOrUpdate(TransactionLimitView transactionLimitView);

    TransactionLimit setActive(TransactionLimitView transactionLimitView, boolean active);

    TransactionLimit delete(Integer id);
}

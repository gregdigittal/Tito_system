package cash.ice.fee.service;

import cash.ice.sqldb.entity.TransactionLimit;

import java.util.List;

public interface TransactionLimitOverrideService {

    List<TransactionLimit> overrideLimits(List<TransactionLimit> limits);
}

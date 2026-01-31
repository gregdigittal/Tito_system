package cash.ice.fee.service;

import cash.ice.common.dto.fee.FeesData;
import cash.ice.sqldb.entity.EntityClass;

import java.util.Map;

public interface TransactionLimitCheckService {

    void checkLimits(FeesData feesData, Map<Integer, EntityClass> allEntities);

    void rollbackLimitDataIfNeed(FeesData feesData);
}

package cash.ice.api.service;

import cash.ice.api.dto.moz.StatisticsTypeMoz;
import cash.ice.api.dto.moz.TransactionStatisticsMoz;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.sqldb.entity.EntityClass;

import java.util.List;

public interface TransactionStatisticsService {

    void addPayment(FeesData feesData);

    List<TransactionStatisticsMoz> getTransactionStatistics(EntityClass entity, StatisticsTypeMoz statisticsType, int days);

    int recalculateTransactionsStatistics();
}

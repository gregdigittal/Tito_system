package cash.ice.ledger.service;

import cash.ice.common.dto.fee.FeesData;
import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.AccountBalance;

import java.math.BigDecimal;
import java.util.Map;

public interface AccountBalanceService {

    void checkAccountBalanceAffordability(FeesData feesData);

    AccountBalance findOrCalculateAccountBalance(int accountId);

    void updateAccountBalances(Map<Account, BigDecimal> accountBalanceChangeMap);

    void checkBalanceCorrectness(Integer accountId);
}

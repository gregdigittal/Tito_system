package cash.ice.api.dto;

import cash.ice.sqldb.entity.Account;
import cash.ice.sqldb.entity.AccountStatus;
import cash.ice.sqldb.entity.AuthorisationType;

import java.math.BigDecimal;

public record AccountInput(
        Integer entityId,
        Integer accountTypeId,
        String accountNumber,
        AccountStatus accountStatus,
        BigDecimal dailyLimit,
        BigDecimal overdraftLimit,
        BigDecimal balanceMinimum,
        BigDecimal balanceWarning,
        Boolean balanceMinimumEnforce,
        Boolean notificationEnabled,
        Boolean autoDebit,
        AuthorisationType authorisationType) {

    public Account updateAccount(Account account) {
        return account
                .setEntityId(entityId)
                .setAccountTypeId(accountTypeId)
                .setAccountNumber(accountNumber)
                .setAccountStatus(accountStatus)
                .setDailyLimit(dailyLimit)
                .setOverdraftLimit(overdraftLimit)
                .setBalanceMinimum(balanceMinimum)
                .setBalanceWarning(balanceWarning)
                .setBalanceMinimumEnforce(balanceMinimumEnforce == Boolean.TRUE)
                .setNotificationEnabled(notificationEnabled == Boolean.TRUE)
                .setAutoDebit(autoDebit == Boolean.TRUE)
                .setAuthorisationType(authorisationType);
    }

    public Account toAccount() {
        return updateAccount(new Account());
    }
}

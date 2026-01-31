package cash.ice.fbc.service;

import cash.ice.fbc.dto.flexcube.FlexcubeBalanceResponse;
import cash.ice.fbc.entity.FlexcubeAccount;
import cash.ice.fbc.entity.FlexcubePayment;

import java.math.BigDecimal;

public interface FlexcubeBalanceService {

    FlexcubeBalanceResponse checkBalance(FlexcubeAccount fbcPoolAccount, BigDecimal amount, FlexcubePayment payment);

    void updateBalanceCache(FlexcubeBalanceResponse balance, FlexcubeAccount fbcPoolAccount, FlexcubePayment payment);
}

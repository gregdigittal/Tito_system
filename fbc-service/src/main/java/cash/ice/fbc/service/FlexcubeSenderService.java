package cash.ice.fbc.service;

import cash.ice.fbc.dto.flexcube.FlexcubeBalanceResponse;
import cash.ice.fbc.dto.flexcube.FlexcubePaymentRequest;
import cash.ice.fbc.dto.flexcube.FlexcubeResponse;
import cash.ice.fbc.dto.flexcube.FlexcubeStatusRequest;
import cash.ice.fbc.error.FlexcubeTimeoutException;

public interface FlexcubeSenderService {

    FlexcubeResponse sendPayment(FlexcubePaymentRequest request) throws FlexcubeTimeoutException;

    FlexcubeResponse sendCheck(FlexcubeStatusRequest request);

    FlexcubeBalanceResponse getBalance(String account, String branch);

    FlexcubeBalanceResponse updateBalance(FlexcubeBalanceResponse response);

    void evictBalance(String account, String branch);
}

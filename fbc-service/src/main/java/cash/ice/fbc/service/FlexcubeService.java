package cash.ice.fbc.service;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.fbc.entity.FlexcubePayment;
import org.apache.kafka.common.header.Headers;

public interface FlexcubeService {

    void processPayment(FeesData feesData, Headers headers);

    void checkStatus(FlexcubePayment payment);

    void failPayment(FeesData feesData, String errorCode, String reason, Headers headers);

    void failPayment(FlexcubePayment flexcubePayment, String errorCode, String reason, Headers headers);

    void processRefund(ErrorData errorData);
}

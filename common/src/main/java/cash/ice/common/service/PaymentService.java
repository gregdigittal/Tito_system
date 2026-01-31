package cash.ice.common.service;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeesData;
import org.apache.kafka.common.header.Headers;

public interface PaymentService {

    void processPayment(FeesData feesData, Headers headers);

    void processRefund(ErrorData errorData);
}

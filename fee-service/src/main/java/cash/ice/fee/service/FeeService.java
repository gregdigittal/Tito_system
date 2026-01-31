package cash.ice.fee.service;

import cash.ice.common.dto.PaymentRequest;
import cash.ice.common.dto.fee.FeesData;

public interface FeeService {

    FeesData process(PaymentRequest paymentRequest);
}

package cash.ice.onemoney.service;

import cash.ice.common.dto.ErrorData;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.service.PaymentService;
import cash.ice.onemoney.entity.OnemoneyPayment;
import com.huawei.cps.cpsinterface.api_resultmgr.Result;
import org.apache.kafka.common.header.Headers;

public interface OnemoneyPaymentService extends PaymentService {
    void processPayment(FeesData feesData, Headers headers);

    void callbackResult(Result result);

    void checkStatus(OnemoneyPayment onemoneyPayment, long durationSeconds, boolean recheck);

    long getStatusPollInitDelay(OnemoneyPayment payment);

    long getExpiredPaymentsRecheckAfterTime(OnemoneyPayment payment);

    void failPayment(FeesData feesData, String errorCode, String message, Headers headers);

    void failPayment(OnemoneyPayment payment, String errorCode, String message, Headers headers);

    void processRefund(ErrorData errorData);
}

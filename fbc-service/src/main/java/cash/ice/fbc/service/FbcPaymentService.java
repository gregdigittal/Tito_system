package cash.ice.fbc.service;

import cash.ice.common.dto.zim.PaymentOtpRequestZim;
import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.fbc.error.FbcException;
import org.apache.kafka.common.header.Headers;

public interface FbcPaymentService {

    void processPayment(PaymentRequestZim paymentRequest);

    void processOtp(PaymentOtpRequestZim otpRequest, Headers headers);

    void processError(FbcException cause);

    void processRefund(String vendorRef);
}

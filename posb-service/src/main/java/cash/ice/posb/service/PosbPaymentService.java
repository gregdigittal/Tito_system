package cash.ice.posb.service;

import cash.ice.common.dto.zim.PaymentOtpRequestZim;
import cash.ice.common.dto.zim.PaymentRefundRequestZim;
import cash.ice.common.dto.zim.PaymentRequestZim;
import cash.ice.posb.dto.PosbPayment;

public interface PosbPaymentService {

    void processPayment(PaymentRequestZim paymentRequest);

    void processOtp(PaymentOtpRequestZim otpRequest);

    void processError(Throwable cause);

    void processRefund(PaymentRefundRequestZim refundRequest);

    PosbPayment getPosbPayment(String vendorRef);
}

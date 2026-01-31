package cash.ice.posb.service;

import cash.ice.common.dto.zim.PaymentOtpRequestZim;
import cash.ice.common.dto.zim.PaymentRequestZim;

public interface PosbValidationService {

    void validatePaymentRequest(PaymentRequestZim paymentRequest);

    void validateOtpRequest(PaymentOtpRequestZim otpRequest);

    void checkSuccessfulStatus(String status);
}

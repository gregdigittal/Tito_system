package cash.ice.posb.service.impl;

import cash.ice.posb.service.PosbValidationService;
import cash.ice.common.dto.zim.PaymentOtpRequestZim;
import cash.ice.common.dto.zim.PaymentRequestZim;
import jakarta.inject.Singleton;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Objects;

@Singleton
@RequiredArgsConstructor
@Slf4j
public class PosbValidationServiceImpl implements PosbValidationService {

    @Override
    public void validatePaymentRequest(PaymentRequestZim paymentRequest) {
        Objects.requireNonNull(paymentRequest.getAccountNumber(), "'accountNumber' payment field is not provided");
        Objects.requireNonNull(paymentRequest.getAmount(), "'amount' payment field is not provided");
        Objects.requireNonNull(paymentRequest.getCurrencyCode(), "'currencyCode' is not provided");
        Objects.requireNonNull(paymentRequest.getPaymentDescription(), "'paymentDescription' is not provided");
        if (paymentRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("'amount' payment field is not positive");
        }
    }

    @Override
    public void validateOtpRequest(PaymentOtpRequestZim otpRequest) {
        Objects.requireNonNull(otpRequest.getOtp(), "'otp' field is not provided");
    }


    @Override
    public void checkSuccessfulStatus(String status) {
        if (!"SUCCESSFUL".equals(status)) {
            throw new RuntimeException("Response status is not SUCCESSFUL: " + status);
        }
    }
}

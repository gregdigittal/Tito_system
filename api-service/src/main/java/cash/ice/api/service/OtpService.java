package cash.ice.api.service;

import cash.ice.api.dto.OtpData;
import cash.ice.api.dto.OtpType;

public interface OtpService {

    OtpData sendOtpToAccount(OtpType otpType, String accountNumber, int digitsAmount, boolean resend);

    OtpData sendOtp(OtpType otpType, int entityId, int digitsAmount, boolean resend);

    OtpData sendOtp(OtpType otpType, String msisdn, int digitsAmount, boolean resend);

    void validateOtp(OtpType otpType, Integer entityId, String otp);

    void validateOtp(OtpType otpType, String msisdn, String otp);

    String restorePin(OtpType otpType, String msisdn, Integer entityId, String accountNumber);

    void cleanupExpiredOtpDataTask();
}

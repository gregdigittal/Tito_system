package cash.ice.api.config.property;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Duration;

@Data
@Accessors(chain = true)
public class MfaProperties {
    private Duration accessTokenExpiration;
    private int otpDigitsAmount;
    private boolean otpSendSms;
    private Duration otpCodeExpiration;
    private int totpQrCodeDigits;
    private int totpQrCodePeriod;
    private int backupCodesAmount;
    private int maxWrongLoginAttempts;
    private Duration maxWrongLoginPeriod;
    private String expiredTokensCleanupCron;
}

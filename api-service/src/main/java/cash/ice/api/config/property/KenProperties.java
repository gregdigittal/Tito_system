package cash.ice.api.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@ConfigurationProperties(prefix = "ice.cash.ken")
@Component("KenProperties")
@Data
public class KenProperties {
    private boolean validateEmailUniqueness;
    private boolean validatePhoneUniqueness;
    private boolean validateIdUniqueness;
    private boolean userRegCheckOtp;
    private boolean registerNotificationSmsEnable;
    private String registerNotificationSmsMessageEn;
    private Duration paymentTimeoutDuration;
    private boolean paymentConfirmationSmsEnable;
    private String paymentConfirmationSmsMessageEn;
}

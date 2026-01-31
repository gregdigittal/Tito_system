package cash.ice.api.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@ConfigurationProperties(prefix = "ice.cash.otp")
@Component("OtpProperties")
@Data
public class OtpProperties {
    private String dataCollection;
    private Duration requestExpirationDuration;
    private Duration resendMsisdnChangeAfter;
    private String expiredRequestsCleanupCron;
}

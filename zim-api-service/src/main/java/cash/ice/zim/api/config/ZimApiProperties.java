package cash.ice.zim.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "ice.cash.zim.api")
@Component
@Data
public class ZimApiProperties {
    private List<String> allowedBanks;
    private Duration paymentTimeout;
    private boolean checkSuccessfulPaymentTimeoutBeforeSp = true;
    private boolean checkSuccessfulPaymentTimeoutAfterSp = true;
    private boolean apiKeyCheck;
    private boolean spPollingEnabled = true;
    private Integer spMaxTries;
    private Duration spTriesInterval;
    private String apiKey;
    private String apiKeyInternal;
    private String reversalUrl;
    private String statusUrl;
    private String nameInfoUrl;
    private String debugInfoUrl;
    private String propsUrl;
    private String actuatorHealthUrl;
    private Map<String, String> serviceHost;
}

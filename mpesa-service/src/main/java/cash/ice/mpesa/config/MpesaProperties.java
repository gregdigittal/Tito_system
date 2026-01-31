package cash.ice.mpesa.config;

import com.fc.sdk.APIMethodType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@ConfigurationProperties(prefix = "ice.cash.mpesa")
@Component
@Data
public class MpesaProperties {
    private String apiKey;
    private String publicKey;
    private String icecashBusinessShortcode;
    private String originHeader;
    private String securityCredential;
    private String initiatorIdentifier;
    private boolean fillOptionalReversalAmount = true;
    private boolean sendStatusQuery = true;
    private Duration statusQueryAfterErrorDelay;
    private Duration statusQueryAfterFailureResponseDelay;
    private Request c2bRequest;
    private Request b2cRequest;
    private Request b2bRequest;
    private Request reversalRequest;
    private Request queryNameRequest;
    private Request queryTransactionStatusRequest;

    @Data
    public static class Request {
        private String addressHost;
        private int addressPort;
        private boolean useSsl;
        private APIMethodType methodType;
        private String path;
    }
}

package cash.ice.paygo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@ConfigurationProperties(prefix = "ice.cash.paygo")
@Component
@Data
public class PaygoProperties {
    private String url;
    private String qr64Url;
    private String prefix;
    private int idTotalDigits;
    private int requestExpirySeconds;
    private Map<String, String> simulateResponse;
    private Admin admin;

    @Data
    public static class Admin {
        private String headerName;
        private String headerValue;
        private String financialInstitutionUrl;
        private String merchantUrl;
        private String authorizedCredentialUrl;
        private String merchantCredentialsUrl;
        private String expirationUrl;
        private String paymentStatusUrl;
    }
}

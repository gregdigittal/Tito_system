package cash.ice.fbc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "ice.cash.fbc")
@Component
@Data
public class FbcProperties {
    private String host;
    private String loginUrl;
    private String accountVerificationUrl;
    private String generateOtpUrl;
    private String verifyOtpUrl;
    private String transferSubmissionUrl;
    private String queryStatusUrl;
    private String username;
    private String password;
    private String initiatorId;
    private String paymentDetails;
}

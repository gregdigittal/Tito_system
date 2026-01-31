package cash.ice.fbc.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@ConfigurationProperties(prefix = "ice.cash.flexcube")
@Component
@Data
public class FlexcubeProperties {
    private String url;
    private String user;
    private String password;
    private String product;
    private String byOrderOf;
    private String remarks;
    private String transactionEndpoint;
    private String pollTransactionEndpoint;
    private String getBalanceEndpoint;
    private Duration connectTimeout;
    private Duration readTimeout;
    private Integer statusPollInitDelay;
    private BalanceWarningEmail balanceWarningEmail;

    @Value("${spring.cache.redis.custom-ttl.fbcPoolBalance:5m}")
    private Duration balanceCacheDuration;

    @Data
    public static class BalanceWarningEmail {
        private String from;
        private String subject;
        private String body;
        private String recipients;
    }
}

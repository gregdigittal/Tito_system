package cash.ice.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "ice.cash.kafka.retry-policy")
@Component
@Data
public class KafkaRetryProperties {
    private String backOffType;
    private long intervalMs;
    private long maxIntervalMs = 30000L;
    private long maxElapsedTimeMs = Long.MAX_VALUE;
    private double multiplier = 1;
    private int attempts = 1;
}

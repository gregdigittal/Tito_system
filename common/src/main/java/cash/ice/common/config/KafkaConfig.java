package cash.ice.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {
    private final KafkaRetryProperties retryProperties;

    @Bean
    public BackOff backOff() {
        switch (retryProperties.getBackOffType()) {
            case "exponentialWithMaxRetries":
                ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(retryProperties.getAttempts());
                backOff.setInitialInterval(retryProperties.getIntervalMs());
                backOff.setMaxInterval(retryProperties.getMaxIntervalMs());
                backOff.setMultiplier(retryProperties.getMultiplier());
                return backOff;
            case "exponential":
                ExponentialBackOff backOffExp = new ExponentialBackOff(retryProperties.getIntervalMs(), retryProperties.getMultiplier());
                backOffExp.setMaxInterval(retryProperties.getMaxIntervalMs());
                backOffExp.setMaxElapsedTime(retryProperties.getMaxElapsedTimeMs());
                return backOffExp;
            case "fixed":
                return new FixedBackOff(retryProperties.getIntervalMs(), retryProperties.getAttempts());
            default:
                throw new IllegalArgumentException("Unknown backOffType: " + retryProperties.getBackOffType());
        }
    }

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template, BackOff backOff) {
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), backOff);
    }
}

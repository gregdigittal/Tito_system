package cash.ice.fee.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "ice.cash.fee.limits")
@Component("LimitsProperties")
@Data
public class LimitsProperties {
    private boolean enabled;
    private boolean overridesEnabled;
}

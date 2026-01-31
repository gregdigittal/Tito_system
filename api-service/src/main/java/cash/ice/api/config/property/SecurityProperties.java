package cash.ice.api.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "ice.cash.security")
@Component("SecurityProperties")
@Data
public class SecurityProperties {
    private List<String> trustedIssuers;
}

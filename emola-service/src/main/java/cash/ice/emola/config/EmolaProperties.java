package cash.ice.emola.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "ice.cash.emola")
@Component
@Data
public class EmolaProperties {
    private String paymentUrl;
    private String partnerCode;
    private String inboundSmsContent;
    private String privateKey;
}

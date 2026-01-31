package cash.ice.posb.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@ConfigurationProperties("ice.cash.posb")
@Data
public class PosbProperties {
    private String apiKey;
    private String host;
    private String instructionUrl;
    private String confirmationUrl;
    private String statusUrl;
    private String reversalUrl;
}

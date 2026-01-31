package cash.ice.api.config.property;

import lombok.Data;

@Data
public class KeycloakProperties {
    private String authServerUrl;
    private String realm;
    private String defaultClientId;
    private String defaultClientSecret;
    private String adminClientId;
    private String adminClientSecret;
}

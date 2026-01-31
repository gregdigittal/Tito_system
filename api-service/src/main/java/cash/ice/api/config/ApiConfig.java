package cash.ice.api.config;

import cash.ice.api.config.property.KeycloakProperties;
import cash.ice.api.service.KeycloakService;
import cash.ice.api.service.impl.KeycloakServiceImpl;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiConfig {

    @Bean
    @ConfigurationProperties(prefix = "ice.cash.keycloak.entities")
    protected KeycloakProperties entitiesKeycloakProperties() {
        return new KeycloakProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "ice.cash.keycloak.backoffice")
    protected KeycloakProperties backofficeKeycloakProperties() {
        return new KeycloakProperties();
    }

    @Bean
    protected KeycloakService keycloakService(KeycloakProperties entitiesKeycloakProperties) {
        return new KeycloakServiceImpl(entitiesKeycloakProperties, "entity_");
    }

    @Bean
    protected KeycloakService backofficeKeycloakService(KeycloakProperties backofficeKeycloakProperties) {
        return new KeycloakServiceImpl(backofficeKeycloakProperties, "staff_");
    }
}

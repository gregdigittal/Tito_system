package cash.ice.api.config;

import cash.ice.api.repository.InMemoryLoginDataStore;
import cash.ice.api.repository.LoginDataStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures a LoginDataStore bean exists when neither Mongo nor the no-mongodb profile
 * provides one (e.g. Render without MongoDB env or with failed Mongo connection).
 * Use of the in-memory store means login/MFA session data is not persisted across restarts.
 */
@Configuration
public class LoginDataStoreConfig {

    @Bean
    @ConditionalOnMissingBean(LoginDataStore.class)
    public LoginDataStore fallbackLoginDataStore() {
        return new InMemoryLoginDataStore();
    }
}

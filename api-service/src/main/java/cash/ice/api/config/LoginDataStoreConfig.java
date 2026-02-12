package cash.ice.api.config;

import cash.ice.api.repository.InMemoryLoginDataStore;
import cash.ice.api.repository.LoginDataRepository;
import cash.ice.api.repository.LoginDataStore;
import cash.ice.api.repository.MongoLoginDataStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class LoginDataStoreConfig {

    @Bean
    public LoginDataStore loginDataStore(
            @Autowired(required = false) LoginDataRepository mongoRepository) {
        if (mongoRepository != null) {
            log.info("Using MongoDB LoginDataStore");
            return new MongoLoginDataStore(mongoRepository);
        }
        log.warn("MongoDB not available — using in-memory LoginDataStore (session data will not survive restarts)");
        return new InMemoryLoginDataStore();
    }
}

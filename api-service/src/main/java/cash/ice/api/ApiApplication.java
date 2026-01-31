package cash.ice.api;

import cash.ice.api.entity.ApiEntitiesMarker;
import cash.ice.api.repository.ApiRepositoriesMarker;
import cash.ice.sqldb.entity.CommonEntitiesMarker;
import cash.ice.sqldb.repository.CommonRepositoriesMarker;
import dev.samstevens.totp.spring.autoconfigure.TotpAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"cash.ice.api", "cash.ice.common", "cash.ice.sqldb"}, exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableJpaRepositories(basePackageClasses = {ApiRepositoriesMarker.class, CommonRepositoriesMarker.class})
@EntityScan(basePackageClasses = {ApiEntitiesMarker.class, CommonEntitiesMarker.class})
@Import(value = {TotpAutoConfiguration.class})
@EnableTransactionManagement
@EnableScheduling
@EnableCaching
public class ApiApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ApiApplication.class)
                .properties("spring.config.name:application,security,kafka,mongodb,sqldb,cache,logging")
                .build()
                .run(args);
    }
}

package cash.ice.ledger;

import cash.ice.sqldb.entity.CommonEntitiesMarker;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"cash.ice.ledger", "cash.ice.common", "cash.ice.sqldb"}, exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableJpaRepositories(
        basePackages = {"cash.ice.sqldb.repository", "cash.ice.ledger.repository"}
)
@EntityScan(basePackageClasses = CommonEntitiesMarker.class, basePackages = "cash.ice.ledger.entity")
@EnableTransactionManagement
@EnableCaching
public class LedgerApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        new SpringApplicationBuilder(LedgerApplication.class)
                .properties("spring.config.name:application,kafka,sqldb,cache,logging")
                .build()
                .run(args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(LedgerApplication.class);
    }
}

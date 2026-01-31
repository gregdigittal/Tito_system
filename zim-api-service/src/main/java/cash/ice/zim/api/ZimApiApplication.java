package cash.ice.zim.api;

import cash.ice.zim.api.entity.LegacyEntitiesMarker;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"cash.ice.zim.api", "cash.ice.common"}, exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableJpaRepositories(basePackages = "cash.ice.zim.api.repository")
@EntityScan(basePackageClasses = LegacyEntitiesMarker.class)
@EnableScheduling
public class ZimApiApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ZimApiApplication.class)
                .properties("spring.config.name:application,kafka,sqldb,mongodb,logging")
                .build()
                .run(args);
    }
}

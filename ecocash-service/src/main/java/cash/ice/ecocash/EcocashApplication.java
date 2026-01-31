package cash.ice.ecocash;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"cash.ice.ecocash", "cash.ice.common"}, exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableMongoRepositories(basePackages = "cash.ice.ecocash.repository")
@EnableScheduling
@EnableCaching
public class EcocashApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        new SpringApplicationBuilder(EcocashApplication.class)
                .properties("spring.config.name:application,kafka,mongodb,logging")
                .build()
                .run(args);
    }
}
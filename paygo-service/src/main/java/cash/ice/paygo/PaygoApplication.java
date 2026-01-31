package cash.ice.paygo;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {"cash.ice.paygo", "cash.ice.common"}, exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableMongoRepositories(basePackages = "cash.ice.paygo.repository")
@EnableCaching
public class PaygoApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        new SpringApplicationBuilder(PaygoApplication.class)
                .properties("spring.config.name:application,kafka,mongodb,logging")
                .build()
                .run(args);
    }

}
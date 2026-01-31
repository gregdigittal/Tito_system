package cash.ice.api.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "ice.cash.cors")
@Data
@Slf4j
public class CorsConfig {
    private String pathPattern;
    private List<String> allowedMethods;
    private List<String> allowedOrigins;
    private List<String> allowedHeaders;
    private List<String> allowedOriginPatterns;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NotNull CorsRegistry registry) {
                if (pathPattern != null && !pathPattern.isEmpty()) {
                    log.info("Registering CORS policy for pathPattern: {}, allowedMethods: {}, allowedOrigins: {}, allowedHeaders: {}, allowedOriginPatterns: {}",
                            pathPattern, allowedMethods, allowedOrigins, allowedHeaders, allowedOriginPatterns);
                    CorsRegistration corsRegistration = registry.addMapping(pathPattern);
                    if (allowedMethods != null && !allowedMethods.isEmpty()) {
                        corsRegistration.allowedMethods(allowedMethods.toArray(new String[0]));
                    }
                    if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
                        corsRegistration.allowedOrigins(allowedOrigins.toArray(new String[0]));
                    }
                    if (allowedHeaders != null && !allowedHeaders.isEmpty()) {
                        corsRegistration.allowedHeaders(allowedHeaders.toArray(new String[0]));
                    }
                    if (allowedOriginPatterns != null && !allowedOriginPatterns.isEmpty()) {
                        corsRegistration.allowedOriginPatterns(allowedOriginPatterns.toArray(new String[0]));
                    }
                } else {
                    log.warn("Skipping CORS policy registering, pathPattern: {}", pathPattern);
                }
            }
        };
    }
}

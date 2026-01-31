package cash.ice.zim.api.config;

import cash.ice.common.constant.IceCashProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
public class ZimApiSecurityConfig {

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, @Autowired ApplicationContext applicationContext) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                        .requestMatchers(getPublicUrls()).permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }

    private String[] getPublicUrls() {
        List<String> urls = new ArrayList<>(List.of("/api/v1/zim/payment/**", "/actuator/health", "/actuator/info"));
        if (!IceCashProfile.PROD.equals(activeProfile)) {
            urls.add("/actuator/**");
            urls.add("/docs/**");
        }
        return urls.toArray(new String[0]);
    }
}

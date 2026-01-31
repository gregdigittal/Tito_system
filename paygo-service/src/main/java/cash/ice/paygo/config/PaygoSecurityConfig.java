package cash.ice.paygo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class PaygoSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, @Autowired ApplicationContext applicationContext) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                        .requestMatchers("/api/paygo/**", "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/paygo/**", "/api/v1/unsecure/paygo/**", "/actuator/**", "/docs/**").permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }
}

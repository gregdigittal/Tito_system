package cash.ice.api.config;

import cash.ice.api.config.property.SecurityProperties;
import cash.ice.api.converter.KeycloakJwtConverter;
import cash.ice.api.util.HttpUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true)
@RequiredArgsConstructor
public class ApiSecurityConfig {
    private final SecurityProperties securityProperties;
    private final KeycloakJwtConverter keycloakJwtConverter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, @Autowired ApplicationContext applicationContext) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement -> sessionManagement
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headersConfigurer -> headersConfigurer
                        .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true)
                                .preload(false)))
                .oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(
                        new JwtIssuerAuthenticationManagerResolver(getAuthManagers()::get)))
                .authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                        .requestMatchers("/actuator/health", "/actuator/info", "/api/v1/config/**", "/api/v1/user/**", "/api/v1/moz/**", "/api/v1/me60/**", "/api/v1/ken/**").permitAll()
                        .requestMatchers("/api/v1/users/login", "/api/v1/users/login/form", "/api/v1/users/backoffice/login/form", "/api/v1/users/register").permitAll()
                        .requestMatchers("/api/v1/users/**").authenticated()
                        .requestMatchers("/actuator/**", "/docs/**", "/api/v1/unsecure/**")
                        .access(HttpUtils.getWebExpressionAuthorizationManager(applicationContext, "@IpWhitelist.check()"))
                        .requestMatchers("/api/v1/payments/pending/**", "/api/v1/documents/**").authenticated()
                        .requestMatchers("/graphql/**", "/graphiql/**")
                        .access(HttpUtils.getWebExpressionAuthorizationManager(applicationContext, "@IpWhitelist.check()"))
                        .requestMatchers("/api/v1/payment/**").hasRole("PAYMENT")
                        .anyRequest().denyAll());
        return http.build();
    }

    private Map<String, AuthenticationManager> getAuthManagers() {
        Map<String, AuthenticationManager> authenticationManagers = new HashMap<>();
        securityProperties.getTrustedIssuers().forEach(issuer -> {
            JwtAuthenticationProvider authenticationProvider = new JwtAuthenticationProvider(JwtDecoders.fromIssuerLocation(issuer));
            authenticationProvider.setJwtAuthenticationConverter(keycloakJwtConverter);
            authenticationManagers.put(issuer, authenticationProvider::authenticate);
        });
        return authenticationManagers;
    }
}

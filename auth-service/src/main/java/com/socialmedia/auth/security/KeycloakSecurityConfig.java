package com.socialmedia.auth.security;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Alternative to SecurityConfig for deployments that delegate identity entirely to an
 * external Keycloak realm instead of this service's own JWT issuance. Activate with
 * SPRING_PROFILES_ACTIVE=keycloak and point
 * spring.security.oauth2.resourceserver.jwt.issuer-uri (see application-keycloak.yml) at
 * the realm. Note this only covers auth-service's own endpoints that still need
 * protecting (device/session/audit management) - register/login/refresh become no-ops
 * since Keycloak's own token/auth endpoints replace them, and the other 12 services would
 * need the same resource-server switch to fully honor Keycloak-issued tokens platform-wide.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("keycloak")
public class KeycloakSecurityConfig {

    @Bean
    public SecurityFilterChain keycloakFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/prometheus", "/actuator/info",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakAuthenticationConverter())));

        return http.build();
    }

    /** Maps Keycloak's realm_access.roles claim onto the same ROLE_* authorities our own JwtService issues. */
    private JwtAuthenticationConverter keycloakAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof List<?> roles)) {
            return List.of();
        }
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        return Stream.concat(
                roles.stream().map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + String.valueOf(role).toUpperCase())),
                scopeConverter.convert(jwt).stream()
        ).collect(Collectors.toSet());
    }
}

package com.socialmedia.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.socialmedia.auth.domain.Role;
import com.socialmedia.auth.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;
    private JwtProperties properties;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret("unit-test-secret-key-must-be-at-least-256-bits-long-for-hs256");
        properties.setAccessTokenTtlSeconds(2);
        properties.setRefreshTokenTtlSeconds(60);
        jwtService = new JwtService(properties);
    }

    @Test
    void generatesAndParsesAValidToken() {
        UUID userId = UUID.randomUUID();
        String jti = UUID.randomUUID().toString();

        String token = jwtService.generateAccessToken(userId, jti, Set.of(Role.ROLE_USER, Role.ROLE_PREMIUM), "device-1");
        Claims claims = jwtService.parseAndValidate(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getId()).isEqualTo(jti);
        assertThat(claims.get("deviceId", String.class)).isEqualTo("device-1");
        assertThat(claims.getIssuer()).isEqualTo(properties.getIssuer());
        assertThat(claims.<java.util.List>get("roles", java.util.List.class))
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_PREMIUM");
    }

    @Test
    void rejectsATamperedToken() {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), UUID.randomUUID().toString(),
                Set.of(Role.ROLE_USER), "device-1");
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThatThrownBy(() -> jwtService.parseAndValidate(tampered))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsAnExpiredToken() throws InterruptedException {
        properties.setAccessTokenTtlSeconds(0);
        JwtService shortLivedJwtService = new JwtService(properties);
        String token = shortLivedJwtService.generateAccessToken(UUID.randomUUID(), UUID.randomUUID().toString(),
                Set.of(Role.ROLE_USER), "device-1");

        Thread.sleep(1_100);

        assertThatThrownBy(() -> shortLivedJwtService.parseAndValidate(token))
                .isInstanceOf(InvalidTokenException.class);
    }
}

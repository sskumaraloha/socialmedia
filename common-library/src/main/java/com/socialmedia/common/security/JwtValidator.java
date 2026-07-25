package com.socialmedia.common.security;

import com.socialmedia.common.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Verifies signature + expiry of access tokens issued by auth-service's JwtService.
 * Deliberately does NOT check the session-revocation cache (that lives in auth-service's
 * Redis, not shared cluster-wide) - a token revoked via logout stays technically valid
 * against other services until it naturally expires (access tokens are short-lived, ~15
 * minutes, so this is a bounded, accepted staleness window, not an open-ended one).
 */
@Component
@EnableConfigurationProperties(SharedJwtProperties.class)
public class JwtValidator {

    private final SecretKey signingKey;

    public JwtValidator(SharedJwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public AuthenticatedPrincipal validate(String token) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException("Access token has expired");
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Access token is invalid");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        String deviceId = claims.get("deviceId", String.class);
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        Set<String> roleSet = roles == null ? Set.of() : roles.stream().collect(Collectors.toUnmodifiableSet());
        return new AuthenticatedPrincipal(userId, roleSet, deviceId);
    }
}

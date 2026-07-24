package com.socialmedia.auth.security;

import com.socialmedia.auth.domain.Role;
import com.socialmedia.auth.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

/**
 * Issues and validates the platform's short-lived access tokens. Refresh tokens are
 * deliberately NOT JWTs - they are opaque random strings stored hashed (see
 * RefreshTokenServiceImpl) so they can be individually revoked without a JWT blacklist.
 */
@Service
@EnableConfigurationProperties(JwtProperties.class)
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String jti, Set<Role> roles, String deviceId) {
        Instant now = Instant.now();
        List<String> roleNames = roles.stream().map(Enum::name).collect(Collectors.toList());
        return Jwts.builder()
                .subject(userId.toString())
                .id(jti)
                .issuer(properties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.getAccessTokenTtlSeconds())))
                .claim("roles", roleNames)
                .claim("deviceId", deviceId)
                .signWith(signingKey)
                .compact();
    }

    public long getAccessTokenTtlSeconds() {
        return properties.getAccessTokenTtlSeconds();
    }

    public long getRefreshTokenTtlSeconds() {
        return properties.getRefreshTokenTtlSeconds();
    }

    public Claims parseAndValidate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("Access token has expired");
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Access token is invalid");
        }
    }
}

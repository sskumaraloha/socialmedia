package com.socialmedia.analytics.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Requires a Docker daemon - see auth-service's AuthControllerIT for why these are
 * gated behind the separate `integrationTest` task instead of the default `test` task.
 * Proves the admin-only RBAC gate: no token -> 401, a valid ROLE_USER token (no ADMIN
 * role) -> 403, matching the class-level @PreAuthorize("hasRole('ADMIN')") on
 * AnalyticsController.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsControllerIT {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    private String tokenFor(UUID userId, List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .claim("roles", roles)
                .signWith(key)
                .compact();
    }

    @Test
    void dashboardWithoutATokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/dashboard").param("date", "2026-07-25"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardWithNonAdminTokenIsForbidden() throws Exception {
        String token = tokenFor(UUID.randomUUID(), List.of("ROLE_USER"));

        mockMvc.perform(get("/api/v1/analytics/dashboard")
                        .param("date", "2026-07-25")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardWithAdminTokenReturnsZeroedStatsForAnUnseenDate() throws Exception {
        String token = tokenFor(UUID.randomUUID(), List.of("ROLE_ADMIN"));

        mockMvc.perform(get("/api/v1/analytics/dashboard")
                        .param("date", "2026-07-25")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dau").value(0))
                .andExpect(jsonPath("$.mau").value(0));
    }
}

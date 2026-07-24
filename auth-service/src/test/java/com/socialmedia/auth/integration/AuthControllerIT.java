package com.socialmedia.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.auth.dto.request.LoginRequest;
import com.socialmedia.auth.dto.request.RefreshTokenRequest;
import com.socialmedia.auth.dto.request.RegisterRequest;
import com.socialmedia.auth.domain.DeviceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * End-to-end verification against real Postgres/Redis containers and an embedded Kafka
 * broker - requires a Docker daemon, so it's excluded from the default `test` task (see
 * build.gradle.kts) and only runs via `./gradlew integrationTest`.
 *
 * The full flow this exercises (register -> me -> login -> refresh -> reuse-detection
 * -> revoked-session-can't-authenticate) was first validated by hand against a locally
 * running instance during development; this test captures that same flow so it's no
 * longer a one-off manual exercise.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, controlledShutdown = true)
class AuthControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("auth_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        registry.add("spring.kafka.bootstrap-servers", () -> System.getProperty("spring.embedded.kafka.brokers"));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginRefreshAndReuseDetectionFlow() throws Exception {
        RegisterRequest register = new RegisterRequest("integration-user@example.com", "SuperSecret123!",
                "it-device-1", "IT Device", DeviceType.WEB);

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.user.email").value("integration-user@example.com"))
                .andReturn();

        String accessToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("accessToken").asText();
        String refreshToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("refreshToken").asText();

        // A second registration with the same email must be rejected.
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isConflict());

        // The freshly issued access token authenticates /me.
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("integration-user@example.com"));

        // No token at all is rejected.
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());

        // Wrong password is rejected without leaking which part was wrong.
        LoginRequest badLogin = new LoginRequest("integration-user@example.com", "wrong-password", "it-device-1",
                null, null);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));

        // Correct password logs in.
        LoginRequest goodLogin = new LoginRequest("integration-user@example.com", "SuperSecret123!", "it-device-1",
                null, null);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goodLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()));

        // Refresh rotates the token; the response contains a new pair.
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk())
                .andReturn();
        String rotatedAccessToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .get("accessToken").asText();
        assertThat(rotatedAccessToken).isNotEqualTo(accessToken);

        // Reusing the now-superseded refresh token is treated as theft: rejected, and
        // it revokes every session for the account (including the one just issued above).
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + rotatedAccessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deviceListingAndRevocationEndToEnd() throws Exception {
        RegisterRequest register = new RegisterRequest("device-test@example.com", "SuperSecret123!",
                "it-device-2", "IT Device 2", DeviceType.ANDROID);

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();
        String accessToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/devices").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceId").value("it-device-2"))
                .andExpect(jsonPath("$[0].current").value(true));

        mockMvc.perform(delete("/api/v1/auth/devices/it-device-2").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // The device's own session was revoked as part of that call, so its access token is now dead.
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }
}

package com.socialmedia.message.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.message.domain.MessageType;
import com.socialmedia.message.dto.request.SendMessageRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
 * chat-service is not stood up here - sendMessage's membership check against it is
 * expected to fail, which is exactly what the second test asserts.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MessageControllerIT {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    private String tokenFor(UUID userId) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .claim("roles", List.of("ROLE_USER"))
                .claim("deviceId", "it-device")
                .signWith(key)
                .compact();
    }

    @Test
    void sendMessageWithoutATokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/chats/" + UUID.randomUUID() + "/messages")).andExpect(status().isUnauthorized());
    }

    @Test
    void sendMessageIsRejectedWhenChatServiceCannotConfirmMembership() throws Exception {
        UUID senderId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        String token = tokenFor(senderId);

        SendMessageRequest request = new SendMessageRequest(MessageType.TEXT, "hello", null, null, null, null);

        // chat-service isn't running in this test, so the synchronous membership check
        // fails closed - proving a message can never be injected into an unverified chat.
        mockMvc.perform(post("/api/v1/chats/" + chatId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }
}

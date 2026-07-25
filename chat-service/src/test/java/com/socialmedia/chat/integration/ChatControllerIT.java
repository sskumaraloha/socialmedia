package com.socialmedia.chat.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.chat.dto.request.AddMembersRequest;
import com.socialmedia.chat.dto.request.CreateGroupChatRequest;
import com.socialmedia.chat.dto.request.CreatePrivateChatRequest;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Requires a Docker daemon - see auth-service's AuthControllerIT for why these are
 * gated behind the separate `integrationTest` task instead of the default `test` task.
 * Simulates auth-service-issued JWTs by signing them locally with the shared secret.
 * Group chat creation now runs GroupChatCreationSaga, whose validation step calls
 * user-service over HTTP - stubbed here with a bare com.sun.net.httpserver.HttpServer
 * (JDK built-in, no new test dependency) that answers 200 OK to every GET, i.e. every
 * invited member id is treated as a real user.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("chat_db")
            .withUsername("postgres")
            .withPassword("postgres");

    private static HttpServer userServiceStub;

    @BeforeAll
    static void startUserServiceStub() throws Exception {
        userServiceStub = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        userServiceStub.createContext("/api/v1/users", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });
        userServiceStub.start();
    }

    @AfterAll
    static void stopUserServiceStub() {
        userServiceStub.stop(0);
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("services.user-service.base-url",
                () -> "http://localhost:" + userServiceStub.getAddress().getPort());
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
    void creatingAPrivateChatTwiceReturnsTheSameChat() throws Exception {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        String aliceToken = tokenFor(aliceId);

        String firstResponse = mockMvc.perform(post("/api/v1/chats/private")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreatePrivateChatRequest(bobId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("PRIVATE"))
                .andReturn().getResponse().getContentAsString();
        String chatId = objectMapper.readTree(firstResponse).get("id").asText();

        mockMvc.perform(post("/api/v1/chats/private")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreatePrivateChatRequest(bobId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(chatId));
    }

    @Test
    void nonAdminGroupMemberCannotAddMembers() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID newMemberId = UUID.randomUUID();
        String ownerToken = tokenFor(ownerId);
        String memberToken = tokenFor(memberId);

        CreateGroupChatRequest createRequest = new CreateGroupChatRequest("Team", "desc", Set.of(memberId), false, false);
        String createResponse = mockMvc.perform(post("/api/v1/chats/group")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String chatId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(post("/api/v1/chats/" + chatId + "/members")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new AddMembersRequest(Set.of(newMemberId)))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listChatsWithoutATokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/chats")).andExpect(status().isUnauthorized());
    }
}

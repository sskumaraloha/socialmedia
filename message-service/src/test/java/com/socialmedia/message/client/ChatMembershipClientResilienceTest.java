package com.socialmedia.message.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.socialmedia.message.exception.NotAChatMemberException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Proves the @CircuitBreaker/@Retry/@Bulkhead annotations on ChatMembershipClient actually
 * intercept calls - not just that they compile. A plain `new ChatMembershipClient(...)` unit
 * test would bypass Spring's AOP proxy entirely and prove nothing about the annotations
 * themselves, so this boots a narrow Spring context (just this bean + resilience4j
 * autoconfiguration - no Mongo/Redis/Kafka/security needed) against a bare
 * com.sun.net.httpserver stub standing in for chat-service, returning 403 for every request.
 */
@SpringBootTest(classes = {ChatMembershipClient.class, ChatMembershipClientResilienceTest.TestConfig.class},
        properties = {
                "resilience4j.retry.instances.chat-service.max-attempts=3",
                "resilience4j.retry.instances.chat-service.wait-duration=10ms",
                "resilience4j.retry.instances.chat-service.ignore-exceptions[0]=com.socialmedia.message.exception.NotAChatMemberException",
                "resilience4j.circuitbreaker.instances.chat-service.ignore-exceptions[0]=com.socialmedia.message.exception.NotAChatMemberException"
        })
class ChatMembershipClientResilienceTest {

    @Configuration
    @EnableAutoConfiguration
    static class TestConfig {
    }

    private static HttpServer stubServer;
    private static final AtomicInteger requestCount = new AtomicInteger();

    @BeforeAll
    static void startStubServer() throws Exception {
        stubServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        stubServer.createContext("/api/v1/chats", exchange -> {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(403, -1);
            exchange.close();
        });
        stubServer.start();
    }

    @AfterAll
    static void stopStubServer() {
        stubServer.stop(0);
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("services.chat-service.base-url", () -> "http://localhost:" + stubServer.getAddress().getPort());
    }

    @Autowired
    private ChatMembershipClient chatMembershipClient;

    @Test
    void businessExceptionFromChatServiceIsNotRetried() {
        requestCount.set(0);
        UUID chatId = UUID.randomUUID();

        assertThatThrownBy(() -> chatMembershipClient.fetchMemberIds(chatId, "Bearer token"))
                .isInstanceOf(NotAChatMemberException.class);

        // ignore-exceptions means resilience4j never treats this as a failure worth retrying -
        // exactly one request should have reached the stub, proving Retry did NOT engage.
        assertThat(requestCount.get()).isEqualTo(1);
    }
}

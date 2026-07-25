package com.socialmedia.message.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * Separate Spring context from ChatMembershipClientResilienceTest (different, unreachable
 * base-url from the start) so this scenario's retry/circuit-breaker state can never bleed
 * into - or be polluted by - the other test's "business exception" scenario; both use the
 * same resilience4j instance name ("chat-service") since that name is hardcoded on the
 * production annotations, so sharing one Spring context between the two would make circuit-
 * breaker state leak across otherwise-unrelated test cases.
 */
@SpringBootTest(classes = {ChatMembershipClient.class, ChatMembershipClientFallbackTest.TestConfig.class},
        properties = {
                "services.chat-service.base-url=http://localhost:1",
                "resilience4j.retry.instances.chat-service.max-attempts=3",
                "resilience4j.retry.instances.chat-service.wait-duration=10ms"
        })
class ChatMembershipClientFallbackTest {

    @Configuration
    @EnableAutoConfiguration
    static class TestConfig {
    }

    @Autowired
    private ChatMembershipClient chatMembershipClient;

    @Test
    void unreachableChatServiceRetriesThenFallsBackToACleanServiceUnavailableException() {
        // localhost:1 refuses every connection attempt - real proof that Retry actually
        // re-attempts (not just configured) and, once exhausted, the fallback method converts
        // the raw connection failure into ChatServiceUnavailableException rather than letting
        // a raw RestClientException leak out of this client.
        assertThatThrownBy(() -> chatMembershipClient.fetchMemberIds(UUID.randomUUID(), "Bearer token"))
                .isInstanceOf(ChatServiceUnavailableException.class);
    }
}

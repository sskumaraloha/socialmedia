package com.socialmedia.message.client;

import com.socialmedia.message.exception.NotAChatMemberException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Authoritative, synchronous membership check against chat-service before accepting a
 * send/list/react/etc - unlike ChatMembership (this service's local Kafka-fed mirror, used
 * only for typing-indicator fan-out), this call cannot be stale: chat-service's own
 * GET /chats/{chatId} already 403s a non-member and 404s an unknown chat, so reusing it
 * here avoids duplicating chat-service's membership logic. As a bonus, the same call
 * returns the full member list, which is exactly what's needed to fan out the new
 * message over WebSocket without waiting for the (eventually-consistent) local mirror.
 *
 * <p>The only synchronous inter-service call in this platform, and so the one place this
 * platform wraps with Circuit Breaker + Retry + Bulkhead (resilience4j, annotation-driven):
 * a chat-service outage or slowdown must not cascade into every message-service request
 * thread blocking or piling up. NotAChatMemberException is excluded from all three
 * (see application.yml's ignoreExceptions) since a 403/404 from chat-service is a normal
 * business outcome, not a technical failure worth retrying or counting against the breaker.
 */
@Component
public class ChatMembershipClient {

    private static final Logger log = LoggerFactory.getLogger(ChatMembershipClient.class);

    private final RestClient restClient;

    public ChatMembershipClient(@Value("${services.chat-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @CircuitBreaker(name = "chat-service", fallbackMethod = "fetchMemberIdsFallback")
    @Retry(name = "chat-service", fallbackMethod = "fetchMemberIdsFallback")
    @Bulkhead(name = "chat-service", fallbackMethod = "fetchMemberIdsFallback")
    public Set<UUID> fetchMemberIds(UUID chatId, String bearerAuthorizationHeader) {
        try {
            ChatMembersView view = restClient.get()
                    .uri("/api/v1/chats/{chatId}", chatId)
                    .header("Authorization", bearerAuthorizationHeader)
                    .retrieve()
                    .body(ChatMembersView.class);
            return view.members().stream().map(ChatMembersView.ChatMemberView::userId).collect(Collectors.toSet());
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.NotFound e) {
            throw new NotAChatMemberException();
        }
    }

    @SuppressWarnings("unused")
    private Set<UUID> fetchMemberIdsFallback(UUID chatId, String bearerAuthorizationHeader, Throwable t) {
        if (t instanceof NotAChatMemberException notAChatMemberException) {
            throw notAChatMemberException;
        }
        log.error("chat-service unavailable (circuit open, retries exhausted, or at capacity) while verifying "
                + "membership for chat {}: {}", chatId, t.getMessage());
        throw new ChatServiceUnavailableException();
    }
}

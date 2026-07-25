package com.socialmedia.message.client;

import com.socialmedia.message.exception.NotAChatMemberException;
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
 */
@Component
public class ChatMembershipClient {

    private static final Logger log = LoggerFactory.getLogger(ChatMembershipClient.class);

    private final RestClient restClient;

    public ChatMembershipClient(@Value("${services.chat-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

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
        } catch (Exception e) {
            log.error("Failed to verify chat membership via chat-service for chat {}: {}", chatId, e.getMessage());
            throw new ChatServiceUnavailableException();
        }
    }
}

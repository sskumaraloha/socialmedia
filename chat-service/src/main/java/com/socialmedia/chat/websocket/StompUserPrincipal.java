package com.socialmedia.chat.websocket;

import java.security.Principal;
import java.util.UUID;

/**
 * Spring's convertAndSendToUser() routes strictly by Principal#getName(), and our
 * AuthenticatedPrincipal record doesn't implement java.security.Principal - this thin
 * wrapper is what actually gets attached to the STOMP session so /user/queue/* delivery
 * can address a session by userId.
 */
public record StompUserPrincipal(UUID userId) implements Principal {

    @Override
    public String getName() {
        return userId.toString();
    }
}

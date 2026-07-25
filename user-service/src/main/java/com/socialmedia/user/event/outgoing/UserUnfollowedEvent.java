package com.socialmedia.user.event.outgoing;

import java.time.Instant;
import java.util.UUID;

public record UserUnfollowedEvent(UUID followerId, UUID followingId, Instant occurredAt) {
}

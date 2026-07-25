package com.socialmedia.chat.dto.request;

import java.time.Instant;

/** Null mutedUntil means "muted indefinitely, until explicitly unmuted". */
public record MuteChatRequest(Instant mutedUntil) {
}

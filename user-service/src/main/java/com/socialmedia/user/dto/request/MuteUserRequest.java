package com.socialmedia.user.dto.request;

import java.time.Instant;

/** Null mutedUntil mutes indefinitely. */
public record MuteUserRequest(Instant mutedUntil) {
}

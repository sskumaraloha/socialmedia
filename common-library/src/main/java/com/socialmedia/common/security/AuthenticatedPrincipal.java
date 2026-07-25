package com.socialmedia.common.security;

import java.util.Set;
import java.util.UUID;

/** What a resource server actually needs from an access token - not the full auth-service User aggregate. */
public record AuthenticatedPrincipal(UUID userId, Set<String> roles, String deviceId) {
}

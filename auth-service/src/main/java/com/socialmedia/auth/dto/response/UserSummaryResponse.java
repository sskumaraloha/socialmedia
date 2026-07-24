package com.socialmedia.auth.dto.response;

import com.socialmedia.auth.domain.AuthProvider;
import com.socialmedia.auth.domain.Role;
import java.util.Set;
import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String email,
        boolean emailVerified,
        boolean twoFactorEnabled,
        AuthProvider provider,
        Set<Role> roles
) {
}

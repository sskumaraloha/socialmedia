package com.socialmedia.common.security;

import com.socialmedia.common.exception.UnauthorizedException;
import org.springframework.security.core.context.SecurityContextHolder;

/** Static accessor for the AuthenticatedPrincipal JwtAuthenticationFilter put in the SecurityContext. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static AuthenticatedPrincipal get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            throw new UnauthorizedException("No authenticated user in context");
        }
        return principal;
    }
}

package com.socialmedia.auth.security;

import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.repository.UserRepository;
import com.socialmedia.common.exception.ResourceNotFoundException;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * The JWT filter only puts a lightweight UserPrincipal (built from token claims, no DB
 * hit) into the SecurityContext - controllers that need the full aggregate go through
 * here, which is the one place that does the by-id lookup.
 */
@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User get() {
        UserPrincipal principal = principal();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
    }

    public UserPrincipal principal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public Optional<String> jti() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return Optional.of(jwtAuth.getJti());
        }
        return Optional.empty();
    }
}

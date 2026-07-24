package com.socialmedia.auth.security;

import com.socialmedia.auth.service.SessionCacheService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates the access token's signature/expiry, then confirms against Redis that the
 * session behind it hasn't been revoked (logout, "log out all devices", admin action) -
 * a plain JWT check alone can't reflect revocation before natural expiry.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final SessionCacheService sessionCacheService;

    public JwtAuthenticationFilter(JwtService jwtService, SessionCacheService sessionCacheService) {
        this.jwtService = jwtService;
        this.sessionCacheService = sessionCacheService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            authenticate(token, request);
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        Claims claims = jwtService.parseAndValidate(token);
        String jti = claims.getId();

        if (sessionCacheService.get(jti).isEmpty()) {
            // Session revoked or expired from the cache - treat as unauthenticated
            // rather than throwing, so public endpoints on the same chain still work.
            return;
        }

        UUID userId = UUID.fromString(claims.getSubject());
        String deviceId = claims.get("deviceId", String.class);
        @SuppressWarnings("unchecked")
        List<String> roleNames = claims.get("roles", List.class);
        Set<GrantedAuthority> authorities = roleNames == null
                ? Set.of()
                : roleNames.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());

        UserPrincipal principal = new UserPrincipal(userId, claims.getSubject(), null, authorities, true, false);
        principal.setDeviceId(deviceId);

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(principal, authorities, jti);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

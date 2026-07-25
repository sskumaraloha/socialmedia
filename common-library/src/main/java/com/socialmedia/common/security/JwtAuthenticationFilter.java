package com.socialmedia.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Drop-in Bearer-token filter for any resource server validating auth-service-issued
 * JWTs. Register it in your service's own SecurityConfig - this library intentionally
 * does NOT auto-wire a full SecurityFilterChain, since which endpoints are public
 * differs per service.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtValidator jwtValidator;

    public JwtAuthenticationFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            try {
                AuthenticatedPrincipal principal = jwtValidator.validate(header.substring(BEARER_PREFIX.length()));
                SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(principal));
            } catch (Exception e) {
                // An invalid/expired token must NOT propagate out of a filter - filters run
                // before Spring's exception-handling machinery, so an uncaught exception here
                // becomes a raw 500 instead of a clean 401. Leaving the SecurityContext empty
                // lets the normal authorizeHttpRequests + AuthenticationEntryPoint path handle it.
                log.debug("Rejecting request with invalid bearer token: {}", e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }
}

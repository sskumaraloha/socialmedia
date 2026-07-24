package com.socialmedia.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.auth.service.RateLimiterService;
import com.socialmedia.common.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Brute-force protection on the endpoints attackers actually target - keyed by client IP
 * so a single attacker can't just retry forever, independent of which account they're
 * probing. Limits are intentionally generous for the endpoint list overall and tight per
 * bucket; tune via {@code app.security.rate-limit.*} once real traffic patterns are known.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;
    private final Map<String, Limit> limitsByPath;

    public record Limit(int maxRequests, Duration window) {
    }

    public RateLimitFilter(RateLimiterService rateLimiterService, ObjectMapper objectMapper,
            Map<String, Limit> limitsByPath) {
        this.rateLimiterService = rateLimiterService;
        this.objectMapper = objectMapper;
        this.limitsByPath = limitsByPath;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Limit limit = limitsByPath.get(request.getRequestURI());
        if (limit == null) {
            chain.doFilter(request, response);
            return;
        }

        String key = request.getRequestURI() + ":" + clientIp(request);
        boolean allowed = rateLimiterService.tryConsume(key, limit.maxRequests(), limit.window());
        if (!allowed) {
            respondTooManyRequests(request, response);
            return;
        }
        chain.doFilter(request, response);
    }

    private void respondTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "RATE_LIMITED",
                "Too many requests - please try again later",
                request.getRequestURI(),
                null,
                null
        );
        objectMapper.writeValue(response.getWriter(), error);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

package com.socialmedia.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.common.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.slf4j.MDC;
import com.socialmedia.common.web.CorrelationIdFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Security filter-chain exceptions happen before any controller runs, so common-library's
 * @RestControllerAdvice never sees them - this renders the same ApiError shape directly.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "UNAUTHENTICATED",
                "Authentication is required to access this resource",
                request.getRequestURI(),
                MDC.get(CorrelationIdFilter.TRACE_ID_MDC_KEY),
                null
        );
        objectMapper.writeValue(response.getWriter(), error);
    }
}

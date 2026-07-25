package com.socialmedia.gateway.controller;

import com.socialmedia.common.exception.ApiError;
import com.socialmedia.gateway.web.ReactiveCorrelationIdFilter;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Where every route's CircuitBreaker filter (see application.yml) sends a request once its
 * backing service's circuit is open - the one place downstream unavailability becomes a
 * clean, uniform response instead of each client having to handle a raw connection-refused
 * or timeout differently depending on which of the 11 backend services it happened to hit.
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public Mono<ApiError> fallback(ServerWebExchange exchange) {
        String correlationId = exchange.getAttribute(ReactiveCorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                "SERVICE_UNAVAILABLE",
                "This service is temporarily unavailable - please try again shortly",
                exchange.getRequest().getPath().value(),
                correlationId,
                null
        );
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        return Mono.just(error);
    }
}

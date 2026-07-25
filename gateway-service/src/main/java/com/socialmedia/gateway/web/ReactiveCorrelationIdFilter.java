package com.socialmedia.gateway.web;

import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * The WebFlux equivalent of common-library's servlet-only CorrelationIdFilter - that one is
 * gated behind @ConditionalOnWebApplication(Type.SERVLET) and never activates here, since
 * gateway-service is this platform's only reactive service. Generates or propagates
 * X-Correlation-Id at the very first hop a request makes into the platform, so every
 * downstream service (which forwards the header along, and puts it in its own MDC via the
 * servlet filter) traces back to the same id from the moment a client's request arrives.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReactiveCorrelationIdFilter implements WebFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_ATTRIBUTE = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        exchange.getAttributes().put(CORRELATION_ID_ATTRIBUTE, correlationId);

        ServerHttpRequest mutatedRequest = request.mutate().header(CORRELATION_ID_HEADER, correlationId).build();
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
}

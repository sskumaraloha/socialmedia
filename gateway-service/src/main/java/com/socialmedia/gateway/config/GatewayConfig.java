package com.socialmedia.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Backs every route's RequestRateLimiter filter (see application.yml) - a coarse,
 * per-client-IP limit at the edge protecting every route uniformly, complementing (not
 * replacing) auth-service's own finer-grained, per-endpoint RateLimitFilter that already
 * protects /login and /register specifically against brute-force attempts. Keys by the
 * client's real IP (X-Forwarded-For, since this gateway is itself the first hop and any
 * upstream load balancer would set that header) falling back to the connection's remote
 * address, rather than by authenticated user - most abusive traffic hitting a gateway
 * arrives unauthenticated (login attempts, registration spam), so a user-id key would miss
 * exactly the traffic this exists to limit.
 */
@Configuration
public class GatewayConfig {

    @Bean
    public KeyResolver clientKeyResolver() {
        return exchange -> {
            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return Mono.just(forwardedFor.split(",")[0].trim());
            }
            String remoteAddress = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(remoteAddress);
        };
    }
}

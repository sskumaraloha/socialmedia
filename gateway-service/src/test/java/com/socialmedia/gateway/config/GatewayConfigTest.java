package com.socialmedia.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

class GatewayConfigTest {

    private final KeyResolver keyResolver = new GatewayConfig().clientKeyResolver();

    @Test
    void resolvesKeyFromXForwardedForWhenPresent() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/chats").header("X-Forwarded-For", "203.0.113.5, 10.0.0.1"));

        String key = keyResolver.resolve(exchange).block();

        assertThat(key).isEqualTo("203.0.113.5");
    }

    @Test
    void fallsBackToRemoteAddressWhenNoForwardedForHeader() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/chats").remoteAddress(
                        new java.net.InetSocketAddress("127.0.0.1", 12345)));

        String key = keyResolver.resolve(exchange).block();

        assertThat(key).isEqualTo("127.0.0.1");
    }
}

package com.socialmedia.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class ReactiveCorrelationIdFilterTest {

    private final ReactiveCorrelationIdFilter filter = new ReactiveCorrelationIdFilter();

    @Test
    void propagatesAnIncomingCorrelationIdRatherThanGeneratingANewOne() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/chats").header("X-Correlation-Id", "existing-id"));
        WebFilterChain chain = ex -> Mono.empty();

        filter.filter(exchange, chain).block();

        String attribute = exchange.getAttribute(ReactiveCorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        assertThat(attribute).isEqualTo("existing-id");
        assertThat(exchange.getResponse().getHeaders().getFirst(ReactiveCorrelationIdFilter.CORRELATION_ID_HEADER))
                .isEqualTo("existing-id");
    }

    @Test
    void generatesAFreshCorrelationIdWhenNoneIsSupplied() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/chats"));
        WebFilterChain chain = ex -> Mono.empty();

        filter.filter(exchange, chain).block();

        String generated = exchange.getAttribute(ReactiveCorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        assertThat(generated).isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(ReactiveCorrelationIdFilter.CORRELATION_ID_HEADER))
                .isEqualTo(generated);
    }
}

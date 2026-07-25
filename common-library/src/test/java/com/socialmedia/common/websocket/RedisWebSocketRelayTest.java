package com.socialmedia.common.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisWebSocketRelayTest {

    @Mock private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void publishSendsAJsonEnvelopeToTheConfiguredChannel() {
        RedisWebSocketRelay relay = new RedisWebSocketRelay(redisTemplate, objectMapper, "ws:relay:test-service");

        relay.publish("user-123", "/queue/test-events", new TestPayload("hello"));

        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), bodyCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo("ws:relay:test-service");
        String json = (String) bodyCaptor.getValue();
        assertThat(json).contains("\"targetUserId\":\"user-123\"");
        assertThat(json).contains("\"destination\":\"/queue/test-events\"");
        assertThat(json).contains("\"message\":\"hello\"");
    }

    @Test
    void publishSwallowsSerializationFailuresInsteadOfPropagating() {
        RedisWebSocketRelay relay = new RedisWebSocketRelay(redisTemplate, objectMapper, "ws:relay:test-service");

        // A self-referencing object can't be serialized to JSON - this must not throw.
        Object[] cyclic = new Object[1];
        cyclic[0] = cyclic;

        relay.publish("user-123", "/queue/test-events", cyclic);

        verify(redisTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    private record TestPayload(String message) {
    }
}

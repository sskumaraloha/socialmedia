package com.socialmedia.common.websocket;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class RedisWebSocketRelayListenerTest {

    @Mock private SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void onMessageDeliversTheEnvelopeToTheTargetUser() {
        RedisWebSocketRelayListener listener = new RedisWebSocketRelayListener(objectMapper, messagingTemplate);
        String json = """
                {"targetUserId":"user-123","destination":"/queue/test-events","payload":{"message":"hi"}}""";

        listener.onMessage(fakeMessage(json), null);

        verify(messagingTemplate).convertAndSendToUser(org.mockito.ArgumentMatchers.eq("user-123"),
                org.mockito.ArgumentMatchers.eq("/queue/test-events"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void onMessageSwallowsMalformedPayloadsInsteadOfThrowing() {
        RedisWebSocketRelayListener listener = new RedisWebSocketRelayListener(objectMapper, messagingTemplate);

        listener.onMessage(fakeMessage("not valid json"), null);

        verify(messagingTemplate, never()).convertAndSendToUser(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    private Message fakeMessage(String body) {
        return new Message() {
            @Override
            public byte[] getBody() {
                return body.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public byte[] getChannel() {
                return "ws:relay:test-service".getBytes(StandardCharsets.UTF_8);
            }
        };
    }
}

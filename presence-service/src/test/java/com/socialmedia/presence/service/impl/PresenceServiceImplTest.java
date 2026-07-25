package com.socialmedia.presence.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.socialmedia.presence.dto.response.PresenceResponse;
import com.socialmedia.presence.event.PresenceEventPublisher;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
class PresenceServiceImplTest {

    private static final long TIMEOUT_SECONDS = 60;

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ZSetOperations<String, String> zSetOperations;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private PresenceEventPublisher eventPublisher;

    private PresenceServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new PresenceServiceImpl(redisTemplate, eventPublisher, TIMEOUT_SECONDS);
    }

    @Test
    void heartbeatPublishesOnlineEventOnlyOnTransitionFromOffline() {
        UUID userId = UUID.randomUUID();
        when(zSetOperations.score("presence:heartbeats", userId.toString())).thenReturn(null);

        service.heartbeat(userId);

        verify(zSetOperations).add(eq("presence:heartbeats"), eq(userId.toString()), anyDouble());
        verify(eventPublisher).publishPresenceChanged(eq(userId), eq(true), any(Instant.class));
    }

    @Test
    void heartbeatDoesNotRepublishWhenAlreadyOnline() {
        UUID userId = UUID.randomUUID();
        double recentScore = Instant.now().toEpochMilli();
        when(zSetOperations.score("presence:heartbeats", userId.toString())).thenReturn(recentScore);

        service.heartbeat(userId);

        verify(eventPublisher, never()).publishPresenceChanged(any(), eq(true), any());
    }

    @Test
    void goOfflineRemovesFromSortedSetAndPublishesOfflineEvent() {
        UUID userId = UUID.randomUUID();

        service.goOffline(userId);

        verify(zSetOperations).remove("presence:heartbeats", userId.toString());
        verify(valueOperations).set(eq("presence:lastseen:" + userId), any(String.class));
        verify(eventPublisher).publishPresenceChanged(eq(userId), eq(false), any(Instant.class));
    }

    @Test
    void getPresenceReportsOfflineWhenNoRecentHeartbeat() {
        UUID userId = UUID.randomUUID();
        when(zSetOperations.score("presence:heartbeats", userId.toString())).thenReturn(null);
        when(valueOperations.get("presence:lastseen:" + userId)).thenReturn(null);

        PresenceResponse response = service.getPresence(userId);

        assertThat(response.online()).isFalse();
        assertThat(response.lastSeenAt()).isNull();
    }

    @Test
    void getPresenceReportsOnlineWhenHeartbeatWithinTimeout() {
        UUID userId = UUID.randomUUID();
        double recentScore = Instant.now().toEpochMilli();
        when(zSetOperations.score("presence:heartbeats", userId.toString())).thenReturn(recentScore);

        PresenceResponse response = service.getPresence(userId);

        assertThat(response.online()).isTrue();
    }

    @Test
    void sweepStaleUsersMarksEachStaleUserOfflineAndPublishesOnce() {
        UUID staleUser = UUID.randomUUID();
        when(zSetOperations.rangeByScore(eq("presence:heartbeats"), eq(0.0), anyDouble()))
                .thenReturn(Set.of(staleUser.toString()));

        int swept = service.sweepStaleUsers();

        assertThat(swept).isEqualTo(1);
        verify(zSetOperations).remove("presence:heartbeats", staleUser.toString());
        verify(eventPublisher).publishPresenceChanged(eq(staleUser), eq(false), any(Instant.class));
    }

    @Test
    void sweepStaleUsersReturnsZeroWhenNothingIsStale() {
        when(zSetOperations.rangeByScore(eq("presence:heartbeats"), eq(0.0), anyDouble())).thenReturn(Set.of());

        assertThat(service.sweepStaleUsers()).isZero();
        verify(eventPublisher, times(0)).publishPresenceChanged(any(), any(Boolean.class), any());
    }
}

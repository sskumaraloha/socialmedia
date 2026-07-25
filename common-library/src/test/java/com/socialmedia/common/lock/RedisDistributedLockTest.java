package com.socialmedia.common.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisDistributedLockTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisDistributedLock lock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lock = new RedisDistributedLock(redisTemplate);
    }

    @Test
    void tryLockReturnsATokenWhenSetIfAbsentSucceeds() {
        when(valueOperations.setIfAbsent(eq("lock:my-key"), any(), eq(Duration.ofSeconds(10)))).thenReturn(true);

        Optional<String> token = lock.tryLock("my-key", Duration.ofSeconds(10));

        assertThat(token).isPresent();
    }

    @Test
    void tryLockReturnsEmptyWhenSomeoneElseAlreadyHoldsIt() {
        when(valueOperations.setIfAbsent(eq("lock:my-key"), any(), any(Duration.class))).thenReturn(false);

        Optional<String> token = lock.tryLock("my-key", Duration.ofSeconds(10));

        assertThat(token).isEmpty();
    }

    @Test
    void withLockRunsActionAndReleasesTheLockAfterward() {
        when(valueOperations.setIfAbsent(eq("lock:order-42"), any(), any(Duration.class))).thenReturn(true);

        String result = lock.withLock("order-42", Duration.ofSeconds(5), Duration.ofSeconds(1), () -> "done");

        assertThat(result).isEqualTo("done");
        verify(redisTemplate, times(1)).execute(any(), eq(java.util.List.of("lock:order-42")), any());
    }

    @Test
    void withLockStillReleasesTheLockWhenTheActionThrows() {
        when(valueOperations.setIfAbsent(eq("lock:order-42"), any(), any(Duration.class))).thenReturn(true);

        assertThatThrownBy(() -> lock.withLock("order-42", Duration.ofSeconds(5), Duration.ofSeconds(1), () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        verify(redisTemplate, times(1)).execute(any(), eq(java.util.List.of("lock:order-42")), any());
    }

    @Test
    void withLockThrowsLockAcquisitionExceptionWhenNeverAvailableWithinTheWaitBudget() {
        when(valueOperations.setIfAbsent(eq("lock:contended"), any(), any(Duration.class))).thenReturn(false);
        AtomicInteger actionCalls = new AtomicInteger();

        assertThatThrownBy(() -> lock.withLock("contended", Duration.ofSeconds(5), Duration.ofMillis(120),
                () -> actionCalls.incrementAndGet()))
                .isInstanceOf(LockAcquisitionException.class);

        assertThat(actionCalls.get()).isZero();
        verify(redisTemplate, never()).execute(any(), any(), any());
    }
}

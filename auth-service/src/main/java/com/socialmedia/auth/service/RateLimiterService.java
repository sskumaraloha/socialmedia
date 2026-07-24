package com.socialmedia.auth.service;

import java.time.Duration;

public interface RateLimiterService {

    /**
     * Atomically increments the counter for {@code key} and reports whether the caller
     * is still within {@code limit} requests per {@code window}.
     */
    boolean tryConsume(String key, int limit, Duration window);
}

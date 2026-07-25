package com.socialmedia.auth.config;

import com.socialmedia.common.lock.RedisDistributedLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class DistributedLockConfig {

    @Bean
    public RedisDistributedLock redisDistributedLock(StringRedisTemplate redisTemplate) {
        return new RedisDistributedLock(redisTemplate);
    }
}

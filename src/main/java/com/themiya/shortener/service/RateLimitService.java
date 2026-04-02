package com.themiya.shortener.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class RateLimitService {
    private final StringRedisTemplate redisTemplate;
    private final int createPerMinute;
    private final int redirectPerMinute;

    public RateLimitService(StringRedisTemplate redisTemplate,
                            @Value("${app.rate-limit.create-per-minute}") int createPerMinute,
                            @Value("${app.rate-limit.redirect-per-minute}") int redirectPerMinute) {
        this.redisTemplate = redisTemplate;
        this.createPerMinute = createPerMinute;
        this.redirectPerMinute = redirectPerMinute;
    }

    public boolean allowCreate(String identifier) {
        return allow("create", identifier, createPerMinute);
    }

    public boolean allowRedirect(String identifier) {
        return allow("redirect", identifier, redirectPerMinute);
    }

    private boolean allow(String endpoint, String identifier, int limit) {
        long minute = Instant.now().getEpochSecond() / 60;
        String key = "ratelimit:" + endpoint + ":" + identifier + ":" + minute;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(2));
        }
        return count == null || count <= limit;
    }
}

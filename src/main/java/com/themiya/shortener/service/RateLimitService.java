package com.themiya.shortener.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

@Service
public class RateLimitService {
    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private static final DefaultRedisScript<Long> SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>(
            "redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])\n" +
                    "redis.call('ZADD', KEYS[1], ARGV[2], ARGV[3])\n" +
                    "local current = redis.call('ZCARD', KEYS[1])\n" +
                    "redis.call('EXPIRE', KEYS[1], ARGV[4])\n" +
                    "if current <= tonumber(ARGV[5]) then return 1 else return 0 end",
            Long.class
    );

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
        return allow("create", identifier, createPerMinute, 60);
    }

    public boolean allowRedirect(String identifier) {
        return allow("redirect", identifier, redirectPerMinute, 60);
    }

    private boolean allow(String endpoint, String identifier, int limit, int windowSeconds) {
        try {
            long nowMillis = Instant.now().toEpochMilli();
            long windowStart = nowMillis - (windowSeconds * 1000L);
            String key = "ratelimit:" + endpoint + ":" + identifier;
            String member = nowMillis + "-" + UUID.randomUUID();

            Long allowed = redisTemplate.execute(
                    SLIDING_WINDOW_SCRIPT,
                    Collections.singletonList(key),
                    String.valueOf(windowStart),
                    String.valueOf(nowMillis),
                    member,
                    String.valueOf(windowSeconds + 1),
                    String.valueOf(limit)
            );
            return allowed != null && allowed == 1L;
        } catch (Exception ex) {
            log.warn("Rate limiter failed open for endpoint={} identifier={}", endpoint, identifier, ex);
            return true;
        }
    }
}

package com.shieldapi.shieldapi.service.impl;

import com.shieldapi.shieldapi.config.RateLimitProperties;
import com.shieldapi.shieldapi.redis.RateLimitResult;
import com.shieldapi.shieldapi.service.RateLimiterService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RedisRateLimiterService implements RateLimiterService {

    private static final String WINDOW_KEY_PREFIX = "rl:window:";
    private static final String VIOLATION_KEY_PREFIX = "rl:violations:";
    private static final String BAN_KEY_PREFIX = "rl:ban:";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> slidingWindowRateLimitScript;
    private final RateLimitProperties rateLimitProperties;

    public RedisRateLimiterService(
            StringRedisTemplate redisTemplate,
            DefaultRedisScript<Long> slidingWindowRateLimitScript,
            RateLimitProperties rateLimitProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.slidingWindowRateLimitScript = slidingWindowRateLimitScript;
        this.rateLimitProperties = rateLimitProperties;
    }

    @Override
    public RateLimitResult check(String identityKey) {
        String windowKey = WINDOW_KEY_PREFIX + identityKey;
        String violationKey = VIOLATION_KEY_PREFIX + identityKey;
        String banKey = BAN_KEY_PREFIX + identityKey;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(banKey))) {
            long ttl = getTtlSeconds(banKey, rateLimitProperties.getBanSeconds());
            return RateLimitResult.blocked(true, ttl, "TEMPORARY_BAN");
        }

        long now = Instant.now().toEpochMilli();
        Long allowed = redisTemplate.execute(
                slidingWindowRateLimitScript,
                Collections.singletonList(windowKey),
                String.valueOf(now),
                String.valueOf(rateLimitProperties.getWindowSeconds() * 1000L),
                String.valueOf(rateLimitProperties.getLimit()),
                now + ":" + UUID.randomUUID()
        );

        if (Objects.equals(allowed, 1L)) {
            redisTemplate.delete(violationKey);
            return RateLimitResult.permit();
        }

        long violations = increaseViolationCounter(violationKey);

        if (violations >= rateLimitProperties.getBanThreshold()) {
            redisTemplate.opsForValue().set(
                    banKey,
                    "1",
                    Duration.ofSeconds(rateLimitProperties.getBanSeconds())
            );
            redisTemplate.delete(violationKey);
            return RateLimitResult.blocked(true, rateLimitProperties.getBanSeconds(), "TEMPORARY_BAN");
        }

        long retryAfter = getTtlSeconds(windowKey, rateLimitProperties.getWindowSeconds());
        return RateLimitResult.blocked(false, retryAfter, "RATE_LIMIT_EXCEEDED");
    }

    private long increaseViolationCounter(String violationKey) {
        Long violations = redisTemplate.opsForValue().increment(violationKey);
        redisTemplate.expire(violationKey, rateLimitProperties.getBanSeconds(), TimeUnit.SECONDS);
        return violations == null ? 1L : violations;
    }

    private long getTtlSeconds(String key, int fallbackSeconds) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        if (ttl == null || ttl < 1) {
            return fallbackSeconds;
        }

        return ttl;
    }
}

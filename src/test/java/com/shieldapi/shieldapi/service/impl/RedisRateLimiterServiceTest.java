package com.shieldapi.shieldapi.service.impl;

import com.shieldapi.shieldapi.config.RateLimitProperties;
import com.shieldapi.shieldapi.redis.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private DefaultRedisScript<Long> script;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisRateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setLimit(5);
        properties.setWindowSeconds(60);
        properties.setBanThreshold(3);
        properties.setBanSeconds(300);

        rateLimiterService = new RedisRateLimiterService(redisTemplate, script, properties);
    }

    @Test
    void shouldAllowRequestWhenWithinLimit() {
        String identity = "user:alice";
        String banKey = "rl:ban:" + identity;

        when(redisTemplate.hasKey(banKey)).thenReturn(false);
        when(redisTemplate.execute(eq(script), anyList(), anyString(), anyString(), anyString(), anyString())).thenReturn(1L);

        RateLimitResult result = rateLimiterService.check(identity);

        assertTrue(result.allowed());
        assertFalse(result.banned());
        verify(redisTemplate).delete("rl:violations:" + identity);
    }

    @Test
    void shouldTemporarilyBanAfterThreeConsecutiveViolations() {
        String identity = "ip:127.0.0.1";
        String banKey = "rl:ban:" + identity;
        String violationKey = "rl:violations:" + identity;

        when(redisTemplate.hasKey(banKey)).thenReturn(false);
        when(redisTemplate.execute(eq(script), anyList(), anyString(), anyString(), anyString(), anyString())).thenReturn(0L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(violationKey)).thenReturn(3L);
        when(redisTemplate.expire(violationKey, 300, TimeUnit.SECONDS)).thenReturn(true);

        RateLimitResult result = rateLimiterService.check(identity);

        assertFalse(result.allowed());
        assertTrue(result.banned());
        verify(valueOperations).set(eq(banKey), eq("1"), eq(Duration.ofSeconds(300)));
        verify(redisTemplate).delete(violationKey);
    }
}

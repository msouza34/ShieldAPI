package com.shieldapi.shieldapi.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RedisScriptConfig {

    @Bean
    public DefaultRedisScript<Long> slidingWindowRateLimitScript() {
        String script = """
                local key = KEYS[1]
                local now = tonumber(ARGV[1])
                local window = tonumber(ARGV[2])
                local limit = tonumber(ARGV[3])
                local request_id = ARGV[4]

                redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
                local current = redis.call('ZCARD', key)

                if current >= limit then
                    redis.call('EXPIRE', key, math.ceil(window / 1000))
                    return 0
                end

                redis.call('ZADD', key, now, request_id)
                redis.call('EXPIRE', key, math.ceil(window / 1000))
                return 1
                """;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        return redisScript;
    }
}

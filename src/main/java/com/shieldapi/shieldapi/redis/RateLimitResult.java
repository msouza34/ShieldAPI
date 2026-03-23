package com.shieldapi.shieldapi.redis;

public record RateLimitResult(
        boolean allowed,
        boolean banned,
        long retryAfterSeconds,
        String reason
) {

    public static RateLimitResult permit() {
        return new RateLimitResult(true, false, 0, "ALLOWED");
    }

    public static RateLimitResult blocked(boolean banned, long retryAfterSeconds, String reason) {
        return new RateLimitResult(false, banned, retryAfterSeconds, reason);
    }
}

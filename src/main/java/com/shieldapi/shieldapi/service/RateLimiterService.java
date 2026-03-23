package com.shieldapi.shieldapi.service;

import com.shieldapi.shieldapi.redis.RateLimitResult;

public interface RateLimiterService {

    RateLimitResult check(String identityKey);
}

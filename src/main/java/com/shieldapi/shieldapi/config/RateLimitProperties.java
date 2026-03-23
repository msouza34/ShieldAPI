package com.shieldapi.shieldapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private int limit = 5;
    private int windowSeconds = 60;
    private int banThreshold = 3;
    private int banSeconds = 300;

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public int getBanThreshold() {
        return banThreshold;
    }

    public void setBanThreshold(int banThreshold) {
        this.banThreshold = banThreshold;
    }

    public int getBanSeconds() {
        return banSeconds;
    }

    public void setBanSeconds(int banSeconds) {
        this.banSeconds = banSeconds;
    }
}

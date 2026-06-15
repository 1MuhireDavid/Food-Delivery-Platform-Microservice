package com.david.api_gateway.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimiterConfiguration {

    @Value("${app.rate-limiter.order-placement.limit-for-period:20}")
    private int limitForPeriod;

    @Value("${app.rate-limiter.order-placement.limit-refresh-period-seconds:1}")
    private int limitRefreshPeriodSeconds;

    @Bean
    public RateLimiter orderPlacementRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(limitForPeriod)
                .limitRefreshPeriod(Duration.ofSeconds(limitRefreshPeriodSeconds))
                .timeoutDuration(Duration.ZERO)  // never block — return false immediately if no permit
                .build();
        return RateLimiterRegistry.of(config).rateLimiter("order-placement");
    }
}

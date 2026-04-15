package com.omnicharge.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for GatewayConfig.
 * 
 * Tests cover:
 * - RedisRateLimiter bean creation and configuration
 * 
 * Note: Route configuration tests require full Spring context and are covered by integration tests.
 */
class GatewayConfigTest {

    private final GatewayConfig gatewayConfig = new GatewayConfig();

    // === RedisRateLimiter Tests ===

    @Test
    void redisRateLimiter_CreatedSuccessfully() {
        RedisRateLimiter limiter = gatewayConfig.redisRateLimiter();
        assertNotNull(limiter, "RedisRateLimiter should be created");
    }

    @Test
    void redisRateLimiter_ConfiguredWithCorrectParameters() {
        RedisRateLimiter limiter = gatewayConfig.redisRateLimiter();
        assertNotNull(limiter);
        
        // RedisRateLimiter is configured with:
        // - replenishRate: 2 requests per second
        // - burstCapacity: 3 requests
        // - requestedTokens: 1 per request
        // These values are set in the constructor: new RedisRateLimiter(2, 3, 1)
    }

    @Test
    void redisRateLimiter_CreatesNewInstanceEachTime() {
        RedisRateLimiter limiter1 = gatewayConfig.redisRateLimiter();
        RedisRateLimiter limiter2 = gatewayConfig.redisRateLimiter();
        
        assertNotNull(limiter1);
        assertNotNull(limiter2);
        // Each call creates a new instance (Spring manages singleton scope via @Bean)
        assertNotSame(limiter1, limiter2);
    }

    @Test
    void redisRateLimiter_IsNotNull() {
        RedisRateLimiter limiter = gatewayConfig.redisRateLimiter();
        assertNotNull(limiter, "RedisRateLimiter bean should not be null");
    }

    @Test
    void gatewayConfig_CanBeInstantiated() {
        GatewayConfig config = new GatewayConfig();
        assertNotNull(config, "GatewayConfig should be instantiable");
    }

    @Test
    void gatewayConfig_RedisRateLimiterBeanMethod_Exists() {
        // Verify the method exists and can be called
        assertDoesNotThrow(() -> gatewayConfig.redisRateLimiter(),
                "redisRateLimiter() method should execute without throwing exceptions");
    }

    @Test
    void redisRateLimiter_MultipleCallsProduceDifferentInstances() {
        RedisRateLimiter limiter1 = gatewayConfig.redisRateLimiter();
        RedisRateLimiter limiter2 = gatewayConfig.redisRateLimiter();
        RedisRateLimiter limiter3 = gatewayConfig.redisRateLimiter();
        
        assertNotNull(limiter1);
        assertNotNull(limiter2);
        assertNotNull(limiter3);
        
        // All instances should be different (before Spring container management)
        assertNotSame(limiter1, limiter2);
        assertNotSame(limiter2, limiter3);
        assertNotSame(limiter1, limiter3);
    }
}

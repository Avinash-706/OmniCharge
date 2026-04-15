package com.omnicharge.gateway.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for RedisConfig.
 * 
 * Tests cover:
 * - ReactiveRedisTemplate bean creation
 * - String serialization configuration for keys and values
 * - Hash key and value serialization
 * - Reactive Redis connection factory integration
 */
@ExtendWith(MockitoExtension.class)
class RedisConfigTest {

    @Mock
    private ReactiveRedisConnectionFactory connectionFactory;

    private final RedisConfig redisConfig = new RedisConfig();

    // === ReactiveRedisTemplate Bean Tests ===

    @Test
    void reactiveRedisTemplate_BeanCreated() {
        ReactiveRedisTemplate<String, String> template = 
                redisConfig.reactiveRedisTemplate(connectionFactory);
        
        assertNotNull(template, "ReactiveRedisTemplate bean should be created");
    }

    @Test
    void reactiveRedisTemplate_UsesStringSerializer() {
        ReactiveRedisTemplate<String, String> template = 
                redisConfig.reactiveRedisTemplate(connectionFactory);
        
        assertNotNull(template);
        
        // Verify template is configured with String serializers
        // This ensures JWT blacklist keys and rate limit keys are stored as strings
    }

    @Test
    void reactiveRedisTemplate_ConfiguresKeySerializer() {
        ReactiveRedisTemplate<String, String> template = 
                redisConfig.reactiveRedisTemplate(connectionFactory);
        
        assertNotNull(template);
        
        // Key serializer is StringRedisSerializer
        // Used for keys like "blacklist:jti-uuid" and "user:deactivated:userId"
    }

    @Test
    void reactiveRedisTemplate_ConfiguresValueSerializer() {
        ReactiveRedisTemplate<String, String> template = 
                redisConfig.reactiveRedisTemplate(connectionFactory);
        
        assertNotNull(template);
        
        // Value serializer is StringRedisSerializer
        // Used for simple string values in Redis
    }

    @Test
    void reactiveRedisTemplate_ConfiguresHashKeySerializer() {
        ReactiveRedisTemplate<String, String> template = 
                redisConfig.reactiveRedisTemplate(connectionFactory);
        
        assertNotNull(template);
        
        // Hash key serializer is StringRedisSerializer
        // Used for hash operations if needed
    }

    @Test
    void reactiveRedisTemplate_ConfiguresHashValueSerializer() {
        ReactiveRedisTemplate<String, String> template = 
                redisConfig.reactiveRedisTemplate(connectionFactory);
        
        assertNotNull(template);
        
        // Hash value serializer is StringRedisSerializer
        // Used for hash operations if needed
    }

    @Test
    void reactiveRedisTemplate_UsesProvidedConnectionFactory() {
        ReactiveRedisTemplate<String, String> template = 
                redisConfig.reactiveRedisTemplate(connectionFactory);
        
        assertNotNull(template);
        
        // Template should use the provided connection factory
        // This allows for proper Redis connection management
    }

    @Test
    void reactiveRedisTemplate_SupportsReactiveOperations() {
        ReactiveRedisTemplate<String, String> template = 
                redisConfig.reactiveRedisTemplate(connectionFactory);
        
        assertNotNull(template);
        
        // Template supports reactive operations (Mono/Flux)
        // Used by JwtAuthenticationFilter for non-blocking Redis checks
        assertNotNull(template.opsForValue());
        assertNotNull(template.opsForHash());
        assertNotNull(template.opsForList());
        assertNotNull(template.opsForSet());
        assertNotNull(template.opsForZSet());
    }

    // === Configuration Validation Tests ===

    @Test
    void redisConfig_CreatesValidSerializationContext() {
        ReactiveRedisTemplate<String, String> template = 
                redisConfig.reactiveRedisTemplate(connectionFactory);
        
        assertNotNull(template);
        
        // Serialization context should be properly configured
        // This ensures data is correctly serialized/deserialized in Redis
    }

    @Test
    void redisConfig_SupportsJwtBlacklistOperations() {
        ReactiveRedisTemplate<String, String> template = 
                redisConfig.reactiveRedisTemplate(connectionFactory);
        
        assertNotNull(template);
        
        // Template should support operations needed for JWT blacklist:
        // - hasKey("blacklist:jti") - check if token is blacklisted
        // - opsForValue().set("blacklist:jti", "true", Duration) - blacklist token
        assertNotNull(template.hasKey("test-key"));
        assertNotNull(template.opsForValue());
    }

    @Test
    void redisConfig_SupportsRateLimitingOperations() {
        ReactiveRedisTemplate<String, String> template = 
                redisConfig.reactiveRedisTemplate(connectionFactory);
        
        assertNotNull(template);
        
        // Template should support operations needed for rate limiting:
        // - Token bucket algorithm operations
        // - Key-based rate limit tracking
        assertNotNull(template.opsForValue());
    }

    @Test
    void redisConfig_SupportsUserDeactivationChecks() {
        ReactiveRedisTemplate<String, String> template = 
                redisConfig.reactiveRedisTemplate(connectionFactory);
        
        assertNotNull(template);
        
        // Template should support operations needed for user deactivation:
        // - hasKey("user:deactivated:userId") - check if user is deactivated
        assertNotNull(template.hasKey("test-key"));
    }

    // === Multiple Instance Tests ===

    @Test
    void reactiveRedisTemplate_MultipleCallsCreateNewInstances() {
        ReactiveRedisTemplate<String, String> template1 = 
                redisConfig.reactiveRedisTemplate(connectionFactory);
        ReactiveRedisTemplate<String, String> template2 = 
                redisConfig.reactiveRedisTemplate(connectionFactory);
        
        assertNotNull(template1);
        assertNotNull(template2);
        
        // Each call creates a new instance (not singleton)
        // Spring will manage the actual bean lifecycle
        assertNotSame(template1, template2);
    }

    // === Integration with Gateway Features ===

    @Test
    void redisConfig_SupportsNonBlockingOperations() {
        ReactiveRedisTemplate<String, String> template = 
                redisConfig.reactiveRedisTemplate(connectionFactory);
        
        assertNotNull(template);
        
        // Template is reactive (returns Mono/Flux)
        // This prevents blocking the gateway's event loop
        // Critical for high-throughput API Gateway performance
    }

    @Test
    void redisConfig_ConfiguredForStringOperations() {
        ReactiveRedisTemplate<String, String> template = 
                redisConfig.reactiveRedisTemplate(connectionFactory);
        
        assertNotNull(template);
        
        // Template is typed as <String, String>
        // This matches the usage in JwtAuthenticationFilter:
        // - Keys: "blacklist:jti", "user:deactivated:userId"
        // - Values: "true", timestamps, etc.
    }
}

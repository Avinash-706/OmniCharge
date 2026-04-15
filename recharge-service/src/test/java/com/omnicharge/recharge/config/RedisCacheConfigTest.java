package com.omnicharge.recharge.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisCacheConfigTest {

    private final RedisCacheConfig config = new RedisCacheConfig();

    @Test
    void testCacheManagerBean() {
        // Given
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        // When
        RedisCacheManager cacheManager = config.cacheManager(connectionFactory);

        // Then
        assertThat(cacheManager).isNotNull();
    }
}

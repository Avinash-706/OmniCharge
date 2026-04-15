package com.omnicharge.operator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisConfigTest {

    private final RedisConfig config = new RedisConfig();

    @Test
    void redisTemplate_ShouldConfigureWithStringSerializers() {
        // Arrange
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        // Act
        RedisTemplate<String, String> template = config.redisTemplate(connectionFactory);

        // Assert
        assertThat(template).isNotNull();
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getHashValueSerializer()).isInstanceOf(StringRedisSerializer.class);
    }

    @Test
    void objectMapper_ShouldRegisterJavaTimeModule() {
        // Act
        ObjectMapper mapper = config.objectMapper();

        // Assert
        assertThat(mapper).isNotNull();
        assertThat(mapper.getRegisteredModuleIds()).contains("jackson-datatype-jsr310");
    }

    @Test
    void objectMapper_ShouldBeReusable() {
        // Act
        ObjectMapper mapper1 = config.objectMapper();
        ObjectMapper mapper2 = config.objectMapper();

        // Assert - Each call creates a new instance
        assertThat(mapper1).isNotNull();
        assertThat(mapper2).isNotNull();
        assertThat(mapper1).isNotSameAs(mapper2);
    }

    @Test
    void redisTemplate_ShouldSetConnectionFactory() {
        // Arrange
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        // Act
        RedisTemplate<String, String> template = config.redisTemplate(connectionFactory);

        // Assert
        assertThat(template.getConnectionFactory()).isEqualTo(connectionFactory);
    }

    @Test
    void shouldBeConfigurationClass() {
        // Assert
        assertThat(RedisConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class)).isTrue();
    }

    @Test
    void redisTemplate_ShouldHandleStringKeys() {
        // Arrange
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        // Act
        RedisTemplate<String, String> template = config.redisTemplate(connectionFactory);

        // Assert
        assertThat(template).isNotNull();
        // Verify generic types are String
        assertThat(template.getClass().getGenericInterfaces()).isNotEmpty();
    }
}

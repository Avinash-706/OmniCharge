package com.omnicharge.common.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JacksonConfig.
 * Tests ObjectMapper configuration for Java 8 date/time support.
 */
class JacksonConfigTest {

    private final JacksonConfig jacksonConfig = new JacksonConfig();

    @Test
    void objectMapper_shouldBeConfiguredWithJavaTimeModule() {
        // Act
        ObjectMapper objectMapper = jacksonConfig.objectMapper();

        // Assert
        assertThat(objectMapper).isNotNull();
        assertThat(objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
    }

    @Test
    void objectMapper_shouldSerializeLocalDateTimeAsString() throws JsonProcessingException {
        // Arrange
        ObjectMapper objectMapper = jacksonConfig.objectMapper();
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 45);

        // Act
        String json = objectMapper.writeValueAsString(dateTime);

        // Assert
        assertThat(json).contains("2024");
        assertThat(json).contains("01");
        assertThat(json).contains("15");
        assertThat(json).doesNotContain("timestamp"); // Should not be timestamp format
    }

    @Test
    void objectMapper_shouldDeserializeLocalDateTimeFromString() throws JsonProcessingException {
        // Arrange
        ObjectMapper objectMapper = jacksonConfig.objectMapper();
        String json = "\"2024-01-15T10:30:45\"";

        // Act
        LocalDateTime dateTime = objectMapper.readValue(json, LocalDateTime.class);

        // Assert
        assertThat(dateTime).isNotNull();
        assertThat(dateTime.getYear()).isEqualTo(2024);
        assertThat(dateTime.getMonthValue()).isEqualTo(1);
        assertThat(dateTime.getDayOfMonth()).isEqualTo(15);
        assertThat(dateTime.getHour()).isEqualTo(10);
        assertThat(dateTime.getMinute()).isEqualTo(30);
        assertThat(dateTime.getSecond()).isEqualTo(45);
    }

    @Test
    void objectMapper_shouldHandleComplexObjectsWithLocalDateTime() throws JsonProcessingException {
        // Arrange
        ObjectMapper objectMapper = jacksonConfig.objectMapper();
        TestObject testObject = new TestObject("test", LocalDateTime.of(2024, 1, 15, 10, 30));

        // Act
        String json = objectMapper.writeValueAsString(testObject);
        TestObject deserialized = objectMapper.readValue(json, TestObject.class);

        // Assert
        assertThat(deserialized.name).isEqualTo("test");
        assertThat(deserialized.timestamp).isEqualTo(testObject.timestamp);
    }

    // Test class for complex object serialization
    static class TestObject {
        public String name;
        public LocalDateTime timestamp;

        public TestObject() {}

        public TestObject(String name, LocalDateTime timestamp) {
            this.name = name;
            this.timestamp = timestamp;
        }
    }
}

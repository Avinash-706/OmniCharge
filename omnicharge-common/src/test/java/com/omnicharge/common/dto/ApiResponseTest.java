package com.omnicharge.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ApiResponse DTO.
 * Tests builder methods, serialization, and factory methods.
 */
class ApiResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void constructor_shouldCreateApiResponseWithAllFields() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        String testData = "test data";

        // Act
        ApiResponse<String> response = new ApiResponse<>(true, "Success", testData, now);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Success");
        assertThat(response.getData()).isEqualTo(testData);
        assertThat(response.getTimestamp()).isEqualTo(now);
    }

    @Test
    void successFactory_shouldCreateSuccessResponseWithData() {
        // Arrange
        String testData = "test data";

        // Act
        ApiResponse<String> response = ApiResponse.success(testData);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Success");
        assertThat(response.getData()).isEqualTo(testData);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void successFactory_shouldCreateSuccessResponseWithCustomMessage() {
        // Arrange
        String testData = "test data";
        String customMessage = "Operation completed";

        // Act
        ApiResponse<String> response = ApiResponse.success(customMessage, testData);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo(customMessage);
        assertThat(response.getData()).isEqualTo(testData);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void errorFactory_shouldCreateErrorResponseWithMessage() {
        // Arrange
        String errorMessage = "Something went wrong";

        // Act
        ApiResponse<Object> response = ApiResponse.error(errorMessage);

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo(errorMessage);
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void apiResponse_shouldSerializeToJson() throws Exception {
        // Arrange
        ApiResponse<String> response = ApiResponse.success("test data");

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        assertThat(json).contains("\"success\":true");
        assertThat(json).contains("\"message\":\"Success\"");
        assertThat(json).contains("\"data\":\"test data\"");
        assertThat(json).contains("\"timestamp\"");
    }

    @Test
    void apiResponse_shouldDeserializeFromJson() throws Exception {
        // Arrange
        String json = "{\"success\":true,\"message\":\"Success\",\"data\":\"test\",\"timestamp\":\"2024-01-15T10:30:45.0000000\"}";

        // Act
        ApiResponse<?> response = objectMapper.readValue(json, ApiResponse.class);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Success");
        assertThat(response.getData()).isEqualTo("test");
    }

    @Test
    void apiResponse_shouldExcludeNullFieldsInJson() throws Exception {
        // Arrange
        ApiResponse<Object> response = new ApiResponse<>(true, "Success", null, LocalDateTime.now());

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        // data field should be excluded when null due to @JsonInclude(JsonInclude.Include.NON_NULL)
        assertThat(json).contains("\"success\":true");
        assertThat(json).contains("\"message\":\"Success\"");
    }

    @Test
    void apiResponse_shouldHandleComplexDataTypes() {
        // Arrange
        TestData testData = new TestData("value1", 123);

        // Act
        ApiResponse<TestData> response = ApiResponse.success(testData);

        // Assert
        assertThat(response.getData()).isEqualTo(testData);
        assertThat(response.getData().field1).isEqualTo("value1");
        assertThat(response.getData().field2).isEqualTo(123);
    }

    // Test data class
    static class TestData {
        public String field1;
        public int field2;

        public TestData(String field1, int field2) {
            this.field1 = field1;
            this.field2 = field2;
        }
    }
}

package com.omnicharge.common.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ErrorResponse DTO.
 */
class ErrorResponseTest {

    @Test
    void constructor_shouldCreateErrorResponseWithStatusMessageAndPath() {
        // Act
        ErrorResponse response = new ErrorResponse(404, "Not found", "/api/users/123");

        // Assert
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getMessage()).isEqualTo("Not found");
        assertThat(response.getPath()).isEqualTo("/api/users/123");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getErrors()).isNull();
    }

    @Test
    void constructor_shouldCreateErrorResponseWithValidationErrors() {
        // Arrange
        Map<String, String> errors = new HashMap<>();
        errors.put("email", "Invalid email format");
        errors.put("password", "Password too short");

        // Act
        ErrorResponse response = new ErrorResponse(400, "Validation failed", errors, "/api/register");

        // Assert
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getMessage()).isEqualTo("Validation failed");
        assertThat(response.getErrors()).hasSize(2);
        assertThat(response.getErrors().get("email")).isEqualTo("Invalid email format");
        assertThat(response.getErrors().get("password")).isEqualTo("Password too short");
        assertThat(response.getPath()).isEqualTo("/api/register");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void allArgsConstructor_shouldSetAllFields() {
        // Arrange
        Map<String, String> errors = new HashMap<>();
        errors.put("field", "error");
        LocalDateTime timestamp = LocalDateTime.now();

        // Act
        ErrorResponse response = new ErrorResponse(500, "Server error", errors, timestamp, "/api/test");

        // Assert
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getMessage()).isEqualTo("Server error");
        assertThat(response.getErrors()).isEqualTo(errors);
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
        assertThat(response.getPath()).isEqualTo("/api/test");
    }

    @Test
    void setters_shouldUpdateFields() {
        // Arrange
        ErrorResponse response = new ErrorResponse();

        // Act
        response.setStatus(403);
        response.setMessage("Forbidden");
        response.setPath("/api/admin");
        response.setTimestamp(LocalDateTime.now());

        // Assert
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getMessage()).isEqualTo("Forbidden");
        assertThat(response.getPath()).isEqualTo("/api/admin");
        assertThat(response.getTimestamp()).isNotNull();
    }
}

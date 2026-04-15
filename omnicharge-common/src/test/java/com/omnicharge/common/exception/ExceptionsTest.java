package com.omnicharge.common.exception;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for all custom exception classes.
 */
class ExceptionsTest {

    @Test
    void badRequestException_shouldCreateWithMessage() {
        BadRequestException ex = new BadRequestException("Invalid input");
        assertThat(ex.getMessage()).isEqualTo("Invalid input");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void duplicateResourceException_shouldCreateWithMessage() {
        DuplicateResourceException ex = new DuplicateResourceException("Resource exists");
        assertThat(ex.getMessage()).isEqualTo("Resource exists");
    }

    @Test
    void duplicateResourceException_shouldCreateWithFormattedMessage() {
        DuplicateResourceException ex = new DuplicateResourceException("User", "email", "test@test.com");
        assertThat(ex.getMessage()).isEqualTo("User already exists with email: 'test@test.com'");
    }

    @Test
    void forbiddenException_shouldCreateWithMessage() {
        ForbiddenException ex = new ForbiddenException("Access denied");
        assertThat(ex.getMessage()).isEqualTo("Access denied");
    }

    @Test
    void resourceNotFoundException_shouldCreateWithMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
        assertThat(ex.getMessage()).isEqualTo("Not found");
    }

    @Test
    void resourceNotFoundException_shouldCreateWithFormattedMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "id", 123);
        assertThat(ex.getMessage()).isEqualTo("User not found with id: '123'");
    }

    @Test
    void serviceUnavailableException_shouldCreateWithMessage() {
        ServiceUnavailableException ex = new ServiceUnavailableException("Service down");
        assertThat(ex.getMessage()).isEqualTo("Service down");
    }

    @Test
    void unauthorizedException_shouldCreateWithMessage() {
        UnauthorizedException ex = new UnauthorizedException("Not authorized");
        assertThat(ex.getMessage()).isEqualTo("Not authorized");
    }

    @Test
    void exceptions_shouldBeThrowable() {
        assertThatThrownBy(() -> {
            throw new BadRequestException("test");
        }).isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> {
            throw new ResourceNotFoundException("test");
        }).isInstanceOf(ResourceNotFoundException.class);
    }
}

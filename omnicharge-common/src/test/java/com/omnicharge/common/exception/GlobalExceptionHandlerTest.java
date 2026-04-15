package com.omnicharge.common.exception;

import com.omnicharge.common.dto.ErrorResponse;
import com.omnicharge.common.logging.LogEventPublisher;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private LogEventPublisher logEventPublisher;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void handleResourceNotFoundException_shouldReturn404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");
        
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFoundException(ex, request);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("User not found");
    }

    @Test
    void handleBadRequestException_shouldReturn400() {
        BadRequestException ex = new BadRequestException("Invalid input");
        
        ResponseEntity<ErrorResponse> response = handler.handleBadRequestException(ex, request);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    void handleUnauthorizedException_shouldReturn401() {
        UnauthorizedException ex = new UnauthorizedException("Not authorized");
        
        ResponseEntity<ErrorResponse> response = handler.handleUnauthorizedException(ex, request);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getStatus()).isEqualTo(401);
    }

    @Test
    void handleForbiddenException_shouldReturn403() {
        ForbiddenException ex = new ForbiddenException("Access denied");
        
        ResponseEntity<ErrorResponse> response = handler.handleForbiddenException(ex, request);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getStatus()).isEqualTo(403);
    }

    @Test
    void handleDuplicateResourceException_shouldReturn409() {
        DuplicateResourceException ex = new DuplicateResourceException("Resource exists");
        
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateResourceException(ex, request);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getStatus()).isEqualTo(409);
    }

    @Test
    void handleServiceUnavailableException_shouldReturn503() {
        ServiceUnavailableException ex = new ServiceUnavailableException("Service down");
        
        ResponseEntity<ErrorResponse> response = handler.handleServiceUnavailableException(ex, request);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getStatus()).isEqualTo(503);
    }

    @Test
    void handleValidationException_shouldReturn400WithErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("user", "email", "Invalid email");
        
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(Collections.singletonList(fieldError));
        
        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, request);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrors()).containsKey("email");
    }

    @Test
    void handleGlobalException_shouldReturn500() {
        Exception ex = new Exception("Unexpected error");
        
        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(ex, request);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getStatus()).isEqualTo(500);
    }
}

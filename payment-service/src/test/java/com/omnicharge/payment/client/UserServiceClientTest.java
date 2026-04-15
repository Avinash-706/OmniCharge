package com.omnicharge.payment.client;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.payment.dto.UserProfileResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserServiceClientTest {

    @Test
    void testGetUserByIdFallback() {
        // Create an anonymous implementation to test the default method
        UserServiceClient client = new UserServiceClient() {
            @Override
            public ApiResponse<UserProfileResponse> getUserById(Long id) {
                throw new RuntimeException("Service unavailable");
            }
        };

        // Test fallback method
        Exception testException = new RuntimeException("Test exception");
        ApiResponse<UserProfileResponse> response = client.getUserByIdFallback(1L, testException);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("User Service temporarily unavailable", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void testGetUserByIdFallbackWithDifferentException() {
        UserServiceClient client = new UserServiceClient() {
            @Override
            public ApiResponse<UserProfileResponse> getUserById(Long id) {
                throw new RuntimeException("Service unavailable");
            }
        };

        Exception testException = new IllegalStateException("Circuit breaker open");
        ApiResponse<UserProfileResponse> response = client.getUserByIdFallback(1L, testException);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("User Service temporarily unavailable", response.getMessage());
    }

    @Test
    void testGetUserByIdFallbackWithNullException() {
        UserServiceClient client = new UserServiceClient() {
            @Override
            public ApiResponse<UserProfileResponse> getUserById(Long id) {
                throw new RuntimeException("Service unavailable");
            }
        };

        ApiResponse<UserProfileResponse> response = client.getUserByIdFallback(1L, null);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("User Service temporarily unavailable", response.getMessage());
    }

    @Test
    void testGetUserByIdFallbackWithDifferentUserId() {
        UserServiceClient client = new UserServiceClient() {
            @Override
            public ApiResponse<UserProfileResponse> getUserById(Long id) {
                throw new RuntimeException("Service unavailable");
            }
        };

        ApiResponse<UserProfileResponse> response1 = client.getUserByIdFallback(1L, new RuntimeException());
        ApiResponse<UserProfileResponse> response2 = client.getUserByIdFallback(999L, new RuntimeException());

        // Fallback returns same error message regardless of user ID
        assertEquals(response1.getMessage(), response2.getMessage());
    }

    @Test
    void testFeignClientAnnotation() {
        assertTrue(UserServiceClient.class.isAnnotationPresent(org.springframework.cloud.openfeign.FeignClient.class));
        
        org.springframework.cloud.openfeign.FeignClient annotation = 
            UserServiceClient.class.getAnnotation(org.springframework.cloud.openfeign.FeignClient.class);
        
        assertEquals("user-service", annotation.name());
    }

    @Test
    void testGetUserByIdMethodHasCircuitBreakerAnnotation() throws NoSuchMethodException {
        var method = UserServiceClient.class.getMethod("getUserById", Long.class);
        
        assertTrue(method.isAnnotationPresent(io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker.class));
        
        io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker annotation = 
            method.getAnnotation(io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker.class);
        
        assertEquals("userService", annotation.name());
        assertEquals("getUserByIdFallback", annotation.fallbackMethod());
    }

    @Test
    void testGetUserByIdMethodHasRetryAnnotation() throws NoSuchMethodException {
        var method = UserServiceClient.class.getMethod("getUserById", Long.class);
        
        assertTrue(method.isAnnotationPresent(io.github.resilience4j.retry.annotation.Retry.class));
        
        io.github.resilience4j.retry.annotation.Retry annotation = 
            method.getAnnotation(io.github.resilience4j.retry.annotation.Retry.class);
        
        assertEquals("userService", annotation.name());
    }

    @Test
    void testGetUserByIdMethodHasGetMappingAnnotation() throws NoSuchMethodException {
        var method = UserServiceClient.class.getMethod("getUserById", Long.class);
        
        assertTrue(method.isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class));
        
        org.springframework.web.bind.annotation.GetMapping annotation = 
            method.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class);
        
        assertEquals("/api/users/internal/{id}", annotation.value()[0]);
    }
}

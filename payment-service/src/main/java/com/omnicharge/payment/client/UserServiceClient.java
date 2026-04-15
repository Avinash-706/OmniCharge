package com.omnicharge.payment.client;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.payment.dto.UserProfileResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    /**
     * Get user details by ID with circuit breaker and retry
     * 
     * @CircuitBreaker: Prevents cascading failures if User Service is down
     * @Retry: Retries failed calls with exponential backoff (max 3 attempts)
     * 
     * Used for enriching Top Spenders with full name and registration date
     * 
     * Fallback: Returns null if service is unavailable after retries
     */
    @GetMapping("/api/users/internal/{id}")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    @Retry(name = "userService")
    ApiResponse<UserProfileResponse> getUserById(@PathVariable("id") Long id);
    
    /**
     * Fallback method when User Service is unavailable
     * Returns error response - Top Spenders will show email as fallback
     */
    default ApiResponse<UserProfileResponse> getUserByIdFallback(Long id, Exception e) {
        return ApiResponse.error("User Service temporarily unavailable");
    }
}

package com.omnicharge.operator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for OperatorServiceApplication.
 * 
 * Verifies that the Spring Boot application context loads successfully
 * with all required beans and configurations.
 */
@SpringBootTest
@ActiveProfiles("test")
class OperatorServiceApplicationTest {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        // If the context fails to load, this test will fail
    }
}

package com.omnicharge.gateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApiGatewayApplication.
 * 
 * Tests the main application class structure and annotations.
 */
@ExtendWith(MockitoExtension.class)
class ApiGatewayApplicationTests {

    @Test
    void mainMethod_Exists() {
        // Verify main method exists and is accessible
        assertDoesNotThrow(() -> {
            ApiGatewayApplication.class.getMethod("main", String[].class);
        });
    }

    @Test
    void applicationClass_HasSpringBootApplicationAnnotation() {
        // Verify @SpringBootApplication annotation is present
        assertTrue(ApiGatewayApplication.class.isAnnotationPresent(
                org.springframework.boot.autoconfigure.SpringBootApplication.class));
    }

    @Test
    void applicationClass_HasEnableDiscoveryClientAnnotation() {
        // Verify @EnableDiscoveryClient annotation is present
        assertTrue(ApiGatewayApplication.class.isAnnotationPresent(
                org.springframework.cloud.client.discovery.EnableDiscoveryClient.class));
    }

    @Test
    void applicationClass_IsPublic() {
        // Verify class is public
        assertTrue(java.lang.reflect.Modifier.isPublic(
                ApiGatewayApplication.class.getModifiers()));
    }

    @Test
    void mainMethod_IsPublicStatic() throws NoSuchMethodException {
        // Verify main method is public and static
        var mainMethod = ApiGatewayApplication.class.getMethod("main", String[].class);
        assertTrue(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers()));
    }

    @Test
    void mainMethod_ReturnsVoid() throws NoSuchMethodException {
        // Verify main method returns void
        var mainMethod = ApiGatewayApplication.class.getMethod("main", String[].class);
        assertEquals(void.class, mainMethod.getReturnType());
    }

    @Test
    void applicationClass_HasCorrectPackage() {
        // Verify class is in correct package
        assertEquals("com.omnicharge.gateway", ApiGatewayApplication.class.getPackageName());
    }
}

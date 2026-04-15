package com.omnicharge.notification.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    @Test
    void openApiConfig_HasOpenAPIDefinitionAnnotation() {
        OpenAPIDefinition annotation = OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);
        
        assertNotNull(annotation);
        assertEquals("Notification Service API", annotation.info().title());
        assertEquals("v1.0", annotation.info().version());
        assertEquals("Email, SMS Notifications, Plan Expiry Scheduler", annotation.info().description());
    }

    @Test
    void openApiConfig_HasSecuritySchemeAnnotation() {
        SecurityScheme annotation = OpenApiConfig.class.getAnnotation(SecurityScheme.class);
        
        assertNotNull(annotation);
        assertEquals("bearerAuth", annotation.name());
        assertEquals(io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP, annotation.type());
        assertEquals("bearer", annotation.scheme());
        assertEquals("JWT", annotation.bearerFormat());
    }

    @Test
    void openApiConfig_IsConfiguration() {
        assertTrue(OpenApiConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
    }

    @Test
    void openApiConfig_CanBeInstantiated() {
        assertDoesNotThrow(() -> new OpenApiConfig());
    }
}

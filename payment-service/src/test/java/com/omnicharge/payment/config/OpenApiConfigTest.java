package com.omnicharge.payment.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    @Test
    void testOpenApiConfigExists() {
        OpenApiConfig config = new OpenApiConfig();
        assertNotNull(config);
    }

    @Test
    void testOpenApiConfigHasOpenAPIDefinitionAnnotation() {
        assertTrue(OpenApiConfig.class.isAnnotationPresent(OpenAPIDefinition.class));
    }

    @Test
    void testOpenApiConfigHasSecuritySchemeAnnotation() {
        assertTrue(OpenApiConfig.class.isAnnotationPresent(SecurityScheme.class));
    }

    @Test
    void testOpenAPIDefinitionInfo() {
        OpenAPIDefinition annotation = OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);
        assertNotNull(annotation);
        assertEquals("Payment Service API", annotation.info().title());
        assertEquals("v1.0", annotation.info().version());
        assertEquals("Razorpay Integration, Payment Verification, Refunds", annotation.info().description());
    }

    @Test
    void testSecuritySchemeConfiguration() {
        SecurityScheme annotation = OpenApiConfig.class.getAnnotation(SecurityScheme.class);
        assertNotNull(annotation);
        assertEquals("bearerAuth", annotation.name());
        assertEquals(io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP, annotation.type());
        assertEquals("bearer", annotation.scheme());
        assertEquals("JWT", annotation.bearerFormat());
    }

    @Test
    void testConfigurationAnnotation() {
        assertTrue(OpenApiConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
    }
}

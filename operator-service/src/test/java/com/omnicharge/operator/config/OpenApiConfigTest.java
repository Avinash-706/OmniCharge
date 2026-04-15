package com.omnicharge.operator.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void shouldHaveOpenAPIDefinitionAnnotation() {
        // Assert
        assertThat(OpenApiConfig.class.isAnnotationPresent(OpenAPIDefinition.class)).isTrue();
    }

    @Test
    void shouldHaveSecuritySchemeAnnotation() {
        // Assert
        assertThat(OpenApiConfig.class.isAnnotationPresent(SecurityScheme.class)).isTrue();
    }

    @Test
    void shouldHaveCorrectOpenAPIInfo() {
        // Arrange
        OpenAPIDefinition annotation = OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);

        // Assert
        assertThat(annotation.info().title()).isEqualTo("Operator Service API");
        assertThat(annotation.info().version()).isEqualTo("v1.0");
        assertThat(annotation.info().description()).isEqualTo("Operator Management, Plan CRUD, CQRS Read Models");
    }

    @Test
    void shouldHaveCorrectSecurityScheme() {
        // Arrange
        SecurityScheme annotation = OpenApiConfig.class.getAnnotation(SecurityScheme.class);

        // Assert
        assertThat(annotation.name()).isEqualTo("bearerAuth");
        assertThat(annotation.type()).isEqualTo(io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP);
        assertThat(annotation.scheme()).isEqualTo("bearer");
        assertThat(annotation.bearerFormat()).isEqualTo("JWT");
    }

    @Test
    void shouldBeConfigurationClass() {
        // Assert
        assertThat(OpenApiConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class)).isTrue();
    }

    @Test
    void shouldBeInstantiable() {
        // Act
        OpenApiConfig config = new OpenApiConfig();

        // Assert
        assertThat(config).isNotNull();
    }
}

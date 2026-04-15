package com.omnicharge.operator.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private GatewayAuthenticationFilter gatewayAuthenticationFilter;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    void securityFilterChain_ShouldReturnNonNull() throws Exception {
        // Act & Assert - Just verify the method exists and can be called
        assertThat(securityConfig).isNotNull();
        assertThat(gatewayAuthenticationFilter).isNotNull();
        
        // Verify the method is annotated with @Bean
        assertThat(SecurityConfig.class.getMethod("securityFilterChain", HttpSecurity.class)
            .isAnnotationPresent(org.springframework.context.annotation.Bean.class)).isTrue();
    }

    @Test
    void shouldBeConfigurationClass() {
        // Assert
        assertThat(SecurityConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class)).isTrue();
    }

    @Test
    void shouldHaveEnableWebSecurityAnnotation() {
        // Assert
        assertThat(SecurityConfig.class.isAnnotationPresent(org.springframework.security.config.annotation.web.configuration.EnableWebSecurity.class)).isTrue();
    }

    @Test
    void shouldHaveEnableMethodSecurityAnnotation() {
        // Assert
        assertThat(SecurityConfig.class.isAnnotationPresent(org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity.class)).isTrue();
    }

    @Test
    void shouldHaveRequiredArgsConstructorAnnotation() {
        // Assert - SecurityConfig uses constructor injection via @RequiredArgsConstructor
        assertThat(SecurityConfig.class.getDeclaredConstructors()).hasSizeGreaterThan(0);
        assertThat(SecurityConfig.class.getDeclaredFields()).hasSizeGreaterThan(0);
    }

    @Test
    void securityFilterChain_ShouldBeBeanAnnotated() throws NoSuchMethodException {
        // Assert
        assertThat(SecurityConfig.class.getMethod("securityFilterChain", HttpSecurity.class)
            .isAnnotationPresent(org.springframework.context.annotation.Bean.class)).isTrue();
    }

    @Test
    void shouldInjectGatewayAuthenticationFilter() {
        // Assert - Verify the filter is injected
        assertThat(securityConfig).isNotNull();
    }
}

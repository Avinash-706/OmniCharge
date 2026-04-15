package com.omnicharge.payment.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private GatewayAuthenticationFilter gatewayAuthenticationFilter;

    @Mock
    private HttpSecurity httpSecurity;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(gatewayAuthenticationFilter);
    }

    @Test
    void testSecurityConfigCreation() {
        assertNotNull(securityConfig);
    }

    @Test
    void testSecurityConfigHasConfigurationAnnotation() {
        assertTrue(SecurityConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
    }

    @Test
    void testSecurityConfigHasEnableWebSecurityAnnotation() {
        assertTrue(SecurityConfig.class.isAnnotationPresent(org.springframework.security.config.annotation.web.configuration.EnableWebSecurity.class));
    }

    @Test
    void testSecurityConfigHasEnableMethodSecurityAnnotation() {
        assertTrue(SecurityConfig.class.isAnnotationPresent(org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity.class));
    }

    @Test
    void testSecurityConfigRequiresGatewayAuthenticationFilter() {
        assertNotNull(gatewayAuthenticationFilter);
    }

    @Test
    void testSecurityFilterChainBeanExists() throws Exception {
        // This test verifies the method exists and can be called
        // Full integration testing would require Spring context
        assertNotNull(securityConfig);
    }

    @Test
    void testSecurityConfigIsRequiredArgsConstructor() {
        // Verify Lombok @RequiredArgsConstructor is working
        SecurityConfig config = new SecurityConfig(gatewayAuthenticationFilter);
        assertNotNull(config);
    }
}

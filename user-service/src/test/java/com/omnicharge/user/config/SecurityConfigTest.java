package com.omnicharge.user.config;

import com.omnicharge.user.filter.GatewayAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private GatewayAuthenticationFilter gatewayAuthenticationFilter;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    void passwordEncoder_ShouldReturnBCryptPasswordEncoder() {
        // Given
        SecurityConfig config = new SecurityConfig(null);

        // When
        PasswordEncoder encoder = config.passwordEncoder();

        // Then
        assertThat(encoder).isNotNull();
        assertThat(encoder.getClass().getSimpleName()).isEqualTo("BCryptPasswordEncoder");
    }

    @Test
    void passwordEncoder_ShouldEncodePassword() {
        // Given
        SecurityConfig config = new SecurityConfig(null);
        PasswordEncoder encoder = config.passwordEncoder();
        String rawPassword = "testPassword123";

        // When
        String encodedPassword = encoder.encode(rawPassword);

        // Then
        assertThat(encodedPassword).isNotNull();
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, encodedPassword)).isTrue();
    }

    @Test
    void passwordEncoder_ShouldNotMatchWrongPassword() {
        // Given
        SecurityConfig config = new SecurityConfig(null);
        PasswordEncoder encoder = config.passwordEncoder();
        String rawPassword = "testPassword123";
        String wrongPassword = "wrongPassword";

        // When
        String encodedPassword = encoder.encode(rawPassword);

        // Then
        assertThat(encoder.matches(wrongPassword, encodedPassword)).isFalse();
    }

    @Test
    void securityConfig_ShouldHaveGatewayFilter() {
        // Then
        assertNotNull(securityConfig);
        assertNotNull(gatewayAuthenticationFilter);
    }

    @Test
    void passwordEncoder_ShouldProduceDifferentHashesForSamePassword() {
        // Given
        SecurityConfig config = new SecurityConfig(null);
        PasswordEncoder encoder = config.passwordEncoder();
        String rawPassword = "testPassword123";

        // When
        String hash1 = encoder.encode(rawPassword);
        String hash2 = encoder.encode(rawPassword);

        // Then - BCrypt uses salt, so hashes should be different
        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(encoder.matches(rawPassword, hash1)).isTrue();
        assertThat(encoder.matches(rawPassword, hash2)).isTrue();
    }
}

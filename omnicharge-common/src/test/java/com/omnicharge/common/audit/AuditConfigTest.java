package com.omnicharge.common.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AuditConfig.
 * Tests the auditor aware bean configuration and fallback behavior.
 */
@ExtendWith(MockitoExtension.class)
class AuditConfigTest {

    private final AuditConfig auditConfig = new AuditConfig();

    @Test
    void auditorAware_shouldReturnSystemWhenNoSecurityContext() {
        // Arrange
        AuditorAware<String> auditorAware = auditConfig.auditorAware();

        // Act
        Optional<String> auditor = auditorAware.getCurrentAuditor();

        // Assert
        assertThat(auditor).isPresent();
        assertThat(auditor.get()).isEqualTo("SYSTEM");
    }

    @Test
    void auditorAware_shouldReturnSystemWhenSecurityContextNotAvailable() {
        // Arrange
        AuditorAware<String> auditorAware = auditConfig.auditorAware();

        // Act
        Optional<String> auditor = auditorAware.getCurrentAuditor();

        // Assert
        // When Spring Security is not on classpath, should default to SYSTEM
        assertThat(auditor).isPresent();
        assertThat(auditor.get()).isEqualTo("SYSTEM");
    }

    @Test
    void auditorAware_shouldBeReusable() {
        // Arrange
        AuditorAware<String> auditorAware = auditConfig.auditorAware();

        // Act
        Optional<String> auditor1 = auditorAware.getCurrentAuditor();
        Optional<String> auditor2 = auditorAware.getCurrentAuditor();

        // Assert
        assertThat(auditor1).isEqualTo(auditor2);
    }

    @Test
    void auditorAware_shouldHandleMultipleCalls() {
        // Arrange
        AuditorAware<String> auditorAware = auditConfig.auditorAware();

        // Act & Assert - Multiple calls should consistently return SYSTEM
        for (int i = 0; i < 5; i++) {
            Optional<String> auditor = auditorAware.getCurrentAuditor();
            assertThat(auditor).isPresent();
            assertThat(auditor.get()).isEqualTo("SYSTEM");
        }
    }

    @Test
    void auditorAware_shouldReturnNonEmptyOptional() {
        // Arrange
        AuditorAware<String> auditorAware = auditConfig.auditorAware();

        // Act
        Optional<String> auditor = auditorAware.getCurrentAuditor();

        // Assert
        assertThat(auditor).isNotEmpty();
        assertThat(auditor.get()).isNotNull();
        assertThat(auditor.get()).isNotBlank();
    }

    @Test
    void auditorAware_shouldReturnSystemAsDefaultFallback() {
        // Arrange
        AuditorAware<String> auditorAware = auditConfig.auditorAware();

        // Act
        Optional<String> auditor = auditorAware.getCurrentAuditor();

        // Assert - Verify fallback behavior when Spring Security is not available
        assertThat(auditor).isPresent();
        assertThat(auditor.get()).isEqualTo("SYSTEM");
    }

    @Test
    void auditorAware_beanShouldNotBeNull() {
        // Act
        AuditorAware<String> auditorAware = auditConfig.auditorAware();

        // Assert
        assertThat(auditorAware).isNotNull();
    }
}

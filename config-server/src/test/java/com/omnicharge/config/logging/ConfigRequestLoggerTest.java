package com.omnicharge.config.logging;

import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConfigRequestLogger.
 * 
 * Validates that configuration requests are logged correctly
 * with proper context and event structure.
 */
@ExtendWith(MockitoExtension.class)
class ConfigRequestLoggerTest {

    @Mock
    private LogEventPublisher logEventPublisher;

    @Captor
    private ArgumentCaptor<LogEvent> logEventCaptor;

    private ConfigRequestLogger configRequestLogger;

    @BeforeEach
    void setUp() {
        configRequestLogger = new ConfigRequestLogger(logEventPublisher);
    }

    @Test
    void logConfigRequest_WithAllParameters_LogsCorrectly() {
        // Arrange
        String application = "user-service";
        String profile = "prod";
        String label = "main";

        // Act
        configRequestLogger.logConfigRequest(application, profile, label);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();

        assertEquals("config-server", logEvent.getServiceName());
        assertEquals("INFO", logEvent.getLevel());
        assertEquals("CONFIG_REQUEST", logEvent.getEventType());
        assertTrue(logEvent.getMessage().contains("user-service"));
        assertTrue(logEvent.getMessage().contains("prod"));
        assertTrue(logEvent.getMessage().contains("main"));
        
        assertNotNull(logEvent.getContext());
        assertEquals("user-service", logEvent.getContext().get("application"));
        assertEquals("prod", logEvent.getContext().get("profile"));
        assertEquals("main", logEvent.getContext().get("label"));
    }

    @Test
    void logConfigRequest_WithNullProfile_UsesDefault() {
        // Arrange
        String application = "api-gateway";
        String profile = null;
        String label = "master";

        // Act
        configRequestLogger.logConfigRequest(application, profile, label);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();

        assertEquals("default", logEvent.getContext().get("profile"));
        assertTrue(logEvent.getMessage().contains("default"));
    }

    @Test
    void logConfigRequest_WithNullLabel_UsesMaster() {
        // Arrange
        String application = "payment-service";
        String profile = "dev";
        String label = null;

        // Act
        configRequestLogger.logConfigRequest(application, profile, label);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();

        assertEquals("master", logEvent.getContext().get("label"));
        assertTrue(logEvent.getMessage().contains("master"));
    }

    @Test
    void logConfigRequest_WithBothNullProfileAndLabel_UsesDefaults() {
        // Arrange
        String application = "recharge-service";
        String profile = null;
        String label = null;

        // Act
        configRequestLogger.logConfigRequest(application, profile, label);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();

        assertEquals("default", logEvent.getContext().get("profile"));
        assertEquals("master", logEvent.getContext().get("label"));
        assertTrue(logEvent.getMessage().contains("default"));
        assertTrue(logEvent.getMessage().contains("master"));
    }

    @Test
    void logConfigRequest_VerifyLoggerClassName() {
        // Arrange
        String application = "operator-service";
        String profile = "staging";
        String label = "release";

        // Act
        configRequestLogger.logConfigRequest(application, profile, label);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();

        assertEquals(ConfigRequestLogger.class.getName(), logEvent.getLogger());
    }

    @Test
    void logConfigRequest_VerifyTimestampIsSet() {
        // Arrange
        String application = "notification-service";
        String profile = "prod";
        String label = "main";

        // Act
        configRequestLogger.logConfigRequest(application, profile, label);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();

        assertNotNull(logEvent.getTimestamp());
    }

    @Test
    void logConfigRequest_VerifyThreadNameIsSet() {
        // Arrange
        String application = "logging-service";
        String profile = "dev";
        String label = "feature";

        // Act
        configRequestLogger.logConfigRequest(application, profile, label);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();

        assertNotNull(logEvent.getThreadName());
        assertFalse(logEvent.getThreadName().isEmpty());
    }

    @Test
    void logConfigRequest_VerifyClientIpIsIncluded() {
        // Arrange
        String application = "config-server";
        String profile = "prod";
        String label = "main";

        // Act
        configRequestLogger.logConfigRequest(application, profile, label);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();

        assertNotNull(logEvent.getContext().get("clientIp"));
        // Without request context, should be "unknown"
        assertEquals("unknown", logEvent.getContext().get("clientIp"));
    }

    @Test
    void logConfigRequest_WhenPublisherThrowsException_DoesNotPropagateException() {
        // Arrange
        String application = "user-service";
        String profile = "prod";
        String label = "main";
        doThrow(new RuntimeException("RabbitMQ connection failed"))
            .when(logEventPublisher).publish(any(LogEvent.class));

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> 
            configRequestLogger.logConfigRequest(application, profile, label)
        );
    }

    @Test
    void logConfigRequest_VerifyContextContainsAllRequiredFields() {
        // Arrange
        String application = "api-gateway";
        String profile = "staging";
        String label = "hotfix";

        // Act
        configRequestLogger.logConfigRequest(application, profile, label);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();

        assertNotNull(logEvent.getContext());
        assertTrue(logEvent.getContext().containsKey("application"));
        assertTrue(logEvent.getContext().containsKey("profile"));
        assertTrue(logEvent.getContext().containsKey("label"));
        assertTrue(logEvent.getContext().containsKey("clientIp"));
    }

    @Test
    void logConfigRequest_WithSpecialCharactersInApplication_HandlesCorrectly() {
        // Arrange
        String application = "user-service-v2.0";
        String profile = "prod";
        String label = "release/1.0";

        // Act
        configRequestLogger.logConfigRequest(application, profile, label);

        // Assert
        verify(logEventPublisher).publish(logEventCaptor.capture());
        LogEvent logEvent = logEventCaptor.getValue();

        assertEquals("user-service-v2.0", logEvent.getContext().get("application"));
        assertEquals("release/1.0", logEvent.getContext().get("label"));
    }
}


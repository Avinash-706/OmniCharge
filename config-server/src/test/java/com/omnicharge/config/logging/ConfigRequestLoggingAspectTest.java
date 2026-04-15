package com.omnicharge.config.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConfigRequestLoggingAspect.
 * 
 * Validates that AOP aspect correctly intercepts config requests
 * and delegates to ConfigRequestLogger without breaking the flow.
 */
@ExtendWith(MockitoExtension.class)
class ConfigRequestLoggingAspectTest {

    @Mock
    private ConfigRequestLogger configRequestLogger;

    @Mock
    private ProceedingJoinPoint joinPoint;

    private ConfigRequestLoggingAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new ConfigRequestLoggingAspect(configRequestLogger);
    }

    @Test
    void logConfigRequest_WithAllArguments_LogsAndProceeds() throws Throwable {
        // Arrange
        Object[] args = {"user-service", "prod", "main"};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("config-response");

        // Act
        Object result = aspect.logConfigRequest(joinPoint);

        // Assert
        verify(configRequestLogger).logConfigRequest("user-service", "prod", "main");
        verify(joinPoint).proceed();
        assertEquals("config-response", result);
    }

    @Test
    void logConfigRequest_WithTwoArguments_LogsWithNullLabel() throws Throwable {
        // Arrange
        Object[] args = {"api-gateway", "dev"};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("config-response");

        // Act
        Object result = aspect.logConfigRequest(joinPoint);

        // Assert
        verify(configRequestLogger).logConfigRequest("api-gateway", "dev", null);
        verify(joinPoint).proceed();
        assertEquals("config-response", result);
    }

    @Test
    void logConfigRequest_WithOneArgument_LogsWithNullProfileAndLabel() throws Throwable {
        // Arrange
        Object[] args = {"payment-service"};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("config-response");

        // Act
        Object result = aspect.logConfigRequest(joinPoint);

        // Assert
        verify(configRequestLogger).logConfigRequest("payment-service", null, null);
        verify(joinPoint).proceed();
        assertEquals("config-response", result);
    }

    @Test
    void logConfigRequest_WithNoArguments_LogsUnknownAndProceeds() throws Throwable {
        // Arrange
        Object[] args = {};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("config-response");

        // Act
        Object result = aspect.logConfigRequest(joinPoint);

        // Assert
        verify(configRequestLogger).logConfigRequest("unknown", null, null);
        verify(joinPoint).proceed();
        assertEquals("config-response", result);
    }

    @Test
    void logConfigRequest_WhenLoggerThrowsException_StillProceeds() throws Throwable {
        // Arrange
        Object[] args = {"recharge-service", "staging", "release"};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("config-response");
        doThrow(new RuntimeException("Logging failed")).when(configRequestLogger)
            .logConfigRequest(anyString(), anyString(), anyString());

        // Act
        Object result = aspect.logConfigRequest(joinPoint);

        // Assert - Should still proceed despite logging error
        verify(joinPoint).proceed();
        assertEquals("config-response", result);
    }

    @Test
    void logConfigRequest_WhenJoinPointThrowsException_PropagatesException() throws Throwable {
        // Arrange
        Object[] args = {"notification-service", "prod", "main"};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Config serving failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> aspect.logConfigRequest(joinPoint));
        verify(configRequestLogger).logConfigRequest("notification-service", "prod", "main");
    }

    @Test
    void logConfigRequest_WithNullArguments_HandlesGracefully() throws Throwable {
        // Arrange
        Object[] args = {null, null, null};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("config-response");

        // Act
        Object result = aspect.logConfigRequest(joinPoint);

        // Assert
        verify(configRequestLogger).logConfigRequest("null", "null", "null");
        verify(joinPoint).proceed();
        assertEquals("config-response", result);
    }

    @Test
    void logConfigRequest_WithMixedNullArguments_HandlesCorrectly() throws Throwable {
        // Arrange
        Object[] args = {"operator-service", null, "feature-branch"};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("config-response");

        // Act
        Object result = aspect.logConfigRequest(joinPoint);

        // Assert
        verify(configRequestLogger).logConfigRequest("operator-service", "null", "feature-branch");
        verify(joinPoint).proceed();
        assertEquals("config-response", result);
    }

    @Test
    void logConfigRequest_VerifyAspectDoesNotBlockConfigServing() throws Throwable {
        // Arrange
        Object[] args = {"logging-service", "prod", "main"};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("config-response");

        // Act
        long startTime = System.currentTimeMillis();
        Object result = aspect.logConfigRequest(joinPoint);
        long endTime = System.currentTimeMillis();

        // Assert - Aspect should add minimal overhead (< 100ms)
        assertTrue(endTime - startTime < 100, "Aspect should not add significant overhead");
        assertEquals("config-response", result);
    }
}

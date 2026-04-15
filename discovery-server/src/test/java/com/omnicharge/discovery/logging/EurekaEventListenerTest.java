package com.omnicharge.discovery.logging;

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.netflix.eureka.server.event.*;

import static org.mockito.Mockito.*;

/**
 * Unit tests for EurekaEventListener.
 * 
 * Validates that Eureka server events are properly handled and logged
 * through the ServiceRegistrationLogger.
 */
@ExtendWith(MockitoExtension.class)
class EurekaEventListenerTest {

    @Mock
    private ServiceRegistrationLogger serviceRegistrationLogger;

    @Mock
    private InstanceInfo instanceInfo;

    private EurekaEventListener eurekaEventListener;

    @BeforeEach
    void setUp() {
        eurekaEventListener = new EurekaEventListener(serviceRegistrationLogger);
    }

    @Test
    void handleInstanceRegistered_WithValidEvent_LogsRegistration() {
        // Arrange
        when(instanceInfo.getAppName()).thenReturn("USER-SERVICE");
        when(instanceInfo.getInstanceId()).thenReturn("user-service-instance-1");
        when(instanceInfo.getStatus()).thenReturn(InstanceInfo.InstanceStatus.UP);
        
        EurekaInstanceRegisteredEvent event = new EurekaInstanceRegisteredEvent(
                this, instanceInfo, 90, false
        );

        // Act
        eurekaEventListener.handleInstanceRegistered(event);

        // Assert
        verify(serviceRegistrationLogger).logServiceRegistration(
                "user-service",
                "user-service-instance-1",
                "UP"
        );
    }

    @Test
    void handleInstanceRegistered_WithException_DoesNotThrow() {
        // Arrange
        when(instanceInfo.getAppName()).thenThrow(new RuntimeException("Test exception"));
        
        EurekaInstanceRegisteredEvent event = new EurekaInstanceRegisteredEvent(
                this, instanceInfo, 90, false
        );

        // Act & Assert - should not throw
        eurekaEventListener.handleInstanceRegistered(event);
        
        verify(serviceRegistrationLogger, never()).logServiceRegistration(anyString(), anyString(), anyString());
    }

    @Test
    void handleInstanceCanceled_WithValidEvent_LogsFailure() {
        // Arrange
        EurekaInstanceCanceledEvent event = new EurekaInstanceCanceledEvent(
                this, "PAYMENT-SERVICE", "payment-service-instance-2", false
        );

        // Act
        eurekaEventListener.handleInstanceCanceled(event);

        // Assert
        verify(serviceRegistrationLogger).logServiceFailure(
                "payment-service",
                "payment-service-instance-2",
                "Instance cancelled"
        );
    }

    @Test
    void handleInstanceCanceled_WithException_DoesNotThrow() {
        // Arrange
        EurekaInstanceCanceledEvent event = mock(EurekaInstanceCanceledEvent.class);
        when(event.getAppName()).thenThrow(new RuntimeException("Test exception"));

        // Act & Assert - should not throw
        eurekaEventListener.handleInstanceCanceled(event);
        
        verify(serviceRegistrationLogger, never()).logServiceFailure(anyString(), anyString(), anyString());
    }

    @Test
    void handleInstanceRenewed_DoesNotLogSuccessfulRenewals() {
        // Arrange
        EurekaInstanceRenewedEvent event = new EurekaInstanceRenewedEvent(
                this, "recharge-service", "recharge-service-instance-3", instanceInfo, false
        );

        // Act
        eurekaEventListener.handleInstanceRenewed(event);

        // Assert - should not log anything (too verbose)
        verifyNoInteractions(serviceRegistrationLogger);
    }

    @Test
    void handleRegistryAvailable_LogsSuccessfully() {
        // Arrange
        EurekaRegistryAvailableEvent event = mock(EurekaRegistryAvailableEvent.class);

        // Act & Assert - should not throw
        eurekaEventListener.handleRegistryAvailable(event);
        
        // No interactions with serviceRegistrationLogger expected (uses log.info directly)
        verifyNoInteractions(serviceRegistrationLogger);
    }

    @Test
    void handleServerStarted_LogsSuccessfully() {
        // Arrange
        EurekaServerStartedEvent event = mock(EurekaServerStartedEvent.class);

        // Act & Assert - should not throw
        eurekaEventListener.handleServerStarted(event);
        
        // No interactions with serviceRegistrationLogger expected (uses log.info directly)
        verifyNoInteractions(serviceRegistrationLogger);
    }

    @Test
    void handleInstanceRegistered_WithMixedCaseAppName_ConvertsToLowerCase() {
        // Arrange
        when(instanceInfo.getAppName()).thenReturn("OpErAtOr-SeRvIcE");
        when(instanceInfo.getInstanceId()).thenReturn("operator-service-instance-1");
        when(instanceInfo.getStatus()).thenReturn(InstanceInfo.InstanceStatus.STARTING);
        
        EurekaInstanceRegisteredEvent event = new EurekaInstanceRegisteredEvent(
                this, instanceInfo, 90, false
        );

        // Act
        eurekaEventListener.handleInstanceRegistered(event);

        // Assert
        verify(serviceRegistrationLogger).logServiceRegistration(
                "operator-service",
                "operator-service-instance-1",
                "STARTING"
        );
    }

    @Test
    void handleInstanceCanceled_WithMixedCaseAppName_ConvertsToLowerCase() {
        // Arrange
        EurekaInstanceCanceledEvent event = new EurekaInstanceCanceledEvent(
                this, "NoTiFiCaTiOn-SeRvIcE", "notification-service-instance-1", false
        );

        // Act
        eurekaEventListener.handleInstanceCanceled(event);

        // Assert
        verify(serviceRegistrationLogger).logServiceFailure(
                "notification-service",
                "notification-service-instance-1",
                "Instance cancelled"
        );
    }
}

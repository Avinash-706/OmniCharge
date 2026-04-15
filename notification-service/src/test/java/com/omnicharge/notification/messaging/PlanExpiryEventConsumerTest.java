package com.omnicharge.notification.messaging;

import com.omnicharge.common.event.RechargeCompletedEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.notification.entity.NotificationCategory;
import com.omnicharge.notification.service.IEmailService;
import com.omnicharge.notification.service.INotificationService;
import com.omnicharge.notification.service.ISmsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanExpiryEventConsumerTest {

    @Mock
    private IEmailService emailService;

    @Mock
    private ISmsService smsService;

    @Mock
    private INotificationService notificationService;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private PlanExpiryEventConsumer planExpiryEventConsumer;

    @Test
    void testHandlePlanExpired_WithEmailAndMobile() {
        // Arrange
        RechargeCompletedEvent event = createRechargeEvent("user@example.com", "+919876543210");

        // Act
        planExpiryEventConsumer.handlePlanExpired(event);

        // Assert - Email sent
        verify(emailService, times(1)).sendPlanExpiredNotification(
                eq("user@example.com"),
                eq("User"),
                eq("Airtel"),
                eq("Unlimited Plan"),
                eq("9876543210")
        );

        // Assert - Email notification saved
        verify(notificationService, times(1)).createAndSendEmail(
                eq(100L),
                eq("user@example.com"),
                contains("Plan Expired"),
                anyString(),
                eq(NotificationCategory.PLAN_EXPIRED),
                eq("RCH-123")
        );

        // Assert - SMS notification saved
        verify(notificationService, times(1)).createAndSendSms(
                eq(100L),
                eq("+919876543210"),
                contains("expired"),
                eq(NotificationCategory.PLAN_EXPIRED),
                eq("RCH-123")
        );

        // Assert - Log event published
        verify(logEventPublisher, times(1)).publish(any());
    }

    @Test
    void testHandlePlanExpired_EmailOnly() {
        // Arrange
        RechargeCompletedEvent event = createRechargeEvent("user@example.com", null);

        // Act
        planExpiryEventConsumer.handlePlanExpired(event);

        // Assert - Email sent
        verify(emailService, times(1)).sendPlanExpiredNotification(anyString(), anyString(), anyString(), anyString(), anyString());

        // Assert - Email notification saved
        verify(notificationService, times(1)).createAndSendEmail(anyLong(), anyString(), anyString(), anyString(), any(), anyString());

        // Assert - SMS notification NOT saved (no mobile)
        verify(notificationService, never()).createAndSendSms(anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void testHandlePlanExpired_MobileOnly() {
        // Arrange
        RechargeCompletedEvent event = createRechargeEvent(null, "+919876543210");

        // Act
        planExpiryEventConsumer.handlePlanExpired(event);

        // Assert - Email NOT sent (no email)
        verify(emailService, never()).sendPlanExpiredNotification(anyString(), anyString(), anyString(), anyString(), anyString());

        // Assert - Email notification NOT saved
        verify(notificationService, never()).createAndSendEmail(anyLong(), anyString(), anyString(), anyString(), any(), anyString());

        // Assert - SMS notification saved
        verify(notificationService, times(1)).createAndSendSms(anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void testHandlePlanExpired_EmailServiceThrowsException() {
        // Arrange
        RechargeCompletedEvent event = createRechargeEvent("user@example.com", "+919876543210");
        doThrow(new RuntimeException("Email service down")).when(emailService).sendPlanExpiredNotification(anyString(), anyString(), anyString(), anyString(), anyString());

        // Act - should not throw exception (error is logged)
        planExpiryEventConsumer.handlePlanExpired(event);

        // Assert - Email service was called
        verify(emailService, times(1)).sendPlanExpiredNotification(anyString(), anyString(), anyString(), anyString(), anyString());

        // Assert - SMS notification still saved
        verify(notificationService, times(1)).createAndSendSms(anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void testHandlePlanExpired_NotificationServiceThrowsException() {
        // Arrange
        RechargeCompletedEvent event = createRechargeEvent("user@example.com", "+919876543210");
        doThrow(new RuntimeException("Database down")).when(notificationService).createAndSendEmail(anyLong(), anyString(), anyString(), anyString(), any(), anyString());

        // Act - should not throw exception (error is logged)
        planExpiryEventConsumer.handlePlanExpired(event);

        // Assert - Email service was called
        verify(emailService, times(1)).sendPlanExpiredNotification(anyString(), anyString(), anyString(), anyString(), anyString());

        // Assert - SMS notification still attempted
        verify(notificationService, times(1)).createAndSendSms(anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void testHandlePlanExpired_EmptyEmail() {
        // Arrange
        RechargeCompletedEvent event = createRechargeEvent("", "+919876543210");

        // Act
        planExpiryEventConsumer.handlePlanExpired(event);

        // Assert - Email NOT sent (empty email)
        verify(emailService, never()).sendPlanExpiredNotification(anyString(), anyString(), anyString(), anyString(), anyString());

        // Assert - SMS notification saved
        verify(notificationService, times(1)).createAndSendSms(anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void testHandlePlanExpired_EmptyMobile() {
        // Arrange
        RechargeCompletedEvent event = createRechargeEvent("user@example.com", "");

        // Act
        planExpiryEventConsumer.handlePlanExpired(event);

        // Assert - Email sent
        verify(emailService, times(1)).sendPlanExpiredNotification(anyString(), anyString(), anyString(), anyString(), anyString());

        // Assert - SMS notification NOT saved (empty mobile)
        verify(notificationService, never()).createAndSendSms(anyLong(), anyString(), anyString(), any(), anyString());
    }

    private RechargeCompletedEvent createRechargeEvent(String email, String mobile) {
        RechargeCompletedEvent event = new RechargeCompletedEvent();
        event.setRechargeId("RCH-123");
        event.setUserId(100L);
        event.setUserEmail(email);
        event.setUserMobile(mobile);
        event.setMobileNumber("9876543210");
        event.setOperatorName("Airtel");
        event.setPlanName("Unlimited Plan");
        event.setAmount(BigDecimal.valueOf(399));
        event.setStatus("SUCCESS");
        event.setTimestamp(LocalDateTime.now());
        return event;
    }
}

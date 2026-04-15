package com.omnicharge.notification.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void testNotificationCreation() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(100L);
        notification.setUserEmail("user@example.com");
        notification.setUserMobile("+919876543210");
        notification.setType(NotificationType.EMAIL);
        notification.setCategory(NotificationCategory.PAYMENT_SUCCESS);
        notification.setSubject("Payment Successful");
        notification.setMessage("Your payment was successful");
        notification.setStatus(NotificationStatus.SENT);
        notification.setReferenceId("TXN-123");
        notification.setIsRead(false);

        assertEquals(1L, notification.getId());
        assertEquals(100L, notification.getUserId());
        assertEquals("user@example.com", notification.getUserEmail());
        assertEquals("+919876543210", notification.getUserMobile());
        assertEquals(NotificationType.EMAIL, notification.getType());
        assertEquals(NotificationCategory.PAYMENT_SUCCESS, notification.getCategory());
        assertEquals("Payment Successful", notification.getSubject());
        assertEquals("Your payment was successful", notification.getMessage());
        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals("TXN-123", notification.getReferenceId());
        assertFalse(notification.getIsRead());
    }

    @Test
    void testNotificationAllArgsConstructor() {
        Notification notification = new Notification(
                1L, 100L, "user@example.com", "+919876543210",
                NotificationType.SMS, NotificationCategory.PLAN_EXPIRED,
                "Plan Expired", "Your plan has expired",
                NotificationStatus.SENT, "RCH-456", true
        );

        assertEquals(1L, notification.getId());
        assertEquals(100L, notification.getUserId());
        assertEquals("user@example.com", notification.getUserEmail());
        assertEquals("+919876543210", notification.getUserMobile());
        assertEquals(NotificationType.SMS, notification.getType());
        assertEquals(NotificationCategory.PLAN_EXPIRED, notification.getCategory());
        assertEquals("Plan Expired", notification.getSubject());
        assertEquals("Your plan has expired", notification.getMessage());
        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals("RCH-456", notification.getReferenceId());
        assertTrue(notification.getIsRead());
    }

    @Test
    void testNotificationNoArgsConstructor() {
        Notification notification = new Notification();
        assertNull(notification.getId());
        assertNull(notification.getUserId());
        assertNull(notification.getUserEmail());
        assertNull(notification.getUserMobile());
        assertNull(notification.getType());
        assertNull(notification.getCategory());
        assertNull(notification.getSubject());
        assertNull(notification.getMessage());
        assertNull(notification.getStatus());
        assertNull(notification.getReferenceId());
        // isRead has default value of false
        assertEquals(false, notification.getIsRead());
    }

    @Test
    void testNotificationToString() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(100L);
        notification.setUserEmail("user@example.com");

        String toString = notification.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("Notification"));
    }
}

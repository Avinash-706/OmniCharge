package com.omnicharge.notification.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTypeTest {

    @Test
    void testNotificationTypeValues() {
        NotificationType[] types = NotificationType.values();
        assertEquals(2, types.length);
        
        assertEquals(NotificationType.EMAIL, types[0]);
        assertEquals(NotificationType.SMS, types[1]);
    }

    @Test
    void testNotificationTypeValueOf() {
        assertEquals(NotificationType.EMAIL, NotificationType.valueOf("EMAIL"));
        assertEquals(NotificationType.SMS, NotificationType.valueOf("SMS"));
    }

    @Test
    void testNotificationTypeInvalidValueOf() {
        assertThrows(IllegalArgumentException.class, () -> {
            NotificationType.valueOf("INVALID_TYPE");
        });
    }
}

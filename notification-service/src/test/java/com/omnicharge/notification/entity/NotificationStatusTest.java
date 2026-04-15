package com.omnicharge.notification.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationStatusTest {

    @Test
    void testNotificationStatusValues() {
        NotificationStatus[] statuses = NotificationStatus.values();
        assertEquals(3, statuses.length);
        
        assertEquals(NotificationStatus.PENDING, statuses[0]);
        assertEquals(NotificationStatus.SENT, statuses[1]);
        assertEquals(NotificationStatus.FAILED, statuses[2]);
    }

    @Test
    void testNotificationStatusValueOf() {
        assertEquals(NotificationStatus.PENDING, NotificationStatus.valueOf("PENDING"));
        assertEquals(NotificationStatus.SENT, NotificationStatus.valueOf("SENT"));
        assertEquals(NotificationStatus.FAILED, NotificationStatus.valueOf("FAILED"));
    }

    @Test
    void testNotificationStatusInvalidValueOf() {
        assertThrows(IllegalArgumentException.class, () -> {
            NotificationStatus.valueOf("INVALID_STATUS");
        });
    }
}

package com.omnicharge.notification.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationPriorityTest {

    @Test
    void testNotificationPriorityValues() {
        NotificationPriority[] priorities = NotificationPriority.values();
        assertEquals(4, priorities.length);
        
        assertEquals(NotificationPriority.LOW, priorities[0]);
        assertEquals(NotificationPriority.NORMAL, priorities[1]);
        assertEquals(NotificationPriority.HIGH, priorities[2]);
        assertEquals(NotificationPriority.URGENT, priorities[3]);
    }

    @Test
    void testNotificationPriorityValueOf() {
        assertEquals(NotificationPriority.LOW, NotificationPriority.valueOf("LOW"));
        assertEquals(NotificationPriority.NORMAL, NotificationPriority.valueOf("NORMAL"));
        assertEquals(NotificationPriority.HIGH, NotificationPriority.valueOf("HIGH"));
        assertEquals(NotificationPriority.URGENT, NotificationPriority.valueOf("URGENT"));
    }

    @Test
    void testNotificationPriorityInvalidValueOf() {
        assertThrows(IllegalArgumentException.class, () -> {
            NotificationPriority.valueOf("INVALID_PRIORITY");
        });
    }
}

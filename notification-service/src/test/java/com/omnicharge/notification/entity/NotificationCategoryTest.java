package com.omnicharge.notification.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationCategoryTest {

    @Test
    void testNotificationCategoryValues() {
        NotificationCategory[] categories = NotificationCategory.values();
        assertEquals(4, categories.length);
        
        assertEquals(NotificationCategory.PAYMENT_SUCCESS, categories[0]);
        assertEquals(NotificationCategory.PAYMENT_FAILED, categories[1]);
        assertEquals(NotificationCategory.PLAN_EXPIRY_REMINDER, categories[2]);
        assertEquals(NotificationCategory.PLAN_EXPIRED, categories[3]);
    }

    @Test
    void testNotificationCategoryValueOf() {
        assertEquals(NotificationCategory.PAYMENT_SUCCESS, NotificationCategory.valueOf("PAYMENT_SUCCESS"));
        assertEquals(NotificationCategory.PAYMENT_FAILED, NotificationCategory.valueOf("PAYMENT_FAILED"));
        assertEquals(NotificationCategory.PLAN_EXPIRY_REMINDER, NotificationCategory.valueOf("PLAN_EXPIRY_REMINDER"));
        assertEquals(NotificationCategory.PLAN_EXPIRED, NotificationCategory.valueOf("PLAN_EXPIRED"));
    }

    @Test
    void testNotificationCategoryInvalidValueOf() {
        assertThrows(IllegalArgumentException.class, () -> {
            NotificationCategory.valueOf("INVALID_CATEGORY");
        });
    }
}

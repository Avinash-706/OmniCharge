package com.omnicharge.notification.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationPreferenceTest {

    @Test
    void testNotificationPreferenceCreation() {
        NotificationPreference preference = new NotificationPreference();
        preference.setId(1L);
        preference.setUserId(100L);
        preference.setCategory(NotificationCategory.PAYMENT_SUCCESS);
        preference.setEmailEnabled(true);
        preference.setSmsEnabled(false);
        preference.setIsEnabled(true);

        assertEquals(1L, preference.getId());
        assertEquals(100L, preference.getUserId());
        assertEquals(NotificationCategory.PAYMENT_SUCCESS, preference.getCategory());
        assertTrue(preference.getEmailEnabled());
        assertFalse(preference.getSmsEnabled());
        assertTrue(preference.getIsEnabled());
    }

    @Test
    void testNotificationPreferenceAllArgsConstructor() {
        NotificationPreference preference = new NotificationPreference(
                1L, 100L, NotificationCategory.PLAN_EXPIRY_REMINDER,
                true, true, true
        );

        assertEquals(1L, preference.getId());
        assertEquals(100L, preference.getUserId());
        assertEquals(NotificationCategory.PLAN_EXPIRY_REMINDER, preference.getCategory());
        assertTrue(preference.getEmailEnabled());
        assertTrue(preference.getSmsEnabled());
        assertTrue(preference.getIsEnabled());
    }

    @Test
    void testNotificationPreferenceNoArgsConstructor() {
        NotificationPreference preference = new NotificationPreference();
        assertNull(preference.getId());
        assertNull(preference.getUserId());
        assertNull(preference.getCategory());
        // Default values from entity - these are set to true by default in the entity
        assertTrue(preference.getEmailEnabled());
        assertTrue(preference.getSmsEnabled());
        assertTrue(preference.getIsEnabled());
    }

    @Test
    void testNotificationPreferenceToString() {
        NotificationPreference preference = new NotificationPreference();
        preference.setId(1L);
        preference.setUserId(100L);

        String toString = preference.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("NotificationPreference"));
    }
}

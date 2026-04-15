package com.omnicharge.notification.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTemplateTest {

    @Test
    void testNotificationTemplateCreation() {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(1L);
        template.setCategory(NotificationCategory.PAYMENT_SUCCESS);
        template.setEmailSubject("Payment Successful");
        template.setEmailBody("<html>Payment successful email body</html>");
        template.setSmsBody("Payment successful SMS");
        template.setIsActive(true);
        template.setDescription("Template for payment success notifications");

        assertEquals(1L, template.getId());
        assertEquals(NotificationCategory.PAYMENT_SUCCESS, template.getCategory());
        assertEquals("Payment Successful", template.getEmailSubject());
        assertEquals("<html>Payment successful email body</html>", template.getEmailBody());
        assertEquals("Payment successful SMS", template.getSmsBody());
        assertTrue(template.getIsActive());
        assertEquals("Template for payment success notifications", template.getDescription());
    }

    @Test
    void testNotificationTemplateAllArgsConstructor() {
        NotificationTemplate template = new NotificationTemplate(
                1L, NotificationCategory.PLAN_EXPIRED,
                "Plan Expired", "<html>Plan expired email</html>",
                "Plan expired SMS", true, "Plan expiry template"
        );

        assertEquals(1L, template.getId());
        assertEquals(NotificationCategory.PLAN_EXPIRED, template.getCategory());
        assertEquals("Plan Expired", template.getEmailSubject());
        assertEquals("<html>Plan expired email</html>", template.getEmailBody());
        assertEquals("Plan expired SMS", template.getSmsBody());
        assertTrue(template.getIsActive());
        assertEquals("Plan expiry template", template.getDescription());
    }

    @Test
    void testNotificationTemplateNoArgsConstructor() {
        NotificationTemplate template = new NotificationTemplate();
        assertNull(template.getId());
        assertNull(template.getCategory());
        assertNull(template.getEmailSubject());
        assertNull(template.getEmailBody());
        assertNull(template.getSmsBody());
        // isActive has default value of true in entity
        assertTrue(template.getIsActive());
        assertNull(template.getDescription());
    }

    @Test
    void testNotificationTemplateToString() {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(1L);
        template.setCategory(NotificationCategory.PAYMENT_SUCCESS);

        String toString = template.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("NotificationTemplate"));
    }
}

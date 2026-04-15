package com.omnicharge.notification.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminReportRequestTest {

    @Test
    void testAdminReportRequestCreation() {
        AdminReportRequest request = new AdminReportRequest();
        request.setAdminEmail("admin@example.com");
        request.setReportSubject("Monthly Report");
        request.setReportHtml("<html>Report content</html>");
        request.setPdfBase64("base64encodedpdf");

        assertEquals("admin@example.com", request.getAdminEmail());
        assertEquals("Monthly Report", request.getReportSubject());
        assertEquals("<html>Report content</html>", request.getReportHtml());
        assertEquals("base64encodedpdf", request.getPdfBase64());
    }

    @Test
    void testAdminReportRequestAllArgsConstructor() {
        AdminReportRequest request = new AdminReportRequest(
                "admin@example.com",
                "Weekly Report",
                "<html>Weekly content</html>",
                "base64pdf"
        );

        assertEquals("admin@example.com", request.getAdminEmail());
        assertEquals("Weekly Report", request.getReportSubject());
        assertEquals("<html>Weekly content</html>", request.getReportHtml());
        assertEquals("base64pdf", request.getPdfBase64());
    }

    @Test
    void testAdminReportRequestNoArgsConstructor() {
        AdminReportRequest request = new AdminReportRequest();
        assertNull(request.getAdminEmail());
        assertNull(request.getReportSubject());
        assertNull(request.getReportHtml());
        assertNull(request.getPdfBase64());
    }

    @Test
    void testAdminReportRequestEqualsAndHashCode() {
        AdminReportRequest request1 = new AdminReportRequest("admin@example.com", "Report", "<html>Content</html>", "pdf");
        AdminReportRequest request2 = new AdminReportRequest("admin@example.com", "Report", "<html>Content</html>", "pdf");

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testAdminReportRequestToString() {
        AdminReportRequest request = new AdminReportRequest("admin@example.com", "Report", "<html>Content</html>", "pdf");
        String toString = request.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("AdminReportRequest"));
    }
}

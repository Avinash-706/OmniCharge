package com.omnicharge.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.notification.dto.AdminReportRequest;
import com.omnicharge.notification.service.IEmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
@MockBean(com.omnicharge.common.logging.LogEventPublisher.class)
class AdminReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IEmailService emailService;

    @Test
    void sendReportEmail_WithValidBase64Pdf_Success() throws Exception {
        // Arrange
        String pdfContent = "This is a test PDF content";
        String base64Pdf = Base64.getEncoder().encodeToString(pdfContent.getBytes());
        
        AdminReportRequest request = new AdminReportRequest();
        request.setAdminEmail("admin@test.com");
        request.setReportSubject("Executive Report");
        request.setReportHtml("<p>Report HTML</p>");
        request.setPdfBase64(base64Pdf);

        doNothing().when(emailService).sendEmailWithAttachment(
                anyString(), anyString(), anyString(), any(byte[].class), anyString());

        // Act & Assert
        mockMvc.perform(post("/api/admin/reports/send-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Report emailed successfully to avunashdhanuka@gmail.com"));

        verify(emailService).sendEmailWithAttachment(
                eq("avunashdhanuka@gmail.com"),
                eq("Executive Report"),
                anyString(),
                any(byte[].class),
                eq("OmniCharge_Executive_Report.pdf"));
    }

    @Test
    void sendReportEmail_WithInvalidBase64_ThrowsException() throws Exception {
        // Arrange
        AdminReportRequest request = new AdminReportRequest();
        request.setAdminEmail("admin@test.com");
        request.setReportSubject("Executive Report");
        request.setReportHtml("<p>Report HTML</p>");
        request.setPdfBase64("INVALID_BASE64_STRING!!!@@@");

        // Act & Assert
        mockMvc.perform(post("/api/admin/reports/send-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());

        verify(emailService, never()).sendEmailWithAttachment(
                anyString(), anyString(), anyString(), any(byte[].class), anyString());
    }

    @Test
    void sendReportEmail_WithEmptyBase64_SendsHtmlOnly() throws Exception {
        // Arrange
        AdminReportRequest request = new AdminReportRequest();
        request.setAdminEmail("admin@test.com");
        request.setReportSubject("Executive Report");
        request.setReportHtml("<p>Report HTML</p>");
        request.setPdfBase64("");

        doNothing().when(emailService).sendGenericHtmlEmail(anyString(), anyString(), anyString());

        // Act & Assert
        mockMvc.perform(post("/api/admin/reports/send-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(emailService).sendGenericHtmlEmail(
                eq("avunashdhanuka@gmail.com"),
                eq("Executive Report"),
                eq("<p>Report HTML</p>"));
        verify(emailService, never()).sendEmailWithAttachment(
                anyString(), anyString(), anyString(), any(byte[].class), anyString());
    }

    @Test
    void sendReportEmail_WithNullBase64_SendsHtmlOnly() throws Exception {
        // Arrange
        AdminReportRequest request = new AdminReportRequest();
        request.setAdminEmail("admin@test.com");
        request.setReportSubject("Executive Report");
        request.setReportHtml("<p>Report HTML</p>");
        request.setPdfBase64(null);

        doNothing().when(emailService).sendGenericHtmlEmail(anyString(), anyString(), anyString());

        // Act & Assert
        mockMvc.perform(post("/api/admin/reports/send-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(emailService).sendGenericHtmlEmail(
                eq("avunashdhanuka@gmail.com"),
                eq("Executive Report"),
                eq("<p>Report HTML</p>"));
    }

    @Test
    void sendReportEmail_WithDefaultSubject_UsesDefaultSubject() throws Exception {
        // Arrange
        AdminReportRequest request = new AdminReportRequest();
        request.setAdminEmail("admin@test.com");
        request.setReportSubject(null);
        request.setReportHtml("<p>Report HTML</p>");
        request.setPdfBase64(null);

        doNothing().when(emailService).sendGenericHtmlEmail(anyString(), anyString(), anyString());

        // Act & Assert
        mockMvc.perform(post("/api/admin/reports/send-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(emailService).sendGenericHtmlEmail(
                eq("avunashdhanuka@gmail.com"),
                eq("OmniCharge Executive Summary Report"),
                eq("<p>Report HTML</p>"));
    }

    @Test
    void sendReportEmail_EmailServiceThrowsException_ReturnsError() throws Exception {
        // Arrange
        AdminReportRequest request = new AdminReportRequest();
        request.setAdminEmail("admin@test.com");
        request.setReportSubject("Executive Report");
        request.setReportHtml("<p>Report HTML</p>");
        request.setPdfBase64(null);

        doThrow(new RuntimeException("SMTP server unavailable"))
                .when(emailService).sendGenericHtmlEmail(anyString(), anyString(), anyString());

        // Act & Assert
        mockMvc.perform(post("/api/admin/reports/send-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Failed to dispatch report: SMTP server unavailable"));
    }

    @Test
    void sendReportEmail_WithLargeBase64Pdf_Success() throws Exception {
        // Arrange
        byte[] largePdfContent = new byte[1024 * 100]; // 100KB
        String base64Pdf = Base64.getEncoder().encodeToString(largePdfContent);
        
        AdminReportRequest request = new AdminReportRequest();
        request.setAdminEmail("admin@test.com");
        request.setReportSubject("Large Executive Report");
        request.setReportHtml("<p>Large Report HTML</p>");
        request.setPdfBase64(base64Pdf);

        doNothing().when(emailService).sendEmailWithAttachment(
                anyString(), anyString(), anyString(), any(byte[].class), anyString());

        // Act & Assert
        mockMvc.perform(post("/api/admin/reports/send-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(emailService).sendEmailWithAttachment(
                eq("avunashdhanuka@gmail.com"),
                eq("Large Executive Report"),
                anyString(),
                any(byte[].class),
                eq("OmniCharge_Executive_Report.pdf"));
    }

    @Test
    void sendReportEmail_WithAttachmentFailure_ReturnsError() throws Exception {
        // Arrange
        String pdfContent = "Test PDF";
        String base64Pdf = Base64.getEncoder().encodeToString(pdfContent.getBytes());
        
        AdminReportRequest request = new AdminReportRequest();
        request.setAdminEmail("admin@test.com");
        request.setReportSubject("Executive Report");
        request.setReportHtml("<p>Report HTML</p>");
        request.setPdfBase64(base64Pdf);

        doThrow(new RuntimeException("Attachment size exceeds limit"))
                .when(emailService).sendEmailWithAttachment(
                        anyString(), anyString(), anyString(), any(byte[].class), anyString());

        // Act & Assert
        mockMvc.perform(post("/api/admin/reports/send-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Failed to dispatch report: Attachment size exceeds limit"));
    }
}

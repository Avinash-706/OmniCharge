package com.omnicharge.notification.controller;

import com.omnicharge.notification.entity.Notification;
import com.omnicharge.notification.entity.NotificationCategory;
import com.omnicharge.notification.entity.NotificationStatus;
import com.omnicharge.notification.entity.NotificationType;
import com.omnicharge.notification.repository.NotificationRepository;
import com.omnicharge.notification.service.ISmsService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
@MockBean(com.omnicharge.common.logging.LogEventPublisher.class)
class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    private ISmsService smsService;

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private MimeMessage mimeMessage;

    @Test
    void testEmail_Success() throws Exception {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        mockMvc.perform(get("/api/test/email")
                        .param("toEmail", "test@example.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.message").value("Email sent successfully to test@example.com"));

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testEmail_DefaultEmail() throws Exception {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        mockMvc.perform(get("/api/test/email")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Email sent successfully to avunashdhanuka@gmail.com"));
    }

    @Test
    void testEmail_MailSenderThrowsException() throws Exception {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new org.springframework.mail.MailSendException("SMTP connection failed"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        mockMvc.perform(get("/api/test/email")
                        .param("toEmail", "test@example.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.error").value("MailSendException"));
    }

    @Test
    void testSms_Success() throws Exception {
        // Arrange
        doNothing().when(smsService).sendSms(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(get("/api/test/sms")
                        .param("toMobile", "+919876543210")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.message").value("SMS sent successfully to +919876543210"));

        verify(smsService).sendSms(eq("+919876543210"), anyString());
    }

    @Test
    void testSms_DefaultMobile() throws Exception {
        // Arrange
        doNothing().when(smsService).sendSms(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(get("/api/test/sms")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("SMS sent successfully to +919876543210"));
    }

    @Test
    void testSms_SmsServiceThrowsException() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Twilio API error"))
                .when(smsService).sendSms(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(get("/api/test/sms")
                        .param("toMobile", "+919876543210")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.error").value("RuntimeException"));
    }

    @Test
    void testDatabase_Success() throws Exception {
        // Arrange
        Notification savedNotification = new Notification();
        savedNotification.setId(1L);
        savedNotification.setUserId(999L);
        savedNotification.setUserEmail("test@test.com");
        savedNotification.setUserMobile("+919999999999");
        savedNotification.setType(NotificationType.EMAIL);
        savedNotification.setCategory(NotificationCategory.PAYMENT_SUCCESS);
        savedNotification.setSubject("Test Notification");
        savedNotification.setMessage("Test message");
        savedNotification.setStatus(NotificationStatus.SENT);
        savedNotification.setReferenceId("TEST-123");
        savedNotification.setIsRead(false);

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(notificationRepository.count()).thenReturn(10L);

        // Act & Assert
        mockMvc.perform(get("/api/test/database")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.notificationId").value(1))
                .andExpect(jsonPath("$.data.totalNotifications").value(10));

        verify(notificationRepository).save(any(Notification.class));
        verify(notificationRepository).count();
    }

    @Test
    void testDatabase_SaveFailure() throws Exception {
        // Arrange
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(get("/api/test/database")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.error").value("RuntimeException"));
    }

    @Test
    void testAll_AllComponentsSuccess() throws Exception {
        // Arrange
        Notification savedNotification = new Notification();
        savedNotification.setId(1L);
        savedNotification.setReferenceId("TEST-ALL-123");
        
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));
        doNothing().when(smsService).sendSms(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(get("/api/test/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.database").value("SUCCESS - ID: 1"))
                .andExpect(jsonPath("$.data.email").value("SUCCESS"))
                .andExpect(jsonPath("$.data.sms").value("SUCCESS"));

        verify(notificationRepository).save(any(Notification.class));
        verify(mailSender).send(any(MimeMessage.class));
        verify(smsService).sendSms(anyString(), anyString());
    }

    @Test
    void testAll_DatabaseFailure() throws Exception {
        // Arrange
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("DB error"));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));
        doNothing().when(smsService).sendSms(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(get("/api/test/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.database").value("FAILED - DB error"))
                .andExpect(jsonPath("$.data.email").value("SUCCESS"))
                .andExpect(jsonPath("$.data.sms").value("SUCCESS"));
    }

    @Test
    void testAll_EmailFailure() throws Exception {
        // Arrange
        Notification savedNotification = new Notification();
        savedNotification.setId(1L);
        
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new org.springframework.mail.MailSendException("SMTP error"))
                .when(mailSender).send(any(MimeMessage.class));
        doNothing().when(smsService).sendSms(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(get("/api/test/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.database").value("SUCCESS - ID: 1"))
                .andExpect(jsonPath("$.data.email").value("FAILED - SMTP error"))
                .andExpect(jsonPath("$.data.sms").value("SUCCESS"));
    }

    @Test
    void testAll_SmsFailure() throws Exception {
        // Arrange
        Notification savedNotification = new Notification();
        savedNotification.setId(1L);
        
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));
        doThrow(new RuntimeException("Twilio error"))
                .when(smsService).sendSms(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(get("/api/test/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.database").value("SUCCESS - ID: 1"))
                .andExpect(jsonPath("$.data.email").value("SUCCESS"))
                .andExpect(jsonPath("$.data.sms").value("FAILED - Twilio error"));
    }

    @Test
    void getNotificationCount_Success() throws Exception {
        // Arrange
        when(notificationRepository.count()).thenReturn(42L);

        // Act & Assert
        mockMvc.perform(get("/api/test/count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalNotifications").value(42));

        verify(notificationRepository).count();
    }

    @Test
    void getNotificationCount_Failure() throws Exception {
        // Arrange
        when(notificationRepository.count()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/test/count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }
}

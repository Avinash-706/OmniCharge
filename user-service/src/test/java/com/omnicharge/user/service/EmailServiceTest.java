package com.omnicharge.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void sendOtpEmail_Success() {
        String toEmail = "test@example.com";
        String otp = "123456";

        emailService.sendOtpEmail(toEmail, otp);

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void sendOtpEmail_ContainsOtpInBody() {
        String toEmail = "test@example.com";
        String otp = "654321";

        emailService.sendOtpEmail(toEmail, otp);

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void sendOtpEmail_MessagingException_ThrowsRuntimeException() {
        String toEmail = "test@example.com";
        String otp = "123456";

        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(RuntimeException.class, () -> emailService.sendOtpEmail(toEmail, otp));
    }
}

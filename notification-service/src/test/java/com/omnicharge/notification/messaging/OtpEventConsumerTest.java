package com.omnicharge.notification.messaging;

import com.omnicharge.notification.dto.OtpEvent;
import com.omnicharge.notification.service.ISmsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpEventConsumerTest {

    @Mock
    private ISmsService smsService;

    @InjectMocks
    private OtpEventConsumer otpEventConsumer;

    @Test
    void testConsumeOtpEvent_Success() {
        // Arrange
        OtpEvent event = OtpEvent.builder()
                .mobileNumber("+919876543210")
                .otp("123456")
                .userId(100L)
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        otpEventConsumer.consumeOtpEvent(event);

        // Assert
        verify(smsService, times(1)).sendSms(
                eq("+919876543210"),
                contains("123456")
        );
        verify(smsService, times(1)).sendSms(
                eq("+919876543210"),
                contains("OmniCharge")
        );
    }

    @Test
    void testConsumeOtpEvent_SmsServiceThrowsException() {
        // Arrange
        OtpEvent event = OtpEvent.builder()
                .mobileNumber("+919876543210")
                .otp("654321")
                .userId(200L)
                .timestamp(LocalDateTime.now())
                .build();

        doThrow(new RuntimeException("SMS service down")).when(smsService).sendSms(anyString(), anyString());

        // Act - should not throw exception (error is logged)
        otpEventConsumer.consumeOtpEvent(event);

        // Assert
        verify(smsService, times(1)).sendSms(anyString(), anyString());
    }

    @Test
    void testConsumeOtpEvent_MessageFormat() {
        // Arrange
        OtpEvent event = OtpEvent.builder()
                .mobileNumber("+919876543210")
                .otp("999888")
                .userId(300L)
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        otpEventConsumer.consumeOtpEvent(event);

        // Assert - verify message contains OTP and validity period
        verify(smsService, times(1)).sendSms(
                eq("+919876543210"),
                contains("999888")
        );
        verify(smsService, times(1)).sendSms(
                eq("+919876543210"),
                contains("5 minutes")
        );
    }
}

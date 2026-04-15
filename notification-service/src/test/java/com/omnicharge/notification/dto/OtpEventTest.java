package com.omnicharge.notification.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OtpEventTest {

    @Test
    void testOtpEventCreation() {
        OtpEvent event = new OtpEvent();
        event.setMobileNumber("+919876543210");
        event.setOtp("123456");
        event.setUserId(100L);
        event.setTimestamp(LocalDateTime.now());

        assertEquals("+919876543210", event.getMobileNumber());
        assertEquals("123456", event.getOtp());
        assertEquals(100L, event.getUserId());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void testOtpEventBuilder() {
        LocalDateTime now = LocalDateTime.now();
        OtpEvent event = OtpEvent.builder()
                .mobileNumber("+919876543210")
                .otp("654321")
                .userId(200L)
                .timestamp(now)
                .build();

        assertEquals("+919876543210", event.getMobileNumber());
        assertEquals("654321", event.getOtp());
        assertEquals(200L, event.getUserId());
        assertEquals(now, event.getTimestamp());
    }

    @Test
    void testOtpEventAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        OtpEvent event = new OtpEvent("+919876543210", "123456", 100L, now);

        assertEquals("+919876543210", event.getMobileNumber());
        assertEquals("123456", event.getOtp());
        assertEquals(100L, event.getUserId());
        assertEquals(now, event.getTimestamp());
    }

    @Test
    void testOtpEventNoArgsConstructor() {
        OtpEvent event = new OtpEvent();
        assertNull(event.getMobileNumber());
        assertNull(event.getOtp());
        assertNull(event.getUserId());
        // timestamp has @Builder.Default so it will be set
        assertNotNull(event.getTimestamp());
    }
}

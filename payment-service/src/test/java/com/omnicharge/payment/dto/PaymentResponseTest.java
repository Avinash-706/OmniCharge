package com.omnicharge.payment.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaymentResponseTest {

    @Test
    void testNoArgsConstructor() {
        PaymentResponse response = new PaymentResponse();
        assertNotNull(response);
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        PaymentResponse response = new PaymentResponse(
                "TXN123",
                "SUCCESS",
                "order_123",
                new BigDecimal("100.00"),
                now
        );

        assertEquals("TXN123", response.getTransactionId());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("order_123", response.getRazorpayOrderId());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals(now, response.getTimestamp());
    }

    @Test
    void testBuilder() {
        LocalDateTime now = LocalDateTime.now();
        PaymentResponse response = PaymentResponse.builder()
                .transactionId("TXN123")
                .status("SUCCESS")
                .razorpayOrderId("order_123")
                .amount(new BigDecimal("100.00"))
                .timestamp(now)
                .build();

        assertEquals("TXN123", response.getTransactionId());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("order_123", response.getRazorpayOrderId());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals(now, response.getTimestamp());
    }

    @Test
    void testGettersAndSetters() {
        LocalDateTime now = LocalDateTime.now();
        PaymentResponse response = new PaymentResponse();
        
        response.setTransactionId("TXN123");
        response.setStatus("SUCCESS");
        response.setRazorpayOrderId("order_123");
        response.setAmount(new BigDecimal("100.00"));
        response.setTimestamp(now);

        assertEquals("TXN123", response.getTransactionId());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("order_123", response.getRazorpayOrderId());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals(now, response.getTimestamp());
    }

    @Test
    void testNullValues() {
        PaymentResponse response = new PaymentResponse(null, null, null, null, null);
        
        assertNull(response.getTransactionId());
        assertNull(response.getStatus());
        assertNull(response.getRazorpayOrderId());
        assertNull(response.getAmount());
        assertNull(response.getTimestamp());
    }

    @Test
    void testFailedStatus() {
        PaymentResponse response = PaymentResponse.builder()
                .transactionId("TXN123")
                .status("FAILED")
                .amount(new BigDecimal("100.00"))
                .build();

        assertEquals("FAILED", response.getStatus());
    }

    @Test
    void testPendingStatus() {
        PaymentResponse response = PaymentResponse.builder()
                .transactionId("TXN123")
                .status("PENDING")
                .amount(new BigDecimal("100.00"))
                .build();

        assertEquals("PENDING", response.getStatus());
    }

    @Test
    void testToString() {
        PaymentResponse response = PaymentResponse.builder()
                .transactionId("TXN123")
                .status("SUCCESS")
                .amount(new BigDecimal("100.00"))
                .build();

        String toString = response.toString();
        assertTrue(toString.contains("TXN123"));
        assertTrue(toString.contains("SUCCESS"));
        assertTrue(toString.contains("100.00"));
    }
}

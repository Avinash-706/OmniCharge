package com.omnicharge.payment.dto;

import com.omnicharge.payment.entity.PaymentMethod;
import com.omnicharge.payment.entity.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionResponseTest {

    @Test
    void testBuilder() {
        LocalDateTime now = LocalDateTime.now();
        TransactionResponse response = TransactionResponse.builder()
                .id(1L)
                .transactionId("TXN123")
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod(PaymentMethod.UPI)
                .status(PaymentStatus.SUCCESS)
                .failureReason(null)
                .razorpayOrderId("order_123")
                .userEmail("user@test.com")
                .userMobile("1234567890")
                .mobileNumber("9876543210")
                .operatorName("Airtel")
                .planName("Unlimited Plan")
                .createdDate(now)
                .build();

        assertEquals(1L, response.getId());
        assertEquals("TXN123", response.getTransactionId());
        assertEquals("RECH123", response.getRechargeId());
        assertEquals(1L, response.getUserId());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals(PaymentMethod.UPI, response.getPaymentMethod());
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertNull(response.getFailureReason());
        assertEquals("order_123", response.getRazorpayOrderId());
        assertEquals("user@test.com", response.getUserEmail());
        assertEquals("1234567890", response.getUserMobile());
        assertEquals("9876543210", response.getMobileNumber());
        assertEquals("Airtel", response.getOperatorName());
        assertEquals("Unlimited Plan", response.getPlanName());
        assertEquals(now, response.getCreatedDate());
    }

    @Test
    void testFailedTransaction() {
        TransactionResponse response = TransactionResponse.builder()
                .id(1L)
                .transactionId("TXN123")
                .status(PaymentStatus.FAILED)
                .failureReason("Insufficient funds")
                .build();

        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertEquals("Insufficient funds", response.getFailureReason());
    }

    @Test
    void testPendingTransaction() {
        TransactionResponse response = TransactionResponse.builder()
                .id(1L)
                .transactionId("TXN123")
                .status(PaymentStatus.PENDING)
                .build();

        assertEquals(PaymentStatus.PENDING, response.getStatus());
    }

    @Test
    void testAllPaymentMethods() {
        for (PaymentMethod method : PaymentMethod.values()) {
            TransactionResponse response = TransactionResponse.builder()
                    .paymentMethod(method)
                    .build();
            assertEquals(method, response.getPaymentMethod());
        }
    }

    @Test
    void testNullOptionalFields() {
        TransactionResponse response = TransactionResponse.builder()
                .id(1L)
                .transactionId("TXN123")
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod(PaymentMethod.UPI)
                .status(PaymentStatus.SUCCESS)
                .failureReason(null)
                .razorpayOrderId(null)
                .userEmail(null)
                .userMobile(null)
                .mobileNumber(null)
                .operatorName(null)
                .planName(null)
                .build();

        assertNull(response.getFailureReason());
        assertNull(response.getRazorpayOrderId());
        assertNull(response.getUserEmail());
    }
}

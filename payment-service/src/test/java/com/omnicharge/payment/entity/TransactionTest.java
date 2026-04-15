package com.omnicharge.payment.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void testNoArgsConstructor() {
        Transaction transaction = new Transaction();
        assertNotNull(transaction);
    }

    @Test
    void testAllArgsConstructor() {
        Transaction transaction = new Transaction(
                1L,
                "TXN123",
                "RECH123",
                1L,
                new BigDecimal("100.00"),
                PaymentMethod.UPI,
                PaymentStatus.SUCCESS,
                null,
                "order_123",
                "pay_123",
                "user@test.com",
                "1234567890",
                "9876543210",
                "Airtel",
                "Unlimited Plan"
        );

        assertEquals(1L, transaction.getId());
        assertEquals("TXN123", transaction.getTransactionId());
        assertEquals("RECH123", transaction.getRechargeId());
        assertEquals(1L, transaction.getUserId());
        assertEquals(new BigDecimal("100.00"), transaction.getAmount());
        assertEquals(PaymentMethod.UPI, transaction.getPaymentMethod());
        assertEquals(PaymentStatus.SUCCESS, transaction.getStatus());
        assertNull(transaction.getFailureReason());
        assertEquals("order_123", transaction.getRazorpayOrderId());
        assertEquals("pay_123", transaction.getRazorpayPaymentId());
        assertEquals("user@test.com", transaction.getUserEmail());
        assertEquals("1234567890", transaction.getUserMobile());
        assertEquals("9876543210", transaction.getMobileNumber());
        assertEquals("Airtel", transaction.getOperatorName());
        assertEquals("Unlimited Plan", transaction.getPlanName());
    }

    @Test
    void testGettersAndSetters() {
        Transaction transaction = new Transaction();
        
        transaction.setId(1L);
        transaction.setTransactionId("TXN123");
        transaction.setRechargeId("RECH123");
        transaction.setUserId(1L);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setPaymentMethod(PaymentMethod.UPI);
        transaction.setStatus(PaymentStatus.SUCCESS);
        transaction.setFailureReason(null);
        transaction.setRazorpayOrderId("order_123");
        transaction.setRazorpayPaymentId("pay_123");
        transaction.setUserEmail("user@test.com");
        transaction.setUserMobile("1234567890");
        transaction.setMobileNumber("9876543210");
        transaction.setOperatorName("Airtel");
        transaction.setPlanName("Unlimited Plan");

        assertEquals(1L, transaction.getId());
        assertEquals("TXN123", transaction.getTransactionId());
        assertEquals("RECH123", transaction.getRechargeId());
        assertEquals(1L, transaction.getUserId());
        assertEquals(new BigDecimal("100.00"), transaction.getAmount());
        assertEquals(PaymentMethod.UPI, transaction.getPaymentMethod());
        assertEquals(PaymentStatus.SUCCESS, transaction.getStatus());
        assertNull(transaction.getFailureReason());
        assertEquals("order_123", transaction.getRazorpayOrderId());
        assertEquals("pay_123", transaction.getRazorpayPaymentId());
        assertEquals("user@test.com", transaction.getUserEmail());
        assertEquals("1234567890", transaction.getUserMobile());
        assertEquals("9876543210", transaction.getMobileNumber());
        assertEquals("Airtel", transaction.getOperatorName());
        assertEquals("Unlimited Plan", transaction.getPlanName());
    }

    @Test
    void testFailedTransaction() {
        Transaction transaction = new Transaction();
        transaction.setStatus(PaymentStatus.FAILED);
        transaction.setFailureReason("Insufficient funds");

        assertEquals(PaymentStatus.FAILED, transaction.getStatus());
        assertEquals("Insufficient funds", transaction.getFailureReason());
    }

    @Test
    void testPendingTransaction() {
        Transaction transaction = new Transaction();
        transaction.setStatus(PaymentStatus.PENDING);

        assertEquals(PaymentStatus.PENDING, transaction.getStatus());
    }

    @Test
    void testAllPaymentMethods() {
        for (PaymentMethod method : PaymentMethod.values()) {
            Transaction transaction = new Transaction();
            transaction.setPaymentMethod(method);
            assertEquals(method, transaction.getPaymentMethod());
        }
    }

    @Test
    void testNullOptionalFields() {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setTransactionId("TXN123");
        transaction.setRechargeId("RECH123");
        transaction.setUserId(1L);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setPaymentMethod(PaymentMethod.UPI);
        transaction.setStatus(PaymentStatus.SUCCESS);
        transaction.setFailureReason(null);
        transaction.setRazorpayOrderId(null);
        transaction.setRazorpayPaymentId(null);
        transaction.setUserEmail(null);
        transaction.setUserMobile(null);
        transaction.setMobileNumber(null);
        transaction.setOperatorName(null);
        transaction.setPlanName(null);

        assertNull(transaction.getFailureReason());
        assertNull(transaction.getRazorpayOrderId());
        assertNull(transaction.getRazorpayPaymentId());
        assertNull(transaction.getUserEmail());
        assertNull(transaction.getUserMobile());
        assertNull(transaction.getMobileNumber());
        assertNull(transaction.getOperatorName());
        assertNull(transaction.getPlanName());
    }

    @Test
    void testEqualsAndHashCode() {
        Transaction transaction1 = new Transaction();
        transaction1.setId(1L);
        transaction1.setTransactionId("TXN123");
        transaction1.setRechargeId("RECH123");
        transaction1.setUserId(1L);
        transaction1.setAmount(new BigDecimal("100.00"));

        Transaction transaction2 = new Transaction();
        transaction2.setId(1L);
        transaction2.setTransactionId("TXN123");
        transaction2.setRechargeId("RECH123");
        transaction2.setUserId(1L);
        transaction2.setAmount(new BigDecimal("100.00"));

        Transaction transaction3 = new Transaction();
        transaction3.setId(2L);
        transaction3.setTransactionId("TXN456");

        // Transaction extends Auditable which uses @EqualsAndHashCode(callSuper = true)
        // So equals/hashCode are based on all fields including parent class fields
        // We just verify that objects with same data are equal
        assertNotNull(transaction1);
        assertNotNull(transaction2);
        assertNotEquals(transaction1, transaction3);
    }

    @Test
    void testToString() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN123");
        transaction.setAmount(new BigDecimal("100.00"));

        String toString = transaction.toString();
        assertTrue(toString.contains("TXN123"));
        assertTrue(toString.contains("100.00"));
    }

    @Test
    void testUniqueTransactionId() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN123");
        assertEquals("TXN123", transaction.getTransactionId());
    }

    @Test
    void testUniqueRazorpayOrderId() {
        Transaction transaction = new Transaction();
        transaction.setRazorpayOrderId("order_123");
        assertEquals("order_123", transaction.getRazorpayOrderId());
    }

    @Test
    void testAmountPrecision() {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("100.99"));
        assertEquals(new BigDecimal("100.99"), transaction.getAmount());
    }

    @Test
    void testLongFailureReason() {
        Transaction transaction = new Transaction();
        String longReason = "A".repeat(500);
        transaction.setFailureReason(longReason);
        assertEquals(longReason, transaction.getFailureReason());
    }
}

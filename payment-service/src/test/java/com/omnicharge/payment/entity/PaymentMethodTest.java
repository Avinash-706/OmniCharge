package com.omnicharge.payment.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentMethodTest {

    @Test
    void testEnumValues() {
        PaymentMethod[] methods = PaymentMethod.values();
        assertEquals(5, methods.length);
    }

    @Test
    void testCreditCard() {
        PaymentMethod method = PaymentMethod.CREDIT_CARD;
        assertEquals("CREDIT_CARD", method.name());
    }

    @Test
    void testDebitCard() {
        PaymentMethod method = PaymentMethod.DEBIT_CARD;
        assertEquals("DEBIT_CARD", method.name());
    }

    @Test
    void testUPI() {
        PaymentMethod method = PaymentMethod.UPI;
        assertEquals("UPI", method.name());
    }

    @Test
    void testNetBanking() {
        PaymentMethod method = PaymentMethod.NET_BANKING;
        assertEquals("NET_BANKING", method.name());
    }

    @Test
    void testRazorpay() {
        PaymentMethod method = PaymentMethod.RAZORPAY;
        assertEquals("RAZORPAY", method.name());
    }

    @Test
    void testValueOf() {
        PaymentMethod method = PaymentMethod.valueOf("UPI");
        assertEquals(PaymentMethod.UPI, method);
    }

    @Test
    void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            PaymentMethod.valueOf("INVALID");
        });
    }

    @Test
    void testEnumEquality() {
        PaymentMethod method1 = PaymentMethod.UPI;
        PaymentMethod method2 = PaymentMethod.valueOf("UPI");
        assertEquals(method1, method2);
        assertSame(method1, method2);
    }

    @Test
    void testEnumOrdinal() {
        assertEquals(0, PaymentMethod.CREDIT_CARD.ordinal());
        assertEquals(1, PaymentMethod.DEBIT_CARD.ordinal());
        assertEquals(2, PaymentMethod.UPI.ordinal());
        assertEquals(3, PaymentMethod.NET_BANKING.ordinal());
        assertEquals(4, PaymentMethod.RAZORPAY.ordinal());
    }
}

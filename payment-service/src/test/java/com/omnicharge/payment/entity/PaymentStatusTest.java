package com.omnicharge.payment.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentStatusTest {

    @Test
    void testEnumValues() {
        PaymentStatus[] statuses = PaymentStatus.values();
        assertEquals(3, statuses.length);
    }

    @Test
    void testPending() {
        PaymentStatus status = PaymentStatus.PENDING;
        assertEquals("PENDING", status.name());
    }

    @Test
    void testSuccess() {
        PaymentStatus status = PaymentStatus.SUCCESS;
        assertEquals("SUCCESS", status.name());
    }

    @Test
    void testFailed() {
        PaymentStatus status = PaymentStatus.FAILED;
        assertEquals("FAILED", status.name());
    }

    @Test
    void testValueOf() {
        PaymentStatus status = PaymentStatus.valueOf("SUCCESS");
        assertEquals(PaymentStatus.SUCCESS, status);
    }

    @Test
    void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            PaymentStatus.valueOf("INVALID");
        });
    }

    @Test
    void testEnumEquality() {
        PaymentStatus status1 = PaymentStatus.SUCCESS;
        PaymentStatus status2 = PaymentStatus.valueOf("SUCCESS");
        assertEquals(status1, status2);
        assertSame(status1, status2);
    }

    @Test
    void testEnumOrdinal() {
        assertEquals(0, PaymentStatus.PENDING.ordinal());
        assertEquals(1, PaymentStatus.SUCCESS.ordinal());
        assertEquals(2, PaymentStatus.FAILED.ordinal());
    }

    @Test
    void testAllStatusesUnique() {
        PaymentStatus[] statuses = PaymentStatus.values();
        for (int i = 0; i < statuses.length; i++) {
            for (int j = i + 1; j < statuses.length; j++) {
                assertNotEquals(statuses[i], statuses[j]);
            }
        }
    }
}

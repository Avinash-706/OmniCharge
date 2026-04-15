package com.omnicharge.payment.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TransactionFrequencyStatsTest {

    @Test
    void testBuilder() {
        TransactionFrequencyStats stats = TransactionFrequencyStats.builder()
                .date("2024-01-15")
                .successCount(80L)
                .failedCount(20L)
                .successAmount(new BigDecimal("40000.00"))
                .failedAmount(new BigDecimal("10000.00"))
                .build();

        assertEquals("2024-01-15", stats.getDate());
        assertEquals(80L, stats.getSuccessCount());
        assertEquals(20L, stats.getFailedCount());
        assertEquals(new BigDecimal("40000.00"), stats.getSuccessAmount());
        assertEquals(new BigDecimal("10000.00"), stats.getFailedAmount());
    }

    @Test
    void testAllArgsConstructor() {
        TransactionFrequencyStats stats = new TransactionFrequencyStats(
                "2024-01-15", 80L, 20L, new BigDecimal("40000.00"), new BigDecimal("10000.00")
        );

        assertEquals("2024-01-15", stats.getDate());
        assertEquals(80L, stats.getSuccessCount());
        assertEquals(20L, stats.getFailedCount());
    }

    @Test
    void testNoArgsConstructor() {
        TransactionFrequencyStats stats = new TransactionFrequencyStats();
        assertNotNull(stats);
    }

    @Test
    void testZeroFailures() {
        TransactionFrequencyStats stats = TransactionFrequencyStats.builder()
                .date("2024-01-15")
                .successCount(100L)
                .failedCount(0L)
                .successAmount(new BigDecimal("50000.00"))
                .failedAmount(BigDecimal.ZERO)
                .build();

        assertEquals(0L, stats.getFailedCount());
        assertEquals(BigDecimal.ZERO, stats.getFailedAmount());
    }

    @Test
    void testAllFailures() {
        TransactionFrequencyStats stats = TransactionFrequencyStats.builder()
                .date("2024-01-15")
                .successCount(0L)
                .failedCount(100L)
                .successAmount(BigDecimal.ZERO)
                .failedAmount(new BigDecimal("50000.00"))
                .build();

        assertEquals(0L, stats.getSuccessCount());
        assertEquals(BigDecimal.ZERO, stats.getSuccessAmount());
    }
}

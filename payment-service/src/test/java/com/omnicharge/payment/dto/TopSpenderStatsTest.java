package com.omnicharge.payment.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TopSpenderStatsTest {

    @Test
    void testBuilder() {
        TopSpenderStats stats = TopSpenderStats.builder()
                .userId(1L)
                .userEmail("user@test.com")
                .userMobile("1234567890")
                .fullName("Test User")
                .registrationDate("2024-01-01")
                .transactionCount(10L)
                .successfulTransactions(8L)
                .failedTransactions(2L)
                .totalSpent(new BigDecimal("5000.00"))
                .averageTransactionValue(new BigDecimal("500.00"))
                .successRate(80.0)
                .lastTransactionDate("2024-01-15")
                .firstTransactionDate("2024-01-01")
                .build();

        assertEquals(1L, stats.getUserId());
        assertEquals("user@test.com", stats.getUserEmail());
        assertEquals("1234567890", stats.getUserMobile());
        assertEquals("Test User", stats.getFullName());
        assertEquals("2024-01-01", stats.getRegistrationDate());
        assertEquals(10L, stats.getTransactionCount());
        assertEquals(8L, stats.getSuccessfulTransactions());
        assertEquals(2L, stats.getFailedTransactions());
        assertEquals(new BigDecimal("5000.00"), stats.getTotalSpent());
        assertEquals(new BigDecimal("500.00"), stats.getAverageTransactionValue());
        assertEquals(80.0, stats.getSuccessRate());
        assertEquals("2024-01-15", stats.getLastTransactionDate());
        assertEquals("2024-01-01", stats.getFirstTransactionDate());
    }

    @Test
    void testAllArgsConstructor() {
        TopSpenderStats stats = new TopSpenderStats(
                1L, "user@test.com", "1234567890", "Test User", "2024-01-01",
                10L, 8L, 2L, new BigDecimal("5000.00"), new BigDecimal("500.00"),
                80.0, "2024-01-15", "2024-01-01"
        );

        assertEquals(1L, stats.getUserId());
        assertEquals(10L, stats.getTransactionCount());
        assertEquals(new BigDecimal("5000.00"), stats.getTotalSpent());
    }

    @Test
    void testZeroTransactions() {
        TopSpenderStats stats = TopSpenderStats.builder()
                .userId(1L)
                .transactionCount(0L)
                .successfulTransactions(0L)
                .failedTransactions(0L)
                .totalSpent(BigDecimal.ZERO)
                .successRate(0.0)
                .build();

        assertEquals(0L, stats.getTransactionCount());
        assertEquals(BigDecimal.ZERO, stats.getTotalSpent());
        assertEquals(0.0, stats.getSuccessRate());
    }

    @Test
    void testHighSuccessRate() {
        TopSpenderStats stats = TopSpenderStats.builder()
                .userId(1L)
                .transactionCount(100L)
                .successfulTransactions(100L)
                .failedTransactions(0L)
                .successRate(100.0)
                .build();

        assertEquals(100.0, stats.getSuccessRate());
    }
}

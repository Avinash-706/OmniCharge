package com.omnicharge.payment.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TopUserStatsTest {

    @Test
    void testBuilder() {
        TopUserStats stats = TopUserStats.builder()
                .userId(1L)
                .transactionCount(10L)
                .totalSpent(new BigDecimal("5000.00"))
                .build();

        assertEquals(1L, stats.getUserId());
        assertEquals(10L, stats.getTransactionCount());
        assertEquals(new BigDecimal("5000.00"), stats.getTotalSpent());
    }

    @Test
    void testAllArgsConstructor() {
        TopUserStats stats = new TopUserStats(1L, 10L, new BigDecimal("5000.00"));

        assertEquals(1L, stats.getUserId());
        assertEquals(10L, stats.getTransactionCount());
        assertEquals(new BigDecimal("5000.00"), stats.getTotalSpent());
    }

    @Test
    void testNoArgsConstructor() {
        TopUserStats stats = new TopUserStats();
        assertNotNull(stats);
    }

    @Test
    void testGettersAndSetters() {
        TopUserStats stats = new TopUserStats();
        stats.setUserId(1L);
        stats.setTransactionCount(10L);
        stats.setTotalSpent(new BigDecimal("5000.00"));

        assertEquals(1L, stats.getUserId());
        assertEquals(10L, stats.getTransactionCount());
        assertEquals(new BigDecimal("5000.00"), stats.getTotalSpent());
    }

    @Test
    void testZeroValues() {
        TopUserStats stats = TopUserStats.builder()
                .userId(1L)
                .transactionCount(0L)
                .totalSpent(BigDecimal.ZERO)
                .build();

        assertEquals(0L, stats.getTransactionCount());
        assertEquals(BigDecimal.ZERO, stats.getTotalSpent());
    }
}

package com.omnicharge.payment.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DailyRevenueStatsTest {

    @Test
    void testNoArgsConstructor() {
        DailyRevenueStats stats = new DailyRevenueStats();
        assertNotNull(stats);
    }

    @Test
    void testAllArgsConstructor() {
        DailyRevenueStats stats = new DailyRevenueStats(
                "2024-01-15",
                100L,
                new BigDecimal("5000.00")
        );

        assertEquals("2024-01-15", stats.getDate());
        assertEquals(100L, stats.getTransactionCount());
        assertEquals(new BigDecimal("5000.00"), stats.getRevenue());
    }

    @Test
    void testBuilder() {
        DailyRevenueStats stats = DailyRevenueStats.builder()
                .date("2024-01-15")
                .transactionCount(100L)
                .revenue(new BigDecimal("5000.00"))
                .build();

        assertEquals("2024-01-15", stats.getDate());
        assertEquals(100L, stats.getTransactionCount());
        assertEquals(new BigDecimal("5000.00"), stats.getRevenue());
    }

    @Test
    void testGettersAndSetters() {
        DailyRevenueStats stats = new DailyRevenueStats();
        
        stats.setDate("2024-01-15");
        stats.setTransactionCount(100L);
        stats.setRevenue(new BigDecimal("5000.00"));

        assertEquals("2024-01-15", stats.getDate());
        assertEquals(100L, stats.getTransactionCount());
        assertEquals(new BigDecimal("5000.00"), stats.getRevenue());
    }

    @Test
    void testEqualsAndHashCode() {
        DailyRevenueStats stats1 = new DailyRevenueStats("2024-01-15", 100L, new BigDecimal("5000.00"));
        DailyRevenueStats stats2 = new DailyRevenueStats("2024-01-15", 100L, new BigDecimal("5000.00"));
        DailyRevenueStats stats3 = new DailyRevenueStats("2024-01-16", 200L, new BigDecimal("10000.00"));

        assertEquals(stats1, stats2);
        assertNotEquals(stats1, stats3);
        assertEquals(stats1.hashCode(), stats2.hashCode());
    }

    @Test
    void testToString() {
        DailyRevenueStats stats = new DailyRevenueStats("2024-01-15", 100L, new BigDecimal("5000.00"));
        String toString = stats.toString();

        assertTrue(toString.contains("2024-01-15"));
        assertTrue(toString.contains("100"));
        assertTrue(toString.contains("5000.00"));
    }

    @Test
    void testNullValues() {
        DailyRevenueStats stats = new DailyRevenueStats(null, null, null);
        
        assertNull(stats.getDate());
        assertNull(stats.getTransactionCount());
        assertNull(stats.getRevenue());
    }

    @Test
    void testZeroValues() {
        DailyRevenueStats stats = new DailyRevenueStats("2024-01-15", 0L, BigDecimal.ZERO);
        
        assertEquals("2024-01-15", stats.getDate());
        assertEquals(0L, stats.getTransactionCount());
        assertEquals(BigDecimal.ZERO, stats.getRevenue());
    }

    @Test
    void testNegativeRevenue() {
        DailyRevenueStats stats = new DailyRevenueStats("2024-01-15", 100L, new BigDecimal("-100.00"));
        
        assertEquals(new BigDecimal("-100.00"), stats.getRevenue());
    }
}

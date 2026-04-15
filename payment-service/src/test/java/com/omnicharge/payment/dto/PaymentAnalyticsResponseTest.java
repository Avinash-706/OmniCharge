package com.omnicharge.payment.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentAnalyticsResponseTest {

    @Test
    void testNoArgsConstructor() {
        PaymentAnalyticsResponse response = new PaymentAnalyticsResponse();
        assertNotNull(response);
    }

    @Test
    void testAllArgsConstructor() {
        List<TopSpenderStats> topSpenders = Arrays.asList(
                new TopSpenderStats(1L, "user1@test.com", "1234567890", "User One", "2024-01-01", 10L, 8L, 2L, new BigDecimal("1000.00"), new BigDecimal("100.00"), 80.0, "2024-01-15", "2024-01-01")
        );
        List<DailyRevenueStats> dailyRevenue = Arrays.asList(
                new DailyRevenueStats("2024-01-15", 100L, new BigDecimal("5000.00"))
        );

        PaymentAnalyticsResponse response = new PaymentAnalyticsResponse(
                new BigDecimal("100000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("50000.00"),
                new BigDecimal("500.00"),
                200L,
                180L,
                15L,
                5L,
                90.0,
                7.5,
                new BigDecimal("45000.00"),
                11.11,
                topSpenders,
                dailyRevenue
        );

        assertEquals(new BigDecimal("100000.00"), response.getGrossRevenue());
        assertEquals(new BigDecimal("5000.00"), response.getTodayRevenue());
        assertEquals(new BigDecimal("50000.00"), response.getMonthRevenue());
        assertEquals(new BigDecimal("500.00"), response.getAverageTransactionValue());
        assertEquals(200L, response.getTotalTransactions());
        assertEquals(180L, response.getSuccessfulTransactions());
        assertEquals(15L, response.getFailedTransactions());
        assertEquals(5L, response.getPendingTransactions());
        assertEquals(90.0, response.getSuccessRate());
        assertEquals(7.5, response.getAbandonedCheckoutRate());
        assertEquals(new BigDecimal("45000.00"), response.getLastMonthRevenue());
        assertEquals(11.11, response.getRevenueGrowthPercentage());
        assertEquals(1, response.getTopSpenders().size());
        assertEquals(1, response.getDailyRevenue().size());
    }

    @Test
    void testBuilder() {
        List<TopSpenderStats> topSpenders = Collections.singletonList(
                TopSpenderStats.builder()
                        .userId(1L)
                        .userEmail("user1@test.com")
                        .totalSpent(new BigDecimal("1000.00"))
                        .build()
        );

        PaymentAnalyticsResponse response = PaymentAnalyticsResponse.builder()
                .grossRevenue(new BigDecimal("100000.00"))
                .todayRevenue(new BigDecimal("5000.00"))
                .totalTransactions(200L)
                .successfulTransactions(180L)
                .successRate(90.0)
                .topSpenders(topSpenders)
                .build();

        assertEquals(new BigDecimal("100000.00"), response.getGrossRevenue());
        assertEquals(new BigDecimal("5000.00"), response.getTodayRevenue());
        assertEquals(200L, response.getTotalTransactions());
        assertEquals(180L, response.getSuccessfulTransactions());
        assertEquals(90.0, response.getSuccessRate());
        assertEquals(1, response.getTopSpenders().size());
    }

    @Test
    void testGettersAndSetters() {
        PaymentAnalyticsResponse response = new PaymentAnalyticsResponse();
        
        response.setGrossRevenue(new BigDecimal("100000.00"));
        response.setTodayRevenue(new BigDecimal("5000.00"));
        response.setMonthRevenue(new BigDecimal("50000.00"));
        response.setAverageTransactionValue(new BigDecimal("500.00"));
        response.setTotalTransactions(200L);
        response.setSuccessfulTransactions(180L);
        response.setFailedTransactions(15L);
        response.setPendingTransactions(5L);
        response.setSuccessRate(90.0);
        response.setAbandonedCheckoutRate(7.5);
        response.setLastMonthRevenue(new BigDecimal("45000.00"));
        response.setRevenueGrowthPercentage(11.11);

        assertEquals(new BigDecimal("100000.00"), response.getGrossRevenue());
        assertEquals(new BigDecimal("5000.00"), response.getTodayRevenue());
        assertEquals(new BigDecimal("50000.00"), response.getMonthRevenue());
        assertEquals(new BigDecimal("500.00"), response.getAverageTransactionValue());
        assertEquals(200L, response.getTotalTransactions());
        assertEquals(180L, response.getSuccessfulTransactions());
        assertEquals(15L, response.getFailedTransactions());
        assertEquals(5L, response.getPendingTransactions());
        assertEquals(90.0, response.getSuccessRate());
        assertEquals(7.5, response.getAbandonedCheckoutRate());
        assertEquals(new BigDecimal("45000.00"), response.getLastMonthRevenue());
        assertEquals(11.11, response.getRevenueGrowthPercentage());
    }

    @Test
    void testEmptyLists() {
        PaymentAnalyticsResponse response = PaymentAnalyticsResponse.builder()
                .topSpenders(Collections.emptyList())
                .dailyRevenue(Collections.emptyList())
                .build();

        assertNotNull(response.getTopSpenders());
        assertNotNull(response.getDailyRevenue());
        assertTrue(response.getTopSpenders().isEmpty());
        assertTrue(response.getDailyRevenue().isEmpty());
    }

    @Test
    void testNullLists() {
        PaymentAnalyticsResponse response = new PaymentAnalyticsResponse();
        response.setTopSpenders(null);
        response.setDailyRevenue(null);

        assertNull(response.getTopSpenders());
        assertNull(response.getDailyRevenue());
    }

    @Test
    void testZeroValues() {
        PaymentAnalyticsResponse response = PaymentAnalyticsResponse.builder()
                .grossRevenue(BigDecimal.ZERO)
                .todayRevenue(BigDecimal.ZERO)
                .totalTransactions(0L)
                .successfulTransactions(0L)
                .failedTransactions(0L)
                .pendingTransactions(0L)
                .successRate(0.0)
                .abandonedCheckoutRate(0.0)
                .revenueGrowthPercentage(0.0)
                .build();

        assertEquals(BigDecimal.ZERO, response.getGrossRevenue());
        assertEquals(0L, response.getTotalTransactions());
        assertEquals(0.0, response.getSuccessRate());
    }

    @Test
    void testNegativeGrowth() {
        PaymentAnalyticsResponse response = PaymentAnalyticsResponse.builder()
                .revenueGrowthPercentage(-15.5)
                .build();

        assertEquals(-15.5, response.getRevenueGrowthPercentage());
    }

    @Test
    void testHighSuccessRate() {
        PaymentAnalyticsResponse response = PaymentAnalyticsResponse.builder()
                .successRate(100.0)
                .build();

        assertEquals(100.0, response.getSuccessRate());
    }

    @Test
    void testToString() {
        PaymentAnalyticsResponse response = PaymentAnalyticsResponse.builder()
                .grossRevenue(new BigDecimal("100000.00"))
                .totalTransactions(200L)
                .build();

        String toString = response.toString();
        assertTrue(toString.contains("100000.00"));
        assertTrue(toString.contains("200"));
    }
}

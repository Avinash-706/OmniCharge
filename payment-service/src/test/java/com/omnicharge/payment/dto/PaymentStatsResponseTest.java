package com.omnicharge.payment.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class PaymentStatsResponseTest {

    @Test
    void testBuilder() {
        PaymentStatsResponse response = PaymentStatsResponse.builder()
                .totalTransactions(200L)
                .successfulTransactions(180L)
                .failedTransactions(15L)
                .pendingTransactions(5L)
                .totalRevenue(new BigDecimal("100000.00"))
                .successAmount(new BigDecimal("90000.00"))
                .failedAmount(new BigDecimal("7500.00"))
                .averageTransactionAmount(new BigDecimal("500.00"))
                .todayTransactions(50L)
                .todayRevenue(new BigDecimal("25000.00"))
                .revenueByDate(Arrays.asList(new DailyRevenueStats("2024-01-15", 50L, new BigDecimal("25000.00"))))
                .topUsers(Arrays.asList(new TopUserStats(1L, 10L, new BigDecimal("5000.00"))))
                .build();

        assertEquals(200L, response.getTotalTransactions());
        assertEquals(180L, response.getSuccessfulTransactions());
        assertEquals(15L, response.getFailedTransactions());
        assertEquals(5L, response.getPendingTransactions());
        assertEquals(new BigDecimal("100000.00"), response.getTotalRevenue());
        assertEquals(new BigDecimal("90000.00"), response.getSuccessAmount());
        assertEquals(new BigDecimal("7500.00"), response.getFailedAmount());
        assertEquals(new BigDecimal("500.00"), response.getAverageTransactionAmount());
        assertEquals(50L, response.getTodayTransactions());
        assertEquals(new BigDecimal("25000.00"), response.getTodayRevenue());
        assertEquals(1, response.getRevenueByDate().size());
        assertEquals(1, response.getTopUsers().size());
    }

    @Test
    void testEmptyLists() {
        PaymentStatsResponse response = PaymentStatsResponse.builder()
                .revenueByDate(Collections.emptyList())
                .topUsers(Collections.emptyList())
                .build();

        assertNotNull(response.getRevenueByDate());
        assertNotNull(response.getTopUsers());
        assertTrue(response.getRevenueByDate().isEmpty());
        assertTrue(response.getTopUsers().isEmpty());
    }

    @Test
    void testZeroValues() {
        PaymentStatsResponse response = PaymentStatsResponse.builder()
                .totalTransactions(0L)
                .successfulTransactions(0L)
                .failedTransactions(0L)
                .pendingTransactions(0L)
                .totalRevenue(BigDecimal.ZERO)
                .build();

        assertEquals(0L, response.getTotalTransactions());
        assertEquals(BigDecimal.ZERO, response.getTotalRevenue());
    }
}

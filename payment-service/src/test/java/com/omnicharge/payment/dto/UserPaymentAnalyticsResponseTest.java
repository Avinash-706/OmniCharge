package com.omnicharge.payment.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class UserPaymentAnalyticsResponseTest {

    @Test
    void testBuilder() {
        UserPaymentAnalyticsResponse response = UserPaymentAnalyticsResponse.builder()
                .userId(1L)
                .userEmail("user@test.com")
                .userMobile("1234567890")
                .fullName("Test User")
                .registrationDate("2024-01-01")
                .totalTransactions(100L)
                .successfulTransactions(90L)
                .failedTransactions(8L)
                .pendingTransactions(2L)
                .successRate(90.0)
                .totalSpent(new BigDecimal("50000.00"))
                .averageTransactionValue(new BigDecimal("500.00"))
                .highestTransaction(new BigDecimal("2000.00"))
                .lowestTransaction(new BigDecimal("10.00"))
                .firstTransactionDate("2024-01-01")
                .lastTransactionDate("2024-01-15")
                .transactionFrequency(Arrays.asList(
                        new TransactionFrequencyStats("2024-01-15", 10L, 2L, new BigDecimal("5000.00"), new BigDecimal("1000.00"))
                ))
                .build();

        assertEquals(1L, response.getUserId());
        assertEquals("user@test.com", response.getUserEmail());
        assertEquals("1234567890", response.getUserMobile());
        assertEquals("Test User", response.getFullName());
        assertEquals("2024-01-01", response.getRegistrationDate());
        assertEquals(100L, response.getTotalTransactions());
        assertEquals(90L, response.getSuccessfulTransactions());
        assertEquals(8L, response.getFailedTransactions());
        assertEquals(2L, response.getPendingTransactions());
        assertEquals(90.0, response.getSuccessRate());
        assertEquals(new BigDecimal("50000.00"), response.getTotalSpent());
        assertEquals(new BigDecimal("500.00"), response.getAverageTransactionValue());
        assertEquals(new BigDecimal("2000.00"), response.getHighestTransaction());
        assertEquals(new BigDecimal("10.00"), response.getLowestTransaction());
        assertEquals("2024-01-01", response.getFirstTransactionDate());
        assertEquals("2024-01-15", response.getLastTransactionDate());
        assertEquals(1, response.getTransactionFrequency().size());
    }

    @Test
    void testEmptyTransactionFrequency() {
        UserPaymentAnalyticsResponse response = UserPaymentAnalyticsResponse.builder()
                .userId(1L)
                .transactionFrequency(Collections.emptyList())
                .build();

        assertNotNull(response.getTransactionFrequency());
        assertTrue(response.getTransactionFrequency().isEmpty());
    }

    @Test
    void testZeroTransactions() {
        UserPaymentAnalyticsResponse response = UserPaymentAnalyticsResponse.builder()
                .userId(1L)
                .totalTransactions(0L)
                .successfulTransactions(0L)
                .failedTransactions(0L)
                .pendingTransactions(0L)
                .successRate(0.0)
                .totalSpent(BigDecimal.ZERO)
                .build();

        assertEquals(0L, response.getTotalTransactions());
        assertEquals(BigDecimal.ZERO, response.getTotalSpent());
        assertEquals(0.0, response.getSuccessRate());
    }

    @Test
    void testHighSuccessRate() {
        UserPaymentAnalyticsResponse response = UserPaymentAnalyticsResponse.builder()
                .userId(1L)
                .totalTransactions(100L)
                .successfulTransactions(100L)
                .failedTransactions(0L)
                .successRate(100.0)
                .build();

        assertEquals(100.0, response.getSuccessRate());
    }
}

package com.omnicharge.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPaymentAnalyticsResponse {
    // User Profile
    private Long userId;
    private String userEmail;
    private String userMobile;
    private String fullName;
    private String registrationDate;
    
    // Transaction Stats
    private Long totalTransactions;
    private Long successfulTransactions;
    private Long failedTransactions;
    private Long pendingTransactions;
    private Double successRate;
    
    // Financial Stats
    private BigDecimal totalSpent;
    private BigDecimal averageTransactionValue;
    private BigDecimal highestTransaction;
    private BigDecimal lowestTransaction;
    
    // Time-based Stats
    private String firstTransactionDate;
    private String lastTransactionDate;
    
    // Transaction Frequency (for charts)
    private List<TransactionFrequencyStats> transactionFrequency;
}

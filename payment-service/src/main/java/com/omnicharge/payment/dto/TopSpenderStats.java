package com.omnicharge.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopSpenderStats {
    private Long userId;
    private String userEmail;
    private String userMobile;
    private String fullName;
    private String registrationDate;
    private Long transactionCount;
    private Long successfulTransactions;
    private Long failedTransactions;
    private BigDecimal totalSpent;
    private BigDecimal averageTransactionValue;
    private Double successRate;
    private String lastTransactionDate;
    private String firstTransactionDate;
}

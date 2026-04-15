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
public class PaymentAnalyticsResponse {
    // Revenue Metrics
    private BigDecimal grossRevenue;
    private BigDecimal todayRevenue;
    private BigDecimal monthRevenue;
    private BigDecimal averageTransactionValue;
    
    // Transaction Counts
    private Long totalTransactions;
    private Long successfulTransactions;
    private Long failedTransactions;
    private Long pendingTransactions;
    
    // Conversion Metrics
    private Double successRate;
    private Double abandonedCheckoutRate;
    
    // Revenue Growth (Month over Month)
    private BigDecimal lastMonthRevenue;
    private Double revenueGrowthPercentage;
    
    // Top Spenders (Whales)
    private List<TopSpenderStats> topSpenders;
    
    // Daily Revenue Trend
    private List<DailyRevenueStats> dailyRevenue;
}

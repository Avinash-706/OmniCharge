package com.omnicharge.recharge.dto;

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
public class RechargeAnalyticsResponse {
    // Volume Metrics
    private Long totalRecharges;
    private Long todayRecharges;
    private Long monthRecharges;
    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
    private BigDecimal monthRevenue;
    
    // Success Rate
    private Double successRate;
    private Long successCount;
    private Long failedCount;
    private Long pendingCount;
    
    // Active vs Expired
    private Long activeRecharges;
    private Long expiredRecharges;
    private Double activeRatio;
    
    // Top Plans
    private List<PlanPerformanceStats> topPlans;
    
    // Operator Market Share
    private List<OperatorMarketShare> operatorShares;
}

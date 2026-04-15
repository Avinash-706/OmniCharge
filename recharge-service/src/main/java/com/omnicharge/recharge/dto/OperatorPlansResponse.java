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
public class OperatorPlansResponse {
    private Long operatorId;
    private String operatorName;
    private Long totalRecharges;
    private BigDecimal totalRevenue;
    private List<PlanPerformanceStats> plans;
}

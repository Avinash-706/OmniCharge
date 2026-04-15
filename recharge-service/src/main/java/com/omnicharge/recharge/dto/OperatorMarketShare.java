package com.omnicharge.recharge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorMarketShare {
    private Long operatorId;
    private String operatorName;
    private Long rechargeCount;
    private BigDecimal totalRevenue;
    private Double marketSharePercentage;
}

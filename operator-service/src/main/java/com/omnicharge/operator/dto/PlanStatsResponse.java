package com.omnicharge.operator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanStatsResponse {
    private Long totalPlans;
    private Long activePlans;
    private Long inactivePlans;
    private Map<String, Long> plansByCategory; // Category name -> count
}

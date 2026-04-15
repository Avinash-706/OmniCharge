package com.omnicharge.recharge.service;

import com.omnicharge.recharge.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IRechargeService {

    RechargeResponse initiateRecharge(Long userId, RechargeRequest request);

    RechargeResponse getRechargeById(String rechargeId, Long userId);

    Page<RechargeResponse> getRechargeHistory(Long userId, Pageable pageable);

    String getRechargeStatus(String rechargeId);

    Page<RechargeResponse> getAllRecharges(Pageable pageable);

    RechargeStatsResponse getRechargeStats();

    List<ExpiringRechargeResponse> getExpiringRecharges(int daysLeft);

    List<ExpiringRechargeResponse> getExpiredToday();

    void markAsExpired(String rechargeId);
    
    // ========== ENTERPRISE BI ANALYTICS ==========
    
    /**
     * Get comprehensive analytics for admin BI dashboard with time filter
     * @param days Number of days to filter (null = all time)
     */
    RechargeAnalyticsResponse getRechargeAnalytics(Integer days);
    
    /**
     * Get all plans for a specific operator (drill-down)
     */
    OperatorPlansResponse getOperatorPlans(Long operatorId);
    
    /**
     * Get recharge history for a specific plan (drill-down) with filtering and sorting
     */
    Page<RechargeResponse> getPlanRechargeHistory(Long planId, com.omnicharge.recharge.entity.RechargeStatus status, String search, Pageable pageable);
    
    /**
     * Get recharge history for a specific operator (drill-down) with filtering and sorting
     */
    Page<RechargeResponse> getOperatorRechargeHistory(Long operatorId, com.omnicharge.recharge.entity.RechargeStatus status, String search, Pageable pageable);
    
    /**
     * Get recharge history for a specific user (drill-down) with filtering and sorting
     */
    Page<RechargeResponse> getUserRechargeHistory(Long userId, com.omnicharge.recharge.entity.RechargeStatus status, String search, Pageable pageable);
}

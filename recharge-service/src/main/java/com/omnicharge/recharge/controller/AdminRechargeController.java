package com.omnicharge.recharge.controller;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.recharge.dto.OperatorPlansResponse;
import com.omnicharge.recharge.dto.RechargeAnalyticsResponse;
import com.omnicharge.recharge.dto.RechargeResponse;
import com.omnicharge.recharge.dto.RechargeStatsResponse;
import com.omnicharge.recharge.entity.RechargeStatus;
import com.omnicharge.recharge.service.IRechargeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/recharges")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRechargeController {

    private final IRechargeService rechargeService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RechargeResponse>>> getAllRecharges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<RechargeResponse> recharges = rechargeService.getAllRecharges(pageable);
        return ResponseEntity.ok(ApiResponse.success("All recharges retrieved successfully", recharges));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<RechargeStatsResponse>> getRechargeStats() {
        RechargeStatsResponse stats = rechargeService.getRechargeStats();
        return ResponseEntity.ok(ApiResponse.success("Recharge stats retrieved successfully", stats));
    }
    
    // ========== ENTERPRISE BI ANALYTICS ENDPOINTS ==========
    
    /**
     * GET /api/admin/recharges/analytics?days=30
     * Master BI endpoint with comprehensive metrics
     */
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<RechargeAnalyticsResponse>> getRechargeAnalytics(
            @RequestParam(required = false) Integer days) {
        RechargeAnalyticsResponse analytics = rechargeService.getRechargeAnalytics(days);
        return ResponseEntity.ok(ApiResponse.success("Recharge analytics retrieved successfully", analytics));
    }
    
    /**
     * GET /api/admin/recharges/operator/{operatorId}/plans
     * Drill-down: Get all plans for a specific operator
     */
    @GetMapping("/operator/{operatorId}/plans")
    public ResponseEntity<ApiResponse<OperatorPlansResponse>> getOperatorPlans(
            @PathVariable Long operatorId) {
        OperatorPlansResponse response = rechargeService.getOperatorPlans(operatorId);
        return ResponseEntity.ok(ApiResponse.success("Operator plans retrieved successfully", response));
    }
    
    /**
     * GET /api/admin/recharges/plan/{planId}/history
     * Drill-down: Get recharge history for a specific plan with sorting and filtering
     */
    @GetMapping("/plan/{planId}/history")
    public ResponseEntity<ApiResponse<Page<RechargeResponse>>> getPlanRechargeHistory(
            @PathVariable Long planId,
            @RequestParam(required = false) RechargeStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<RechargeResponse> recharges = rechargeService.getPlanRechargeHistory(planId, status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Plan recharge history retrieved successfully", recharges));
    }
    
    /**
     * GET /api/admin/recharges/operator/{operatorId}/history
     * Drill-down: Get recharge history for a specific operator with sorting and filtering
     */
    @GetMapping("/operator/{operatorId}/history")
    public ResponseEntity<ApiResponse<Page<RechargeResponse>>> getOperatorRechargeHistory(
            @PathVariable Long operatorId,
            @RequestParam(required = false) RechargeStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<RechargeResponse> recharges = rechargeService.getOperatorRechargeHistory(operatorId, status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Operator recharge history retrieved successfully", recharges));
    }
    
    /**
     * GET /api/admin/recharges/user/{userId}/history
     * Drill-down: Get recharge history for a specific user with sorting and filtering
     */
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<ApiResponse<Page<RechargeResponse>>> getUserRechargeHistory(
            @PathVariable Long userId,
            @RequestParam(required = false) RechargeStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<RechargeResponse> recharges = rechargeService.getUserRechargeHistory(userId, status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("User recharge history retrieved successfully", recharges));
    }
}

package com.omnicharge.payment.controller;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.payment.dto.PaymentAnalyticsResponse;
import com.omnicharge.payment.dto.PaymentStatsResponse;
import com.omnicharge.payment.dto.TopSpenderStats;
import com.omnicharge.payment.dto.TransactionResponse;
import com.omnicharge.payment.entity.PaymentStatus;
import com.omnicharge.payment.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminPaymentController {

    private final IPaymentService paymentService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getAllTransactions(
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String rechargeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        // Security: Verify ADMIN role
        if (!"ADMIN".equals(userRole)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Access denied: Admin role required"));
        }

        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TransactionResponse> transactions = paymentService.getAllTransactions(
                userId, minAmount, maxAmount, status, startDate, endDate, rechargeId, pageable);
        return ResponseEntity.ok(ApiResponse.success("All transactions retrieved successfully", transactions));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PaymentStatsResponse>> getPaymentStats(
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        
        // Security: Verify ADMIN role
        if (!"ADMIN".equals(userRole)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Access denied: Admin role required"));
        }

        PaymentStatsResponse stats = paymentService.getPaymentStats(days);
        return ResponseEntity.ok(ApiResponse.success("Payment stats retrieved successfully", stats));
    }
    
    // ========== ENTERPRISE BI ANALYTICS ENDPOINTS ==========
    
    /**
     * GET /api/admin/payments/analytics?days=30
     * Master BI endpoint with comprehensive payment metrics
     */
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<PaymentAnalyticsResponse>> getPaymentAnalytics(
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        PaymentAnalyticsResponse analytics = paymentService.getPaymentAnalytics(days);
        return ResponseEntity.ok(ApiResponse.success("Payment analytics retrieved successfully", analytics));
    }
    
    /**
     * GET /api/admin/payments/top-spenders?limit=10&days=30
     * Get top spenders (whales) with dynamic limit and time filter
     */
    @GetMapping("/top-spenders")
    public ResponseEntity<ApiResponse<List<TopSpenderStats>>> getTopSpenders(
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false) Integer days) {
        
        List<TopSpenderStats> topSpenders = paymentService.getTopSpenders(limit, days);
        return ResponseEntity.ok(ApiResponse.success("Top spenders retrieved successfully", topSpenders));
    }
    
    /**
     * GET /api/admin/payments/user/{userId}/transactions
     * Drill-down: Get all transactions for a specific user with sorting and filtering
     */
    @GetMapping("/user/{userId}/transactions")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getUserTransactions(
            @PathVariable Long userId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        log.info("getUserTransactions called: userId={}, sortBy={}, sortDir={}, status={}, search={}", 
                userId, sortBy, sortDir, status, search);
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        log.info("Created Pageable with sort: {}", pageable.getSort());
        
        Page<TransactionResponse> transactions = paymentService.getUserTransactions(userId, status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("User transactions retrieved successfully", transactions));
    }
}

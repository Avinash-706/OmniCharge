package com.omnicharge.payment.service;

import com.omnicharge.payment.dto.*;
import com.omnicharge.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface IPaymentService {

    PaymentResponse processPayment(PaymentRequest request);

    TransactionResponse confirmPayment(String transactionId, String razorpayPaymentId, String razorpaySignature);

    TransactionResponse getTransaction(String transactionId, Long userId);

    Page<TransactionResponse> getPaymentHistory(
            Long userId,
            String transactionId,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            PaymentStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);

    Page<TransactionResponse> getAllTransactions(
            Long userId,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            PaymentStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String rechargeId,
            Pageable pageable);

    PaymentStatsResponse getPaymentStats(Integer days);

    TransactionResponse failPayment(String transactionId, String failureReason);
    
    // ========== ENTERPRISE BI ANALYTICS ==========
    
    /**
     * Get comprehensive payment analytics for admin BI dashboard
     * @param days Number of days to filter (null = all time)
     */
    PaymentAnalyticsResponse getPaymentAnalytics(Integer days);
    
    /**
     * Get top spenders (whales) with dynamic limit
     * @param limit Number of top spenders to return
     */
    List<TopSpenderStats> getTopSpenders(Integer limit);
    
    /**
     * Get top spenders with time filter
     * @param limit Number of top spenders to return
     * @param days Number of days to filter (null = all time)
     */
    List<TopSpenderStats> getTopSpenders(Integer limit, Integer days);
    
    /**
     * Get all transactions for a specific user (drill-down) with optional status filter and search
     */
    Page<TransactionResponse> getUserTransactions(Long userId, PaymentStatus status, String search, Pageable pageable);
}

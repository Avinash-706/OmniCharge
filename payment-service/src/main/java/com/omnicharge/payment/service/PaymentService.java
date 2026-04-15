package com.omnicharge.payment.service;

import com.omnicharge.common.event.PaymentCompletedEvent;
import com.omnicharge.common.exception.BadRequestException;
import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.payment.client.UserServiceClient;
import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;
import com.omnicharge.payment.dto.PaymentStatsResponse;
import com.omnicharge.payment.dto.TransactionResponse;
import com.omnicharge.payment.dto.DailyRevenueStats;
import com.omnicharge.payment.dto.TopUserStats;
import com.omnicharge.payment.dto.PaymentAnalyticsResponse;
import com.omnicharge.payment.dto.TopSpenderStats;
import com.omnicharge.payment.dto.UserProfileResponse;
import com.omnicharge.payment.entity.PaymentMethod;
import com.omnicharge.payment.entity.PaymentStatus;
import com.omnicharge.payment.entity.Transaction;
import com.omnicharge.payment.messaging.PaymentEventProducer;
import com.omnicharge.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService implements IPaymentService {

    private final TransactionRepository transactionRepository;
    private final IRazorpayPaymentService razorpayPaymentService;
    private final PaymentEventProducer paymentEventProducer;
    private final RestTemplate restTemplate;
    private final LogEventPublisher logEventPublisher;
    private final UserServiceClient userServiceClient;

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        // Generate transaction ID first
        String transactionId = "TXN-" + java.util.UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        
        // Log business operation: PAYMENT_PROCESSING_START
        Map<String, Object> startContext = new HashMap<>();
        startContext.put("transactionId", transactionId);
        startContext.put("rechargeId", request.getRechargeId());
        startContext.put("userId", request.getUserId().toString());
        startContext.put("amount", request.getAmount().toString());
        startContext.put("paymentMethod", request.getPaymentMethod());
        
        logEventPublisher.publish(LogEvent.builder()
                .serviceName("payment-service")
                .level("INFO")
                .message("Payment processing started")
                .eventType("PAYMENT_PROCESSING_START")
                .context(startContext)
                .timestamp(LocalDateTime.now())
                .build());
        
        // Create transaction record (PENDING)
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setRechargeId(request.getRechargeId());
        transaction.setUserId(request.getUserId());
        transaction.setAmount(request.getAmount());
        transaction.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
        transaction.setStatus(PaymentStatus.PENDING);
        
        // Save metadata for notification service (used later when webhook confirms payment)
        transaction.setUserEmail(request.getUserEmail());
        transaction.setUserMobile(request.getUserMobile());
        transaction.setMobileNumber(request.getMobileNumber());
        transaction.setOperatorName(request.getOperatorName());
        transaction.setPlanName(request.getPlanName());

        log.info("Saving Transaction metadata: email={}, mobile={}, op={}, plan={}, target={}", 
                transaction.getUserEmail(), transaction.getUserMobile(), 
                transaction.getOperatorName(), transaction.getPlanName(), transaction.getMobileNumber());

        transaction = transactionRepository.save(transaction);
        log.info("Transaction created with PENDING status: {} for recharge: {}", transactionId, request.getRechargeId());

        // Process payment via Razorpay
        PaymentResponse paymentResponse = razorpayPaymentService.processRazorpayPayment(request);

        // Log business operation: RAZORPAY_GATEWAY_INTERACTION
        Map<String, Object> gatewayContext = new HashMap<>();
        gatewayContext.put("transactionId", transactionId);
        gatewayContext.put("rechargeId", request.getRechargeId());
        gatewayContext.put("razorpayOrderId", paymentResponse.getRazorpayOrderId());
        gatewayContext.put("responseStatus", paymentResponse.getStatus());
        gatewayContext.put("amount", request.getAmount().toString());
        
        logEventPublisher.publish(LogEvent.builder()
                .serviceName("payment-service")
                .level("INFO")
                .message("Razorpay gateway interaction completed")
                .eventType("RAZORPAY_GATEWAY_INTERACTION")
                .context(gatewayContext)
                .timestamp(LocalDateTime.now())
                .build());

        // Update transaction with Razorpay response
        transaction.setRazorpayOrderId(paymentResponse.getRazorpayOrderId());

        if ("SUCCESS".equals(paymentResponse.getStatus())) {
            transaction.setStatus(PaymentStatus.SUCCESS);
            log.info("Payment successful for recharge: {}", request.getRechargeId());
            
            // Log business operation: PAYMENT_SUCCESS
            Map<String, Object> successContext = new HashMap<>();
            successContext.put("transactionId", transactionId);
            successContext.put("rechargeId", request.getRechargeId());
            successContext.put("userId", request.getUserId().toString());
            successContext.put("amount", request.getAmount().toString());
            successContext.put("razorpayOrderId", paymentResponse.getRazorpayOrderId());
            successContext.put("paymentMethod", request.getPaymentMethod());
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName("payment-service")
                    .level("INFO")
                    .message("Payment completed successfully")
                    .eventType("PAYMENT_SUCCESS")
                    .context(successContext)
                    .timestamp(LocalDateTime.now())
                    .build());
        } else if ("PENDING".equals(paymentResponse.getStatus())) {
            transaction.setStatus(PaymentStatus.PENDING);
            log.info("Payment pending for recharge: {}", request.getRechargeId());
            
            // Log business operation: PAYMENT_PENDING
            Map<String, Object> pendingContext = new HashMap<>();
            pendingContext.put("transactionId", transactionId);
            pendingContext.put("rechargeId", request.getRechargeId());
            pendingContext.put("userId", request.getUserId().toString());
            pendingContext.put("razorpayOrderId", paymentResponse.getRazorpayOrderId());
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName("payment-service")
                    .level("INFO")
                    .message("Payment is pending confirmation")
                    .eventType("PAYMENT_PENDING")
                    .context(pendingContext)
                    .timestamp(LocalDateTime.now())
                    .build());
        } else {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setFailureReason("Razorpay payment failed");
            log.warn("Payment failed for recharge: {}", request.getRechargeId());
            
            // Log business operation: PAYMENT_FAILED
            Map<String, Object> failedContext = new HashMap<>();
            failedContext.put("transactionId", transactionId);
            failedContext.put("rechargeId", request.getRechargeId());
            failedContext.put("userId", request.getUserId().toString());
            failedContext.put("amount", request.getAmount().toString());
            failedContext.put("failureReason", "Razorpay payment failed");
            failedContext.put("paymentMethod", request.getPaymentMethod());
            
            logEventPublisher.publish(LogEvent.builder()
                    .serviceName("payment-service")
                    .level("WARN")
                    .message("Payment failed")
                    .eventType("PAYMENT_FAILED")
                    .context(failedContext)
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        transaction = transactionRepository.save(transaction);

        // Only publish the event if it's a terminal state to avoid "failed" notifications for PENDING
        if (transaction.getStatus() == PaymentStatus.SUCCESS || transaction.getStatus() == PaymentStatus.FAILED) {
            publishPaymentCompletedEvent(transaction, transaction.getUserEmail(), transaction.getUserMobile(),
                    transaction.getMobileNumber(), transaction.getOperatorName(), transaction.getPlanName());
        }

        return PaymentResponse.builder()
                .transactionId(transactionId)
                .status(paymentResponse.getStatus())
                .razorpayOrderId(paymentResponse.getRazorpayOrderId())
                .amount(paymentResponse.getAmount())
                .timestamp(paymentResponse.getTimestamp())
                .build();
    }

    @Override
    @Transactional
    public TransactionResponse confirmPayment(String transactionId, String razorpayPaymentId, String razorpaySignature) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Transaction {} is already confirmed", transactionId);
            return mapToResponse(transaction);
        }

        String previousStatus = transaction.getStatus().name();
        transaction.setStatus(PaymentStatus.SUCCESS);
        // Store the Razorpay payment ID (NOT in razorpayOrderId — that holds the order_id)
        transaction.setRazorpayPaymentId(razorpayPaymentId);
        transaction = transactionRepository.save(transaction);
        
        log.info("Payment confirmed successfully for transaction: {} (razorpayPaymentId: {}, razorpayOrderId: {})", 
                transactionId, razorpayPaymentId, transaction.getRazorpayOrderId());

        // Log business operation: PAYMENT_CONFIRMED
        Map<String, Object> confirmContext = new HashMap<>();
        confirmContext.put("transactionId", transactionId);
        confirmContext.put("rechargeId", transaction.getRechargeId());
        confirmContext.put("userId", transaction.getUserId().toString());
        confirmContext.put("previousStatus", previousStatus);
        confirmContext.put("currentStatus", "SUCCESS");
        confirmContext.put("razorpayPaymentId", razorpayPaymentId);
        confirmContext.put("amount", transaction.getAmount().toString());
        
        logEventPublisher.publish(LogEvent.builder()
                .serviceName("payment-service")
                .level("INFO")
                .message("Payment confirmed via webhook")
                .eventType("PAYMENT_CONFIRMED")
                .context(confirmContext)
                .timestamp(LocalDateTime.now())
                .build());

        // If notification metadata is missing from Transaction, fetch from recharge-service
        if (transaction.getMobileNumber() == null || transaction.getOperatorName() == null || transaction.getPlanName() == null) {
            log.warn("Transaction {} has null metadata (mobileNumber/operatorName/planName). Fetching from recharge-service...", transactionId);
            enrichTransactionFromRechargeService(transaction);
        }
        
        // Publish PaymentApprovedEvent for saga orchestrator
        com.omnicharge.common.event.saga.PaymentApprovedEvent approvedEvent = com.omnicharge.common.event.saga.PaymentApprovedEvent.builder()
                .rechargeId(transaction.getRechargeId())
                .transactionId(transaction.getTransactionId())
                .razorpayOrderId(transaction.getRazorpayOrderId())
                .amount(transaction.getAmount())
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();
        paymentEventProducer.publishPaymentApproved(approvedEvent);

        // Publish PaymentCompletedEvent for notification-service with enriched data
        publishPaymentCompletedEvent(transaction, 
                transaction.getUserEmail(), 
                transaction.getUserMobile(), 
                transaction.getMobileNumber(), 
                transaction.getOperatorName(), 
                transaction.getPlanName());

        return mapToResponse(transaction);
    }

    @Override
    public TransactionResponse getTransaction(String transactionId, Long userId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        if (!transaction.getUserId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to transaction");
        }

        return mapToResponse(transaction);
    }

    @Override
    public Page<TransactionResponse> getPaymentHistory(
            Long userId, 
            String transactionId,
            BigDecimal minAmount, 
            BigDecimal maxAmount, 
            PaymentStatus status, 
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Pageable pageable) {
        
        Page<Transaction> transactions = transactionRepository.findByUserIdWithFilters(
                userId, minAmount, maxAmount, status, startDate, endDate, transactionId, pageable);
        return transactions.map(this::mapToResponse);
    }

    @Override
    public Page<TransactionResponse> getAllTransactions(
            Long userId,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            PaymentStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String rechargeId,
            Pageable pageable) {
        
        Page<Transaction> transactions = transactionRepository.findAllWithFilters(
                userId, minAmount, maxAmount, status, startDate, endDate, rechargeId, pageable);
        return transactions.map(this::mapToResponse);
    }

    @Override
    public PaymentStatsResponse getPaymentStats(Integer days) {
        if (days == null) {
            days = 30; // Default to last 30 days
        }
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        
        // Overall stats
        long totalTransactions = transactionRepository.count();
        long successfulTransactions = transactionRepository.countByStatus(PaymentStatus.SUCCESS);
        long failedTransactions = transactionRepository.countByStatus(PaymentStatus.FAILED);
        long pendingTransactions = transactionRepository.countByStatus(PaymentStatus.PENDING);
        
        BigDecimal totalRevenue = transactionRepository.sumAmountByStatus(PaymentStatus.SUCCESS);
        BigDecimal successAmount = totalRevenue;
        BigDecimal failedAmount = transactionRepository.sumAmountByStatus(PaymentStatus.FAILED);
        BigDecimal averageAmount = transactionRepository.averageAmountByStatus(PaymentStatus.SUCCESS);
        
        // Today's stats
        long todayTransactions = transactionRepository.countTransactionsSince(todayStart);
        BigDecimal todayRevenue = transactionRepository.sumAmountSinceByStatus(todayStart, PaymentStatus.SUCCESS);
        
        // Revenue by date (last N days)
        List<Object[]> revenueData = transactionRepository.findRevenueByDate(startDate, PaymentStatus.SUCCESS);
        List<DailyRevenueStats> revenueByDate = revenueData.stream()
                .map(row -> DailyRevenueStats.builder()
                        .date(row[0].toString())
                        .transactionCount((Long) row[1])
                        .revenue((BigDecimal) row[2])
                        .build())
                .toList();
        
        // Top 10 users by revenue
        List<Object[]> topUsersData = transactionRepository.findTopUsersByRevenue(
                PaymentStatus.SUCCESS, PageRequest.of(0, 10));
        List<TopUserStats> topUsers = topUsersData.stream()
                .map(row -> TopUserStats.builder()
                        .userId((Long) row[0])
                        .transactionCount((Long) row[1])
                        .totalSpent((BigDecimal) row[2])
                        .build())
                .toList();

        return PaymentStatsResponse.builder()
                .totalTransactions(totalTransactions)
                .successfulTransactions(successfulTransactions)
                .failedTransactions(failedTransactions)
                .pendingTransactions(pendingTransactions)
                .totalRevenue(totalRevenue)
                .successAmount(successAmount)
                .failedAmount(failedAmount)
                .averageTransactionAmount(averageAmount)
                .todayTransactions(todayTransactions)
                .todayRevenue(todayRevenue)
                .revenueByDate(revenueByDate)
                .topUsers(topUsers)
                .build();
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .rechargeId(transaction.getRechargeId())
                .userId(transaction.getUserId())
                .amount(transaction.getAmount())
                .paymentMethod(transaction.getPaymentMethod())
                .status(transaction.getStatus())
                .failureReason(transaction.getFailureReason())
                .razorpayOrderId(transaction.getRazorpayOrderId())
                .userEmail(transaction.getUserEmail())
                .userMobile(transaction.getUserMobile())
                .mobileNumber(transaction.getMobileNumber())
                .operatorName(transaction.getOperatorName())
                .planName(transaction.getPlanName())
                .createdDate(transaction.getCreatedDate())
                .build();
    }

    private void publishPaymentCompletedEvent(Transaction transaction, String userEmail, String userMobile,
                                              String mobileNumber, String operatorName, String planName) {
        try {
            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .transactionId(transaction.getTransactionId())
                    .rechargeId(transaction.getRechargeId())
                    .userId(transaction.getUserId())
                    .userEmail(userEmail)
                    .userMobile(userMobile)
                    .mobileNumber(mobileNumber)
                    .operatorName(operatorName)
                    .planName(planName)
                    .amount(transaction.getAmount())
                    .status(transaction.getStatus().name())
                    .paymentMethod(transaction.getPaymentMethod().name())
                    .timestamp(LocalDateTime.now())
                    .build();

            paymentEventProducer.publishPaymentCompleted(event);
            log.info("Published payment completed event: {}", transaction.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to publish payment event: {}", transaction.getTransactionId(), e);
        }
    }

    /**
     * Fetches recharge details from the recharge-service and enriches the Transaction entity.
     * This is a fallback for when the saga event did not propagate mobileNumber/operatorName/planName.
     */
    @SuppressWarnings("unchecked")
    private void enrichTransactionFromRechargeService(Transaction transaction) {
        try {
            String url = "http://recharge-service/api/internal/recharges/" + transaction.getRechargeId();
            log.info("Calling recharge-service: {}", url);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, 
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            
            Map<String, Object> body = response.getBody();
            if (body != null && Boolean.TRUE.equals(body.get("success"))) {
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                if (data != null) {
                    String mobileNumber = (String) data.get("mobileNumber");
                    String operatorName = (String) data.get("operatorName");
                    String planName = (String) data.get("planName");
                    
                    log.info("Enriched from recharge-service: mobile={}, op={}, plan={}", 
                            mobileNumber, operatorName, planName);
                    
                    transaction.setMobileNumber(mobileNumber);
                    transaction.setOperatorName(operatorName);
                    transaction.setPlanName(planName);
                    
                    // Persist the enriched data for future reference
                    transactionRepository.save(transaction);
                }
            } else {
                log.warn("Recharge-service returned no data for rechargeId: {}", transaction.getRechargeId());
            }
        } catch (Exception e) {
            log.error("Failed to fetch recharge details from recharge-service for rechargeId: {}", 
                    transaction.getRechargeId(), e);
        }
    }

    /**
     * SAGA ROLLBACK: Marks a PENDING transaction as FAILED and publishes PaymentRejectedEvent.
     * Called when the user closes the Razorpay modal or a payment.failed event fires in the frontend.
     * The RechargeSagaConsumer in recharge-service will listen for this and update Recharge → FAILED.
     */
    @Override
    @Transactional
    public TransactionResponse failPayment(String transactionId, String failureReason) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        // Only fail if currently PENDING — don't overwrite a SUCCESS
        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            log.warn("Cannot fail transaction {} — already marked as SUCCESS", transactionId);
            return mapToResponse(transaction);
        }
        
        if (transaction.getStatus() == PaymentStatus.FAILED) {
            log.info("Transaction {} is already marked as FAILED", transactionId);
            return mapToResponse(transaction);
        }

        String previousStatus = transaction.getStatus().name();
        transaction.setStatus(PaymentStatus.FAILED);
        transaction.setFailureReason(failureReason);
        transaction = transactionRepository.save(transaction);

        log.warn("Payment marked as FAILED for transaction: {} | Reason: {}", transactionId, failureReason);

        // Log business operation: PAYMENT_FAILED_BY_USER
        Map<String, Object> failContext = new HashMap<>();
        failContext.put("transactionId", transactionId);
        failContext.put("rechargeId", transaction.getRechargeId());
        failContext.put("userId", transaction.getUserId().toString());
        failContext.put("previousStatus", previousStatus);
        failContext.put("currentStatus", "FAILED");
        failContext.put("failureReason", failureReason);
        failContext.put("amount", transaction.getAmount().toString());

        logEventPublisher.publish(LogEvent.builder()
                .serviceName("payment-service")
                .level("WARN")
                .message("Payment failed/cancelled by user")
                .eventType("PAYMENT_FAILED_BY_USER")
                .context(failContext)
                .timestamp(LocalDateTime.now())
                .build());

        // Publish PaymentRejectedEvent for SAGA orchestrator → recharge-service updates Recharge to FAILED
        com.omnicharge.common.event.saga.PaymentRejectedEvent rejectedEvent = com.omnicharge.common.event.saga.PaymentRejectedEvent.builder()
                .rechargeId(transaction.getRechargeId())
                .failureReason(failureReason)
                .timestamp(LocalDateTime.now())
                .build();
        paymentEventProducer.publishPaymentRejected(rejectedEvent);

        return mapToResponse(transaction);
    }
    
    // ========== ENTERPRISE BI ANALYTICS IMPLEMENTATION ==========
    
    @Override
    public PaymentAnalyticsResponse getPaymentAnalytics(Integer days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = days != null && days > 0 
                ? now.minusDays(days) 
                : LocalDateTime.of(2020, 1, 1, 0, 0); // All time default
        
        LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime lastMonthStart = monthStart.minusMonths(1);
        LocalDateTime lastMonthEnd = monthStart.minusSeconds(1);
        
        // Revenue Metrics (filtered by date range)
        BigDecimal grossRevenue = transactionRepository.sumAmountBetweenDates(
                startDate, now, PaymentStatus.SUCCESS);
        BigDecimal todayRevenue = transactionRepository.sumAmountBetweenDates(
                todayStart, now, PaymentStatus.SUCCESS);
        BigDecimal monthRevenue = transactionRepository.sumAmountBetweenDates(
                monthStart, now, PaymentStatus.SUCCESS);
        BigDecimal lastMonthRevenue = transactionRepository.sumAmountBetweenDates(
                lastMonthStart, lastMonthEnd, PaymentStatus.SUCCESS);
        
        // Transaction Counts (filtered by date range)
        long totalTransactions = transactionRepository.countTransactionsBetweenDates(
                startDate, now, PaymentStatus.SUCCESS) +
                transactionRepository.countTransactionsBetweenDates(
                startDate, now, PaymentStatus.FAILED) +
                transactionRepository.countTransactionsBetweenDates(
                startDate, now, PaymentStatus.PENDING);
        long successfulTransactions = transactionRepository.countTransactionsBetweenDates(
                startDate, now, PaymentStatus.SUCCESS);
        long failedTransactions = transactionRepository.countTransactionsBetweenDates(
                startDate, now, PaymentStatus.FAILED);
        long pendingTransactions = transactionRepository.countTransactionsBetweenDates(
                startDate, now, PaymentStatus.PENDING);
        
        // Average Transaction Value (SUCCESS only)
        BigDecimal averageTransactionValue = successfulTransactions > 0
                ? grossRevenue.divide(BigDecimal.valueOf(successfulTransactions), 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;
        
        // Conversion Metrics
        double successRate = totalTransactions > 0
                ? (successfulTransactions * 100.0 / totalTransactions)
                : 0.0;
        double abandonedCheckoutRate = totalTransactions > 0
                ? (failedTransactions * 100.0 / totalTransactions)
                : 0.0;
        
        // Revenue Growth (Month over Month)
        double revenueGrowthPercentage = 0.0;
        if (lastMonthRevenue != null && lastMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal growth = monthRevenue.subtract(lastMonthRevenue);
            revenueGrowthPercentage = growth.multiply(BigDecimal.valueOf(100))
                    .divide(lastMonthRevenue, 2, BigDecimal.ROUND_HALF_UP)
                    .doubleValue();
        }
        
        // Top Spenders (filtered by date range)
        List<TopSpenderStats> topSpenders = getTopSpenders(10, days);
        
        // Daily Revenue (Last 30 days or filtered range)
        int daysForChart = days != null && days > 0 && days < 30 ? days : 30;
        LocalDateTime chartStartDate = now.minusDays(daysForChart);
        List<Object[]> revenueData = transactionRepository.findRevenueByDate(chartStartDate, PaymentStatus.SUCCESS);
        List<DailyRevenueStats> dailyRevenue = revenueData.stream()
                .map(row -> {
                    Object revenueObj = row[2];
                    BigDecimal revenue = revenueObj instanceof BigDecimal 
                            ? (BigDecimal) revenueObj 
                            : BigDecimal.valueOf(((Number) revenueObj).doubleValue());
                    
                    return DailyRevenueStats.builder()
                            .date(row[0].toString())
                            .transactionCount((Long) row[1])
                            .revenue(revenue)
                            .build();
                })
                .collect(Collectors.toList());
        
        return PaymentAnalyticsResponse.builder()
                .grossRevenue(grossRevenue != null ? grossRevenue : BigDecimal.ZERO)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .monthRevenue(monthRevenue != null ? monthRevenue : BigDecimal.ZERO)
                .averageTransactionValue(averageTransactionValue)
                .totalTransactions(totalTransactions)
                .successfulTransactions(successfulTransactions)
                .failedTransactions(failedTransactions)
                .pendingTransactions(pendingTransactions)
                .successRate(successRate)
                .abandonedCheckoutRate(abandonedCheckoutRate)
                .lastMonthRevenue(lastMonthRevenue != null ? lastMonthRevenue : BigDecimal.ZERO)
                .revenueGrowthPercentage(revenueGrowthPercentage)
                .topSpenders(topSpenders)
                .dailyRevenue(dailyRevenue)
                .build();
    }
    
    public List<TopSpenderStats> getTopSpenders(Integer limit) {
        return getTopSpenders(limit, null);
    }
    
    @Override
    public List<TopSpenderStats> getTopSpenders(Integer limit, Integer days) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = days != null && days > 0 
                ? now.minusDays(days) 
                : LocalDateTime.of(2020, 1, 1, 0, 0);
        
        // Use date-filtered query if days is specified
        List<Object[]> topSpendersData;
        if (days != null && days > 0) {
            topSpendersData = transactionRepository.findTopSpendersByRevenueWithDateFilter(
                    PaymentStatus.SUCCESS, startDate, PageRequest.of(0, limit));
        } else {
            topSpendersData = transactionRepository.findTopSpendersByRevenue(
                    PaymentStatus.SUCCESS, PageRequest.of(0, limit));
        }
        
        return topSpendersData.stream()
                .map(row -> {
                    Long userId = (Long) row[0];
                    Long transactionCount = (Long) row[1];
                    
                    Object totalSpentObj = row[2];
                    BigDecimal totalSpent = totalSpentObj instanceof BigDecimal 
                            ? (BigDecimal) totalSpentObj 
                            : BigDecimal.valueOf(((Number) totalSpentObj).doubleValue());
                    
                    Object avgObj = row[3];
                    BigDecimal avgTransactionValue = avgObj instanceof BigDecimal 
                            ? (BigDecimal) avgObj 
                            : BigDecimal.valueOf(((Number) avgObj).doubleValue());
                    
                    LocalDateTime lastTransactionDate = (LocalDateTime) row[4];
                    LocalDateTime firstTransactionDate = row.length > 5 ? (LocalDateTime) row[5] : null;
                    
                    // Fetch user details from transaction (email/mobile)
                    String userEmail = null;
                    String userMobile = null;
                    String fullName = null;
                    String registrationDate = null;
                    Long successfulTransactions = transactionCount; // Already filtered by SUCCESS
                    Long failedTransactions = 0L;
                    
                    try {
                        Transaction recentTx = transactionRepository.findAllByUserId(
                                userId, PageRequest.of(0, 1)).getContent().stream().findFirst().orElse(null);
                        if (recentTx != null) {
                            userEmail = recentTx.getUserEmail();
                            userMobile = recentTx.getUserMobile();
                        }
                        
                        // Get transaction stats (success/failed counts)
                        Object[] stats = transactionRepository.getUserTransactionStats(userId);
                        if (stats != null && stats.length >= 3) {
                            Long totalTxCount = ((Number) stats[0]).longValue();
                            successfulTransactions = ((Number) stats[1]).longValue();
                            failedTransactions = ((Number) stats[2]).longValue();
                        }
                        
                        // CRITICAL FIX: Fetch fullName and registrationDate from user-service via FeignClient
                        try {
                            var userResponse = userServiceClient.getUserById(userId);
                            if (userResponse != null && userResponse.isSuccess() && userResponse.getData() != null) {
                                UserProfileResponse userProfile = userResponse.getData();
                                fullName = userProfile.getFullName();
                                registrationDate = userProfile.getCreatedDate();
                                
                                // Use user-service data as source of truth if transaction data is missing
                                if (userEmail == null) {
                                    userEmail = userProfile.getEmail();
                                }
                                if (userMobile == null) {
                                    userMobile = userProfile.getMobileNumber();
                                }
                                
                                log.debug("Enriched Top Spender userId={} with fullName={} from user-service", userId, fullName);
                            } else {
                                log.warn("User Service returned no data for userId: {}", userId);
                                fullName = userEmail != null ? userEmail.split("@")[0] : "User #" + userId;
                            }
                        } catch (Exception e) {
                            log.warn("Failed to fetch user profile from user-service for userId: {} - {}", userId, e.getMessage());
                            fullName = userEmail != null ? userEmail.split("@")[0] : "User #" + userId;
                        }
                        
                    } catch (Exception e) {
                        log.warn("Could not fetch user details for userId: {}", userId);
                        fullName = "User #" + userId;
                    }
                    
                    // Calculate success rate
                    double successRate = (successfulTransactions + failedTransactions) > 0
                            ? (successfulTransactions * 100.0 / (successfulTransactions + failedTransactions))
                            : 0.0;
                    
                    return TopSpenderStats.builder()
                            .userId(userId)
                            .userEmail(userEmail)
                            .userMobile(userMobile)
                            .fullName(fullName)
                            .registrationDate(registrationDate)
                            .transactionCount(transactionCount)
                            .successfulTransactions(successfulTransactions)
                            .failedTransactions(failedTransactions)
                            .totalSpent(totalSpent)
                            .averageTransactionValue(avgTransactionValue)
                            .successRate(successRate)
                            .lastTransactionDate(lastTransactionDate != null ? lastTransactionDate.toString() : null)
                            .firstTransactionDate(firstTransactionDate != null ? firstTransactionDate.toString() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public Page<TransactionResponse> getUserTransactions(Long userId, PaymentStatus status, String search, Pageable pageable) {
        Page<Transaction> transactions;
        
        if (search != null && !search.trim().isEmpty()) {
            // Search by transaction ID
            transactions = transactionRepository.searchByUserIdAndTransactionId(userId, search.trim(), pageable);
        } else if (status != null) {
            transactions = transactionRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            transactions = transactionRepository.findAllByUserId(userId, pageable);
        }
        
        return transactions.map(this::mapToResponse);
    }
}


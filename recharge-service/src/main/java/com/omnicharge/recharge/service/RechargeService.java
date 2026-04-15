package com.omnicharge.recharge.service;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.common.exception.BadRequestException;
import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.recharge.client.OperatorServiceClient;
import com.omnicharge.recharge.client.UserServiceClient;
import com.omnicharge.recharge.dto.*;
import com.omnicharge.recharge.entity.Recharge;
import com.omnicharge.recharge.entity.RechargeStatus;
import com.omnicharge.recharge.messaging.RechargeEventProducer;
import com.omnicharge.recharge.repository.RechargeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RechargeService implements IRechargeService {

    private final RechargeRepository rechargeRepository;
    private final OperatorServiceClient operatorServiceClient;
    private final UserServiceClient userServiceClient;
    private final RechargeEventProducer rechargeEventProducer;
    private final LogEventPublisher logEventPublisher;

    @Override
    @Transactional
    public RechargeResponse initiateRecharge(Long userId, RechargeRequest request) {
        // Validate plan with circuit breaker, retry, and caching
        ApiResponse<PlanResponse> planApiResponse = operatorServiceClient.getPlan(request.getPlanId());
        
        // Check if Operator Service is unavailable (circuit breaker fallback)
        if (planApiResponse == null || !planApiResponse.isSuccess() || planApiResponse.getData() == null) {
            throw new BadRequestException("Unable to validate plan. Operator Service is temporarily unavailable. Please try again later.");
        }
        
        PlanResponse plan = planApiResponse.getData();

        if (!plan.getIsActive()) {
            throw new BadRequestException("Invalid or inactive plan");
        }

        if (!plan.getOperatorId().equals(request.getOperatorId())) {
            throw new BadRequestException("Plan does not belong to the specified operator");
        }

        // Create recharge record (INITIATED)
        Recharge recharge = new Recharge();
        recharge.setRechargeId("OMNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        recharge.setUserId(userId);
        recharge.setMobileNumber(request.getMobileNumber());
        recharge.setOperatorId(plan.getOperatorId());
        recharge.setOperatorName(plan.getOperatorName());
        recharge.setPlanId(plan.getId());
        recharge.setPlanName(plan.getPlanName());
        recharge.setAmount(plan.getPrice());
        recharge.setPlanValidityDays(plan.getValidityDays());
        recharge.setPlanExpiryDate(LocalDate.now().plusDays(plan.getValidityDays()));
        recharge.setStatus(RechargeStatus.INITIATED);

        recharge = rechargeRepository.save(recharge);
        log.info("Recharge initiated: {}", recharge.getRechargeId());

        // Log business operation: RECHARGE_INITIATED
        Map<String, Object> initiatedContext = new HashMap<>();
        initiatedContext.put("rechargeId", recharge.getRechargeId());
        initiatedContext.put("userId", recharge.getUserId().toString());
        initiatedContext.put("amount", recharge.getAmount().toString());
        initiatedContext.put("operatorName", recharge.getOperatorName());
        initiatedContext.put("planName", recharge.getPlanName());
        initiatedContext.put("mobileNumber", recharge.getMobileNumber());
        initiatedContext.put("status", recharge.getStatus().name());
        
        logEventPublisher.publish(LogEvent.builder()
                .serviceName("recharge-service")
                .level("INFO")
                .message("Recharge initiated")
                .eventType("RECHARGE_INITIATED")
                .context(initiatedContext)
                .timestamp(LocalDateTime.now())
                .build());

        // Update to PROCESSING
        recharge.setStatus(RechargeStatus.PROCESSING);
        recharge = rechargeRepository.save(recharge);

        // Log business operation: RECHARGE_PROCESSING
        Map<String, Object> processingContext = new HashMap<>();
        processingContext.put("rechargeId", recharge.getRechargeId());
        processingContext.put("userId", recharge.getUserId().toString());
        processingContext.put("previousStatus", "INITIATED");
        processingContext.put("currentStatus", "PROCESSING");
        
        logEventPublisher.publish(LogEvent.builder()
                .serviceName("recharge-service")
                .level("INFO")
                .message("Recharge status updated to PROCESSING")
                .eventType("RECHARGE_PROCESSING")
                .context(processingContext)
                .timestamp(LocalDateTime.now())
                .build());

        // Fetch user details to pass along for notifications
        String userEmail = "";
        String userMobile = "";
        try {
            ApiResponse<UserProfileResponse> userApiResponse = userServiceClient.getUserById(userId);
            if (userApiResponse != null && userApiResponse.isSuccess() && userApiResponse.getData() != null) {
                userEmail = userApiResponse.getData().getEmail();
                userMobile = userApiResponse.getData().getMobileNumber();
            }
        } catch (Exception e) {
            log.warn("Could not fetch user profile for userId: {}. Payment notification may lack email/mobile details.", userId);
        }

        // Publish event asynchronously for saga orchestration
        com.omnicharge.common.event.saga.RechargeInitiatedEvent sagaEvent = com.omnicharge.common.event.saga.RechargeInitiatedEvent.builder()
                .rechargeId(recharge.getRechargeId())
                .userId(recharge.getUserId())
                .amount(recharge.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .mobileNumber(recharge.getMobileNumber())
                .operatorName(recharge.getOperatorName())
                .planName(recharge.getPlanName())
                .userEmail(userEmail)
                .userMobile(userMobile)
                .timestamp(LocalDateTime.now())
                .build();
        
        log.info("Publishing RechargeInitiatedEvent: email={}, mobile={}, op={}, plan={}, target={}", 
                sagaEvent.getUserEmail(), sagaEvent.getUserMobile(), 
                sagaEvent.getOperatorName(), sagaEvent.getPlanName(), sagaEvent.getMobileNumber());
        
        rechargeEventProducer.publishRechargeInitiated(sagaEvent);

        return mapToResponse(recharge);
    }

    @Override
    public RechargeResponse getRechargeById(String rechargeId, Long userId) {
        Recharge recharge = rechargeRepository.findByRechargeId(rechargeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recharge not found with id: " + rechargeId));

        if (!recharge.getUserId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to recharge");
        }

        return mapToResponse(recharge);
    }

    @Override
    public Page<RechargeResponse> getRechargeHistory(Long userId, Pageable pageable) {
        Page<Recharge> recharges = rechargeRepository.findByUserId(userId, pageable);
        return recharges.map(this::mapToResponse);
    }

    @Override
    public String getRechargeStatus(String rechargeId) {
        Recharge recharge = rechargeRepository.findByRechargeId(rechargeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recharge not found with id: " + rechargeId));
        return recharge.getStatus().name();
    }

    @Override
    public Page<RechargeResponse> getAllRecharges(Pageable pageable) {
        Page<Recharge> recharges = rechargeRepository.findAll(pageable);
        return recharges.map(this::mapToResponse);
    }

    @Override
    public RechargeStatsResponse getRechargeStats() {
        long totalRecharges = rechargeRepository.count();
        long successCount = rechargeRepository.countByStatus(RechargeStatus.SUCCESS);
        long failedCount = rechargeRepository.countByStatus(RechargeStatus.FAILED);

        // Calculate total amount from successful recharges
        List<Recharge> successfulRecharges = rechargeRepository.findByCreatedDateBetween(
                LocalDateTime.now().minusYears(10),
                LocalDateTime.now()
        );

        BigDecimal totalAmount = successfulRecharges.stream()
                .filter(r -> r.getStatus() == RechargeStatus.SUCCESS)
                .map(Recharge::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RechargeStatsResponse.builder()
                .totalRecharges(totalRecharges)
                .successCount(successCount)
                .failedCount(failedCount)
                .totalAmount(totalAmount)
                .build();
    }

    @Override
    public List<ExpiringRechargeResponse> getExpiringRecharges(int daysLeft) {
        LocalDate expiryDate = LocalDate.now().plusDays(daysLeft);
        List<Recharge> recharges = rechargeRepository.findByStatusAndPlanExpiryDate(RechargeStatus.SUCCESS, expiryDate);

        return recharges.stream()
                .map(this::mapToExpiringResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpiringRechargeResponse> getExpiredToday() {
        LocalDate today = LocalDate.now();
        List<Recharge> recharges = rechargeRepository.findByStatusAndPlanExpiryDate(RechargeStatus.SUCCESS, today);

        return recharges.stream()
                .map(this::mapToExpiringResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsExpired(String rechargeId) {
        Recharge recharge = rechargeRepository.findByRechargeId(rechargeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recharge not found with id: " + rechargeId));

        recharge.setStatus(RechargeStatus.EXPIRED);
        rechargeRepository.save(recharge);
        log.info("Marked recharge as expired: {}", rechargeId);

        // Log business operation: RECHARGE_EXPIRED
        Map<String, Object> expiredContext = new HashMap<>();
        expiredContext.put("rechargeId", rechargeId);
        expiredContext.put("userId", recharge.getUserId().toString());
        expiredContext.put("expiryDate", recharge.getPlanExpiryDate().toString());
        
        logEventPublisher.publish(LogEvent.builder()
                .serviceName("recharge-service")
                .level("WARN")
                .message("Recharge marked as expired")
                .eventType("RECHARGE_EXPIRED")
                .context(expiredContext)
                .timestamp(LocalDateTime.now())
                .build());
    }

    private RechargeResponse mapToResponse(Recharge recharge) {
        // TASK 4: Enrich with user's full name from user-service
        String userFullName = null;
        try {
            ApiResponse<UserProfileResponse> userApiResponse = userServiceClient.getUserById(recharge.getUserId());
            if (userApiResponse != null && userApiResponse.isSuccess() && userApiResponse.getData() != null) {
                userFullName = userApiResponse.getData().getFullName();
            }
        } catch (Exception e) {
            log.warn("Could not fetch user full name for userId: {}", recharge.getUserId());
        }
        
        return RechargeResponse.builder()
                .id(recharge.getId())
                .rechargeId(recharge.getRechargeId())
                .userId(recharge.getUserId())
                .userFullName(userFullName)
                .mobileNumber(recharge.getMobileNumber())
                .operatorId(recharge.getOperatorId())
                .operatorName(recharge.getOperatorName())
                .planId(recharge.getPlanId())
                .planName(recharge.getPlanName())
                .amount(recharge.getAmount())
                .planValidityDays(recharge.getPlanValidityDays())
                .planExpiryDate(recharge.getPlanExpiryDate())
                .status(recharge.getStatus())
                .failureReason(recharge.getFailureReason())
                .transactionId(recharge.getTransactionId())
                .createdDate(recharge.getCreatedDate())
                .build();
    }

    private ExpiringRechargeResponse mapToExpiringResponse(Recharge recharge) {
        // Fetch user details
        UserProfileResponse user = null;
        try {
            ApiResponse<UserProfileResponse> userApiResponse = userServiceClient.getUserById(recharge.getUserId());
            user = userApiResponse.getData();
        } catch (Exception e) {
            log.error("Failed to fetch user details for userId: {}", recharge.getUserId(), e);
        }

        return ExpiringRechargeResponse.builder()
                .rechargeId(recharge.getRechargeId())
                .userId(recharge.getUserId())
                .userEmail(user != null ? user.getEmail() : null)
                .userMobile(user != null ? user.getMobileNumber() : null)
                .mobileNumber(recharge.getMobileNumber())
                .operatorName(recharge.getOperatorName())
                .planName(recharge.getPlanName())
                .amount(recharge.getAmount())
                .expiryDate(recharge.getPlanExpiryDate())
                .build();
    }
    
    // ========== ENTERPRISE BI ANALYTICS IMPLEMENTATION ==========
    
    @Override
    public RechargeAnalyticsResponse getRechargeAnalytics(Integer days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = days != null && days > 0 
                ? now.minusDays(days) 
                : LocalDateTime.of(2020, 1, 1, 0, 0); // All time default
        
        LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDate today = LocalDate.now();
        
        // Volume Metrics (filtered by date range) - ONLY SUCCESS transactions
        long totalRecharges = rechargeRepository.countRechargesBetweenDates(
                startDate, now, RechargeStatus.SUCCESS);
        
        long todayRecharges = rechargeRepository.countRechargesBetweenDates(
                todayStart, now, RechargeStatus.SUCCESS);
        long monthRecharges = rechargeRepository.countRechargesBetweenDates(
                monthStart, now, RechargeStatus.SUCCESS);
        
        BigDecimal totalRevenue = rechargeRepository.sumAmountBetweenDates(
                startDate, now, RechargeStatus.SUCCESS);
        BigDecimal todayRevenue = rechargeRepository.sumAmountBetweenDates(
                todayStart, now, RechargeStatus.SUCCESS);
        BigDecimal monthRevenue = rechargeRepository.sumAmountBetweenDates(
                monthStart, now, RechargeStatus.SUCCESS);
        
        // Success Rate (filtered by date range)
        long successCount = rechargeRepository.countRechargesBetweenDates(
                startDate, now, RechargeStatus.SUCCESS);
        long failedCount = rechargeRepository.countRechargesBetweenDates(
                startDate, now, RechargeStatus.FAILED);
        long pendingCount = rechargeRepository.countRechargesBetweenDates(
                startDate, now, RechargeStatus.PROCESSING);
        
        double successRate = totalRecharges > 0 
                ? (successCount * 100.0 / totalRecharges) 
                : 0.0;
        
        // Active vs Expired
        long activeRecharges = rechargeRepository.countActiveRecharges(RechargeStatus.SUCCESS, today);
        long expiredRecharges = rechargeRepository.countExpiredRecharges(RechargeStatus.SUCCESS, today);
        
        double activeRatio = (activeRecharges + expiredRecharges) > 0
                ? (activeRecharges * 100.0 / (activeRecharges + expiredRecharges))
                : 0.0;
        
        // Top Plans (Top 10 by revenue) - FILTERED BY DATE RANGE
        List<Object[]> topPlansData = rechargeRepository.findTopPlansByRevenueWithDateFilter(
                RechargeStatus.SUCCESS, startDate, now, PageRequest.of(0, 10));
        List<PlanPerformanceStats> topPlans = topPlansData.stream()
                .map(row -> {
                    // Query returns: planId, planName, operatorId, operatorName, COUNT, SUM, AVG
                    BigDecimal planRevenue = row[5] instanceof BigDecimal 
                            ? (BigDecimal) row[5] 
                            : BigDecimal.valueOf(((Number) row[5]).doubleValue());
                    BigDecimal planAvgAmount = row[6] instanceof BigDecimal 
                            ? (BigDecimal) row[6] 
                            : BigDecimal.valueOf(((Number) row[6]).doubleValue());
                    
                    return PlanPerformanceStats.builder()
                            .planId((Long) row[0])
                            .planName((String) row[1])
                            .operatorId((Long) row[2])
                            .operatorName((String) row[3])
                            .rechargeCount((Long) row[4])
                            .totalRevenue(planRevenue)
                            .averageAmount(planAvgAmount)
                            .build();
                })
                .collect(Collectors.toList());
        
        // Operator Market Share - FILTERED BY DATE RANGE
        List<Object[]> operatorShareData = rechargeRepository.findOperatorMarketShareWithDateFilter(
                RechargeStatus.SUCCESS, startDate, now);
        BigDecimal totalMarketRevenue = operatorShareData.stream()
                .map(row -> {
                    Object revenueObj = row[3];
                    return revenueObj instanceof BigDecimal 
                            ? (BigDecimal) revenueObj 
                            : BigDecimal.valueOf(((Number) revenueObj).doubleValue());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<OperatorMarketShare> operatorShares = operatorShareData.stream()
                .map(row -> {
                    Object revenueObj = row[3];
                    BigDecimal operatorRevenue = revenueObj instanceof BigDecimal 
                            ? (BigDecimal) revenueObj 
                            : BigDecimal.valueOf(((Number) revenueObj).doubleValue());
                    
                    double marketShare = totalMarketRevenue.compareTo(BigDecimal.ZERO) > 0
                            ? operatorRevenue.multiply(BigDecimal.valueOf(100))
                                    .divide(totalMarketRevenue, 2, BigDecimal.ROUND_HALF_UP)
                                    .doubleValue()
                            : 0.0;
                    
                    return OperatorMarketShare.builder()
                            .operatorId((Long) row[0])
                            .operatorName((String) row[1])
                            .rechargeCount((Long) row[2])
                            .totalRevenue(operatorRevenue)
                            .marketSharePercentage(marketShare)
                            .build();
                })
                .collect(Collectors.toList());
        
        return RechargeAnalyticsResponse.builder()
                .totalRecharges(totalRecharges)
                .todayRecharges(todayRecharges)
                .monthRecharges(monthRecharges)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .monthRevenue(monthRevenue != null ? monthRevenue : BigDecimal.ZERO)
                .successRate(successRate)
                .successCount(successCount)
                .failedCount(failedCount)
                .pendingCount(pendingCount)
                .activeRecharges(activeRecharges)
                .expiredRecharges(expiredRecharges)
                .activeRatio(activeRatio)
                .topPlans(topPlans)
                .operatorShares(operatorShares)
                .build();
    }
    
    @Override
    public OperatorPlansResponse getOperatorPlans(Long operatorId) {
        // Get operator-level stats
        List<Object[]> operatorData = rechargeRepository.findOperatorMarketShare(RechargeStatus.SUCCESS);
        Object[] operatorStats = operatorData.stream()
                .filter(row -> ((Long) row[0]).equals(operatorId))
                .findFirst()
                .orElse(new Object[]{operatorId, "Unknown", 0L, BigDecimal.ZERO});
        
        // Get all plans for this operator
        List<Object[]> plansData = rechargeRepository.findTopPlansByRevenue(
                RechargeStatus.SUCCESS, PageRequest.of(0, 1000));
        
        List<PlanPerformanceStats> plans = plansData.stream()
                .map(row -> {
                    // Query returns: planId, planName, operatorId, operatorName, COUNT, SUM, AVG
                    BigDecimal planRevenue = row[5] instanceof BigDecimal 
                            ? (BigDecimal) row[5] 
                            : BigDecimal.valueOf(((Number) row[5]).doubleValue());
                    BigDecimal planAvgAmount = row[6] instanceof BigDecimal 
                            ? (BigDecimal) row[6] 
                            : BigDecimal.valueOf(((Number) row[6]).doubleValue());
                    
                    return PlanPerformanceStats.builder()
                            .planId((Long) row[0])
                            .planName((String) row[1])
                            .operatorId((Long) row[2])
                            .operatorName((String) row[3])
                            .rechargeCount((Long) row[4])
                            .totalRevenue(planRevenue)
                            .averageAmount(planAvgAmount)
                            .build();
                })
                .filter(plan -> plan.getOperatorName().equals((String) operatorStats[1]))
                .collect(Collectors.toList());
        
        Object revenueObj = operatorStats[3];
        BigDecimal operatorTotalRevenue = revenueObj instanceof BigDecimal 
                ? (BigDecimal) revenueObj 
                : BigDecimal.valueOf(((Number) revenueObj).doubleValue());
        
        return OperatorPlansResponse.builder()
                .operatorId(operatorId)
                .operatorName((String) operatorStats[1])
                .totalRecharges((Long) operatorStats[2])
                .totalRevenue(operatorTotalRevenue)
                .plans(plans)
                .build();
    }
    
    @Override
    public Page<RechargeResponse> getPlanRechargeHistory(Long planId, RechargeStatus status, String search, Pageable pageable) {
        Page<Recharge> recharges;
        
        if (search != null && !search.trim().isEmpty()) {
            recharges = rechargeRepository.searchByPlanId(planId, search.trim(), pageable);
        } else if (status != null) {
            recharges = rechargeRepository.findByPlanIdAndStatus(planId, status, pageable);
        } else {
            recharges = rechargeRepository.findAllByPlanId(planId, pageable);
        }
        
        return recharges.map(this::mapToResponse);
    }
    
    @Override
    public Page<RechargeResponse> getOperatorRechargeHistory(Long operatorId, RechargeStatus status, String search, Pageable pageable) {
        Page<Recharge> recharges;
        
        if (search != null && !search.trim().isEmpty()) {
            recharges = rechargeRepository.searchByOperatorId(operatorId, search.trim(), pageable);
        } else if (status != null) {
            recharges = rechargeRepository.findByOperatorIdAndStatus(operatorId, status, pageable);
        } else {
            recharges = rechargeRepository.findAllByOperatorId(operatorId, pageable);
        }
        
        return recharges.map(this::mapToResponse);
    }
    
    @Override
    public Page<RechargeResponse> getUserRechargeHistory(Long userId, RechargeStatus status, String search, Pageable pageable) {
        Page<Recharge> recharges;
        
        if (search != null && !search.trim().isEmpty()) {
            // Search by mobile number, operator name, or plan name
            recharges = rechargeRepository.searchByUserId(userId, search.trim(), pageable);
        } else if (status != null) {
            recharges = rechargeRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            recharges = rechargeRepository.findByUserId(userId, pageable);
        }
        
        return recharges.map(this::mapToResponse);
    }
}

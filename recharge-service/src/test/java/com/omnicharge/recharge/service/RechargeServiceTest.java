package com.omnicharge.recharge.service;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.common.exception.BadRequestException;
import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.recharge.client.OperatorServiceClient;
import com.omnicharge.recharge.client.UserServiceClient;
import com.omnicharge.recharge.dto.*;
import com.omnicharge.recharge.entity.Recharge;
import com.omnicharge.recharge.entity.RechargeStatus;
import com.omnicharge.recharge.messaging.RechargeEventProducer;
import com.omnicharge.recharge.repository.RechargeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RechargeServiceTest {

    @Mock
    private RechargeRepository rechargeRepository;

    @Mock
    private OperatorServiceClient operatorServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private RechargeEventProducer rechargeEventProducer;

    @Mock
    private com.omnicharge.common.logging.LogEventPublisher logEventPublisher;

    @InjectMocks
    private RechargeService rechargeService;

    private RechargeRequest rechargeRequest;
    private PlanResponse planResponse;
    private UserProfileResponse userProfileResponse;
    private Recharge recharge;

    @BeforeEach
    void setUp() {
        rechargeRequest = new RechargeRequest();
        rechargeRequest.setMobileNumber("9876543210");
        rechargeRequest.setOperatorId(1L);
        rechargeRequest.setPlanId(10L);
        rechargeRequest.setPaymentMethod("UPI");

        planResponse = PlanResponse.builder()
                .id(10L)
                .operatorId(1L)
                .operatorName("Airtel")
                .planName("Unlimited 5G")
                .price(new BigDecimal("299.00"))
                .validityDays(28)
                .isActive(true)
                .build();

        userProfileResponse = UserProfileResponse.builder()
                .id(1L)
                .email("test@example.com")
                .mobileNumber("9876543210")
                .build();

        recharge = new Recharge();
        recharge.setId(100L);
        recharge.setRechargeId("OMNI-A1B2C3D4");
        recharge.setUserId(1L);
        recharge.setMobileNumber("9876543210");
        recharge.setOperatorId(1L);
        recharge.setOperatorName("Airtel");
        recharge.setPlanId(10L);
        recharge.setPlanName("Unlimited 5G");
        recharge.setAmount(new BigDecimal("299.00"));
        recharge.setPlanValidityDays(28);
        recharge.setPlanExpiryDate(LocalDate.now().plusDays(28));
        recharge.setStatus(RechargeStatus.SUCCESS);
        recharge.setCreatedDate(LocalDateTime.now());
    }

    @Test
    void initiateRecharge_Success() {
        when(operatorServiceClient.getPlan(anyLong()))
                .thenReturn(ApiResponse.success("Success", planResponse));
        when(userServiceClient.getUserById(anyLong()))
                .thenReturn(ApiResponse.success("Success", userProfileResponse));

        when(rechargeRepository.save(any(Recharge.class))).thenAnswer(invocation -> {
            Recharge r = invocation.getArgument(0);
            r.setRechargeId("OMNI-MOCK123");
            return r;
        });

        doNothing().when(rechargeEventProducer).publishRechargeInitiated(any());

        RechargeResponse response = rechargeService.initiateRecharge(1L, rechargeRequest);

        assertNotNull(response);
        assertEquals(RechargeStatus.PROCESSING, response.getStatus());
        verify(rechargeEventProducer, times(1)).publishRechargeInitiated(any());
        verify(rechargeRepository, times(2)).save(any(Recharge.class));
    }

    @Test
    void initiateRecharge_CircuitBreaker_OperatorDown_ThrowsException() {
        // Simulates Resilience4j fallback returning null or an error map from circuit breaker intercept
        when(operatorServiceClient.getPlan(anyLong())).thenReturn(null);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
            rechargeService.initiateRecharge(1L, rechargeRequest)
        );
        assertTrue(ex.getMessage().contains("Operator Service is temporarily unavailable"));
        verify(rechargeRepository, never()).save(any());
    }

    @Test
    void initiateRecharge_OperatorAPI_ReturnsError_ThrowsException() {
        when(operatorServiceClient.getPlan(anyLong())).thenReturn(ApiResponse.error("Internal Server Error"));

        assertThrows(BadRequestException.class, () -> rechargeService.initiateRecharge(1L, rechargeRequest));
    }

    @Test
    void initiateRecharge_PlanInactive_ThrowsException() {
        planResponse.setIsActive(false);
        when(operatorServiceClient.getPlan(anyLong())).thenReturn(ApiResponse.success("Success", planResponse));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                rechargeService.initiateRecharge(1L, rechargeRequest)
        );
        assertEquals("Invalid or inactive plan", ex.getMessage());
    }

    @Test
    void initiateRecharge_PlanOperatorMismatch_ThrowsException() {
        planResponse.setOperatorId(99L); // Differs from rechargeRequest (1L)
        when(operatorServiceClient.getPlan(anyLong())).thenReturn(ApiResponse.success("Success", planResponse));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                rechargeService.initiateRecharge(1L, rechargeRequest)
        );
        assertEquals("Plan does not belong to the specified operator", ex.getMessage());
    }

    @Test
    void initiateRecharge_UserServiceDown_StillSucceedsWithoutUserDetails() {
        when(operatorServiceClient.getPlan(anyLong()))
                .thenReturn(ApiResponse.success("Success", planResponse));
        // Fallback or outage returns null or error for UserServiceClient
        when(userServiceClient.getUserById(anyLong())).thenThrow(new RuntimeException("API DOWN"));

        when(rechargeRepository.save(any(Recharge.class))).thenAnswer(i -> i.getArgument(0));

        // It should gracefully catch the exception from UserServiceClient and proceed
        assertDoesNotThrow(() -> rechargeService.initiateRecharge(1L, rechargeRequest));
        verify(rechargeEventProducer, times(1)).publishRechargeInitiated(any());
    }

    @Test
    void getRechargeById_Success() {
        when(rechargeRepository.findByRechargeId("OMNI-A1B2C3D4")).thenReturn(Optional.of(recharge));

        RechargeResponse response = rechargeService.getRechargeById("OMNI-A1B2C3D4", 1L);

        assertNotNull(response);
        assertEquals("OMNI-A1B2C3D4", response.getRechargeId());
    }

    @Test
    void getRechargeById_Unauthorized() {
        when(rechargeRepository.findByRechargeId("OMNI-A1B2C3D4")).thenReturn(Optional.of(recharge));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                rechargeService.getRechargeById("OMNI-A1B2C3D4", 99L) // Wrong user ID
        );
        assertEquals("Unauthorized access to recharge", ex.getMessage());
    }

    @Test
    void getExpiringRecharges_Success() {
        when(rechargeRepository.findByStatusAndPlanExpiryDate(eq(RechargeStatus.SUCCESS), any(LocalDate.class)))
                .thenReturn(List.of(recharge));
        when(userServiceClient.getUserById(1L)).thenReturn(ApiResponse.success("OK", userProfileResponse));

        List<ExpiringRechargeResponse> responses = rechargeService.getExpiringRecharges(5);

        assertFalse(responses.isEmpty());
        assertEquals("test@example.com", responses.get(0).getUserEmail());
    }

    @Test
    void getExpiringRecharges_UserServiceFails_ReturnsNullEmail() {
        when(rechargeRepository.findByStatusAndPlanExpiryDate(eq(RechargeStatus.SUCCESS), any(LocalDate.class)))
                .thenReturn(List.of(recharge));
        when(userServiceClient.getUserById(1L)).thenThrow(new RuntimeException("Service down"));

        List<ExpiringRechargeResponse> responses = rechargeService.getExpiringRecharges(5);

        assertFalse(responses.isEmpty());
        assertNull(responses.get(0).getUserEmail());
    }

    @Test
    void getExpiredToday_Success() {
        when(rechargeRepository.findByStatusAndPlanExpiryDate(eq(RechargeStatus.SUCCESS), any(LocalDate.class)))
                .thenReturn(List.of(recharge));
        when(userServiceClient.getUserById(1L)).thenReturn(ApiResponse.success("OK", userProfileResponse));

        List<ExpiringRechargeResponse> responses = rechargeService.getExpiredToday();

        assertFalse(responses.isEmpty());
    }

    @Test
    void markAsExpired_Success() {
        when(rechargeRepository.findByRechargeId("OMNI-A1B2C3D4")).thenReturn(Optional.of(recharge));
        when(rechargeRepository.save(any(Recharge.class))).thenReturn(recharge);

        assertDoesNotThrow(() -> rechargeService.markAsExpired("OMNI-A1B2C3D4"));
        verify(rechargeRepository, times(1)).save(any(Recharge.class));
        verify(logEventPublisher, times(1)).publish(any());
    }

    @Test
    void markAsExpired_NotFound_ThrowsException() {
        when(rechargeRepository.findByRechargeId("INVALID")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> rechargeService.markAsExpired("INVALID"));
    }

    @Test
    void getRechargeHistory_Success() {
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<RechargeResponse> result = rechargeService.getRechargeHistory(1L, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getRechargeStatus_Success() {
        when(rechargeRepository.findByRechargeId("OMNI-A1B2C3D4")).thenReturn(Optional.of(recharge));

        String status = rechargeService.getRechargeStatus("OMNI-A1B2C3D4");

        assertEquals("SUCCESS", status);
    }

    @Test
    void getRechargeStatus_NotFound_ThrowsException() {
        when(rechargeRepository.findByRechargeId("INVALID")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> rechargeService.getRechargeStatus("INVALID"));
    }

    @Test
    void getAllRecharges_Success() {
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<RechargeResponse> result = rechargeService.getAllRecharges(PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getRechargeStats_Success() {
        when(rechargeRepository.count()).thenReturn(100L);
        when(rechargeRepository.countByStatus(RechargeStatus.SUCCESS)).thenReturn(80L);
        when(rechargeRepository.countByStatus(RechargeStatus.FAILED)).thenReturn(20L);
        
        Recharge successRecharge = new Recharge();
        successRecharge.setStatus(RechargeStatus.SUCCESS);
        successRecharge.setAmount(new BigDecimal("500.00"));
        
        when(rechargeRepository.findByCreatedDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(successRecharge));

        RechargeStatsResponse stats = rechargeService.getRechargeStats();

        assertEquals(100L, stats.getTotalRecharges());
        assertEquals(80L, stats.getSuccessCount());
        assertEquals(20L, stats.getFailedCount());
        assertEquals(new BigDecimal("500.00"), stats.getTotalAmount());
    }

    @Test
    void getRechargeStats_NoSuccessfulRecharges_TotalAmountIsZero() {
        when(rechargeRepository.count()).thenReturn(50L);
        when(rechargeRepository.countByStatus(RechargeStatus.SUCCESS)).thenReturn(0L);
        when(rechargeRepository.countByStatus(RechargeStatus.FAILED)).thenReturn(50L);
        
        // Return recharges with non-SUCCESS status (lambda filter branch)
        Recharge failedRecharge = new Recharge();
        failedRecharge.setStatus(RechargeStatus.FAILED);
        failedRecharge.setAmount(new BigDecimal("300.00"));
        
        when(rechargeRepository.findByCreatedDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(failedRecharge));

        RechargeStatsResponse stats = rechargeService.getRechargeStats();

        assertEquals(50L, stats.getTotalRecharges());
        assertEquals(0L, stats.getSuccessCount());
        assertEquals(50L, stats.getFailedCount());
        assertEquals(BigDecimal.ZERO, stats.getTotalAmount()); // Lambda filter excludes non-SUCCESS
    }

    @Test
    void getRechargeAnalytics_WithDaysFilter_Success() {
        // Mock volume metrics
        when(rechargeRepository.countRechargesBetweenDates(any(), any(), eq(RechargeStatus.SUCCESS)))
                .thenReturn(100L, 10L, 50L, 100L, 5L);
        when(rechargeRepository.sumAmountBetweenDates(any(), any(), eq(RechargeStatus.SUCCESS)))
                .thenReturn(new BigDecimal("50000"), new BigDecimal("5000"), new BigDecimal("25000"));
        when(rechargeRepository.countRechargesBetweenDates(any(), any(), eq(RechargeStatus.FAILED)))
                .thenReturn(10L);
        when(rechargeRepository.countRechargesBetweenDates(any(), any(), eq(RechargeStatus.PROCESSING)))
                .thenReturn(5L);
        
        // Mock active/expired
        when(rechargeRepository.countActiveRecharges(eq(RechargeStatus.SUCCESS), any(LocalDate.class)))
                .thenReturn(80L);
        when(rechargeRepository.countExpiredRecharges(eq(RechargeStatus.SUCCESS), any(LocalDate.class)))
                .thenReturn(20L);
        
        // Mock top plans
        Object[] planRow = {1L, "Plan A", 1L, "Airtel", 50L, new BigDecimal("25000"), new BigDecimal("500")};
        when(rechargeRepository.findTopPlansByRevenueWithDateFilter(eq(RechargeStatus.SUCCESS), any(), any(), any()))
                .thenReturn(Collections.singletonList(planRow));
        
        // Mock operator shares
        Object[] operatorRow = {1L, "Airtel", 100L, new BigDecimal("50000")};
        when(rechargeRepository.findOperatorMarketShareWithDateFilter(eq(RechargeStatus.SUCCESS), any(), any()))
                .thenReturn(Collections.singletonList(operatorRow));

        RechargeAnalyticsResponse analytics = rechargeService.getRechargeAnalytics(30);

        assertNotNull(analytics);
        assertEquals(100L, analytics.getTotalRecharges());
        assertEquals(new BigDecimal("50000"), analytics.getTotalRevenue());
        assertEquals(1, analytics.getTopPlans().size());
        assertEquals(1, analytics.getOperatorShares().size());
    }

    @Test
    void getRechargeAnalytics_NullDays_UsesAllTime() {
        when(rechargeRepository.countRechargesBetweenDates(any(), any(), any())).thenReturn(0L);
        when(rechargeRepository.sumAmountBetweenDates(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(rechargeRepository.countActiveRecharges(any(), any())).thenReturn(0L);
        when(rechargeRepository.countExpiredRecharges(any(), any())).thenReturn(0L);
        when(rechargeRepository.findTopPlansByRevenueWithDateFilter(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(rechargeRepository.findOperatorMarketShareWithDateFilter(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        RechargeAnalyticsResponse analytics = rechargeService.getRechargeAnalytics(null);

        assertNotNull(analytics);
        assertEquals(0L, analytics.getTotalRecharges());
    }

    @Test
    void getRechargeAnalytics_ZeroTotalRecharges_SuccessRateIsZero() {
        when(rechargeRepository.countRechargesBetweenDates(any(), any(), any())).thenReturn(0L);
        when(rechargeRepository.sumAmountBetweenDates(any(), any(), any())).thenReturn(null);
        when(rechargeRepository.countActiveRecharges(any(), any())).thenReturn(0L);
        when(rechargeRepository.countExpiredRecharges(any(), any())).thenReturn(0L);
        when(rechargeRepository.findTopPlansByRevenueWithDateFilter(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(rechargeRepository.findOperatorMarketShareWithDateFilter(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        RechargeAnalyticsResponse analytics = rechargeService.getRechargeAnalytics(7);

        assertEquals(0.0, analytics.getSuccessRate());
        assertEquals(BigDecimal.ZERO, analytics.getTotalRevenue());
    }

    @Test
    void getRechargeAnalytics_NumberTypeConversion_Success() {
        when(rechargeRepository.countRechargesBetweenDates(any(), any(), any())).thenReturn(50L);
        when(rechargeRepository.sumAmountBetweenDates(any(), any(), any())).thenReturn(new BigDecimal("10000"));
        when(rechargeRepository.countActiveRecharges(any(), any())).thenReturn(40L);
        when(rechargeRepository.countExpiredRecharges(any(), any())).thenReturn(10L);
        
        // Test Number type conversion (Double instead of BigDecimal)
        Object[] planRow = {1L, "Plan B", 2L, "Jio", 30L, 15000.0, 500.0};
        when(rechargeRepository.findTopPlansByRevenueWithDateFilter(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(planRow));
        
        Object[] operatorRow = {2L, "Jio", 50L, 25000.0};
        when(rechargeRepository.findOperatorMarketShareWithDateFilter(any(), any(), any()))
                .thenReturn(Collections.singletonList(operatorRow));

        RechargeAnalyticsResponse analytics = rechargeService.getRechargeAnalytics(15);

        assertNotNull(analytics);
        assertEquals("Plan B", analytics.getTopPlans().get(0).getPlanName());
        assertEquals("Jio", analytics.getOperatorShares().get(0).getOperatorName());
    }

    @Test
    void getOperatorPlans_Success() {
        Object[] operatorData = {1L, "Airtel", 100L, new BigDecimal("50000")};
        when(rechargeRepository.findOperatorMarketShare(RechargeStatus.SUCCESS))
                .thenReturn(Collections.singletonList(operatorData));
        
        Object[] planRow = {1L, "Plan A", 1L, "Airtel", 50L, new BigDecimal("25000"), new BigDecimal("500")};
        when(rechargeRepository.findTopPlansByRevenue(eq(RechargeStatus.SUCCESS), any()))
                .thenReturn(Collections.singletonList(planRow));

        OperatorPlansResponse response = rechargeService.getOperatorPlans(1L);

        assertNotNull(response);
        assertEquals("Airtel", response.getOperatorName());
        assertEquals(1, response.getPlans().size());
    }

    @Test
    void getOperatorPlans_OperatorNotFound_ReturnsUnknown() {
        when(rechargeRepository.findOperatorMarketShare(RechargeStatus.SUCCESS))
                .thenReturn(Collections.emptyList());
        when(rechargeRepository.findTopPlansByRevenue(any(), any()))
                .thenReturn(Collections.emptyList());

        OperatorPlansResponse response = rechargeService.getOperatorPlans(999L);

        assertEquals("Unknown", response.getOperatorName());
        assertTrue(response.getPlans().isEmpty());
    }

    @Test
    void getOperatorPlans_NumberTypeConversion_Success() {
        Object[] operatorData = {1L, "Vodafone", 80L, 40000.0};
        when(rechargeRepository.findOperatorMarketShare(RechargeStatus.SUCCESS))
                .thenReturn(Collections.singletonList(operatorData));
        
        Object[] planRow = {5L, "Plan X", 1L, "Vodafone", 40L, 20000.0, 500.0};
        when(rechargeRepository.findTopPlansByRevenue(any(), any()))
                .thenReturn(Collections.singletonList(planRow));

        OperatorPlansResponse response = rechargeService.getOperatorPlans(1L);

        assertEquals("Vodafone", response.getOperatorName());
        assertEquals(new BigDecimal("40000.0"), response.getTotalRevenue());
    }

    @Test
    void getPlanRechargeHistory_WithSearch_Success() {
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.searchByPlanId(eq(10L), eq("test"), any(Pageable.class)))
                .thenReturn(page);

        Page<RechargeResponse> result = rechargeService.getPlanRechargeHistory(10L, null, "test", PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getPlanRechargeHistory_WithStatus_Success() {
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.findByPlanIdAndStatus(eq(10L), eq(RechargeStatus.SUCCESS), any(Pageable.class)))
                .thenReturn(page);

        Page<RechargeResponse> result = rechargeService.getPlanRechargeHistory(10L, RechargeStatus.SUCCESS, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getPlanRechargeHistory_NoFilters_Success() {
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.findAllByPlanId(eq(10L), any(Pageable.class)))
                .thenReturn(page);

        Page<RechargeResponse> result = rechargeService.getPlanRechargeHistory(10L, null, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getPlanRechargeHistory_EmptySearch_UsesNoFilter() {
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.findAllByPlanId(eq(10L), any(Pageable.class)))
                .thenReturn(page);

        Page<RechargeResponse> result = rechargeService.getPlanRechargeHistory(10L, null, "  ", PageRequest.of(0, 10));

        assertNotNull(result);
        verify(rechargeRepository, never()).searchByPlanId(anyLong(), anyString(), any());
    }

    @Test
    void getPlanRechargeHistory_WithStatusAndSearch_UsesSearch() {
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.searchByPlanId(eq(10L), eq("premium"), any(Pageable.class)))
                .thenReturn(page);

        Page<RechargeResponse> result = rechargeService.getPlanRechargeHistory(10L, RechargeStatus.SUCCESS, "premium", PageRequest.of(0, 10));

        assertNotNull(result);
        verify(rechargeRepository).searchByPlanId(eq(10L), eq("premium"), any());
        verify(rechargeRepository, never()).findByPlanIdAndStatus(anyLong(), any(), any());
    }

    @Test
    void getOperatorRechargeHistory_WithSearch_Success() {
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.searchByOperatorId(eq(1L), eq("airtel"), any(Pageable.class)))
                .thenReturn(page);

        Page<RechargeResponse> result = rechargeService.getOperatorRechargeHistory(1L, null, "airtel", PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getOperatorRechargeHistory_WithStatus_Success() {
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.findByOperatorIdAndStatus(eq(1L), eq(RechargeStatus.SUCCESS), any(Pageable.class)))
                .thenReturn(page);

        Page<RechargeResponse> result = rechargeService.getOperatorRechargeHistory(1L, RechargeStatus.SUCCESS, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getOperatorRechargeHistory_NoFilters_Success() {
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.findAllByOperatorId(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        Page<RechargeResponse> result = rechargeService.getOperatorRechargeHistory(1L, null, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getOperatorRechargeHistory_EmptySearch_UsesNoFilter() {
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.findAllByOperatorId(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        Page<RechargeResponse> result = rechargeService.getOperatorRechargeHistory(1L, null, "  ", PageRequest.of(0, 10));

        assertNotNull(result);
        verify(rechargeRepository, never()).searchByOperatorId(anyLong(), anyString(), any());
    }

    @Test
    void getOperatorRechargeHistory_WithStatusAndSearch_UsesSearch() {
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.searchByOperatorId(eq(1L), eq("test"), any(Pageable.class)))
                .thenReturn(page);

        Page<RechargeResponse> result = rechargeService.getOperatorRechargeHistory(1L, RechargeStatus.SUCCESS, "test", PageRequest.of(0, 10));

        assertNotNull(result);
        verify(rechargeRepository).searchByOperatorId(eq(1L), eq("test"), any());
        verify(rechargeRepository, never()).findByOperatorIdAndStatus(anyLong(), any(), any());
    }

    @Test
    void getUserRechargeHistory_WithSearch_Success() {
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.searchByUserId(eq(1L), eq("9876"), any(Pageable.class)))
                .thenReturn(page);

        Page<RechargeResponse> result = rechargeService.getUserRechargeHistory(1L, null, "9876", PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getUserRechargeHistory_WithStatus_Success() {
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.findByUserIdAndStatus(eq(1L), eq(RechargeStatus.SUCCESS), any(Pageable.class)))
                .thenReturn(page);

        Page<RechargeResponse> result = rechargeService.getUserRechargeHistory(1L, RechargeStatus.SUCCESS, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getUserRechargeHistory_NoFilters_Success() {
        Page<Recharge> page = new PageImpl<>(List.of(recharge));
        when(rechargeRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        Page<RechargeResponse> result = rechargeService.getUserRechargeHistory(1L, null, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void mapToResponse_UserServiceReturnsNull_UserFullNameIsNull() {
        when(rechargeRepository.findByRechargeId("OMNI-A1B2C3D4")).thenReturn(Optional.of(recharge));
        when(userServiceClient.getUserById(1L)).thenReturn(null);

        RechargeResponse response = rechargeService.getRechargeById("OMNI-A1B2C3D4", 1L);

        assertNull(response.getUserFullName());
    }

    @Test
    void mapToResponse_UserServiceReturnsError_UserFullNameIsNull() {
        when(rechargeRepository.findByRechargeId("OMNI-A1B2C3D4")).thenReturn(Optional.of(recharge));
        when(userServiceClient.getUserById(1L)).thenReturn(ApiResponse.error("Service unavailable"));

        RechargeResponse response = rechargeService.getRechargeById("OMNI-A1B2C3D4", 1L);

        assertNull(response.getUserFullName());
    }

    @Test
    void mapToResponse_UserServiceThrowsException_UserFullNameIsNull() {
        when(rechargeRepository.findByRechargeId("OMNI-A1B2C3D4")).thenReturn(Optional.of(recharge));
        when(userServiceClient.getUserById(1L)).thenThrow(new RuntimeException("Connection timeout"));

        RechargeResponse response = rechargeService.getRechargeById("OMNI-A1B2C3D4", 1L);

        assertNull(response.getUserFullName());
    }
}

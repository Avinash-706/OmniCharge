package com.omnicharge.operator.service;

import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.operator.dto.PlanRequest;
import com.omnicharge.operator.dto.PlanResponse;
import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.entity.PlanCategory;
import com.omnicharge.operator.messaging.OperatorEventPublisher;
import com.omnicharge.operator.repository.OperatorRepository;
import com.omnicharge.operator.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private OperatorEventPublisher operatorEventPublisher;

    @Mock
    private com.omnicharge.common.logging.LogEventPublisher logEventPublisher;

    @Mock
    private SystemCacheService systemCacheService;

    @InjectMocks
    private PlanService planService;

    private Operator operator;
    private Plan plan;
    private PlanRequest planRequest;

    @BeforeEach
    void setUp() {
        operator = new Operator();
        operator.setId(1L);
        operator.setName("Airtel");
        operator.setIsActive(true);

        plan = new Plan();
        plan.setId(10L);
        plan.setOperator(operator);
        plan.setPlanName("Unlimited 5G");
        plan.setPrice(new BigDecimal("299.00"));
        plan.setCategory(PlanCategory.UNLIMITED);
        plan.setIsActive(true);

        planRequest = new PlanRequest();
        planRequest.setPlanName("Unlimited 5G Mod");
        planRequest.setPrice(new BigDecimal("399.00"));
        planRequest.setValidityDays(28);
        planRequest.setCategory(PlanCategory.UNLIMITED);
    }

    @Test
    void createPlan_Success() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(planRepository.save(any(Plan.class))).thenReturn(plan);
        doNothing().when(operatorEventPublisher).publishPlanUpdatedEvent(1L);

        PlanResponse result = planService.createPlan(1L, planRequest);

        assertNotNull(result);
        assertEquals(1L, result.getOperatorId());
        verify(planRepository, times(1)).save(any(Plan.class));
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
    }

    @Test
    void createPlan_OperatorNotFound() {
        when(operatorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> planService.createPlan(99L, planRequest));
        verify(planRepository, never()).save(any(Plan.class));
    }

    @Test
    void updatePlan_Success() {
        when(planRepository.findById(10L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(Plan.class))).thenReturn(plan);
        doNothing().when(operatorEventPublisher).publishPlanUpdatedEvent(1L);

        PlanResponse result = planService.updatePlan(10L, planRequest);

        assertNotNull(result);
        assertEquals(1L, result.getOperatorId());
        verify(planRepository, times(1)).save(plan);
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
    }

    @Test
    void deletePlan_Success() {
        when(planRepository.findById(10L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(Plan.class))).thenReturn(plan);
        doNothing().when(operatorEventPublisher).publishPlanUpdatedEvent(1L);

        planService.deletePlan(10L);

        assertFalse(plan.getIsActive());
        verify(planRepository, times(1)).save(plan);
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
    }

    @Test
    void getPlanStats_Success() {
        when(planRepository.count()).thenReturn(100L);
        when(planRepository.countByIsActive(true)).thenReturn(80L);
        when(planRepository.countByIsActive(false)).thenReturn(20L);
        when(planRepository.countActivePlansByCategory()).thenReturn(
            java.util.List.of(
                new Object[]{PlanCategory.UNLIMITED, 30L},
                new Object[]{PlanCategory.DATA, 25L},
                new Object[]{PlanCategory.RECOMMENDED, 15L},
                new Object[]{PlanCategory.TALKTIME, 10L}
            )
        );

        com.omnicharge.operator.dto.PlanStatsResponse result = planService.getPlanStats();

        assertNotNull(result);
        assertEquals(100L, result.getTotalPlans());
        assertEquals(80L, result.getActivePlans());
        assertEquals(20L, result.getInactivePlans());
        assertEquals(4, result.getPlansByCategory().size());
        assertEquals(30L, result.getPlansByCategory().get("UNLIMITED"));
        verify(planRepository, times(1)).count();
        verify(planRepository, times(1)).countByIsActive(true);
        verify(planRepository, times(1)).countByIsActive(false);
        verify(planRepository, times(1)).countActivePlansByCategory();
    }

    @Test
    void getPlansByOperator_Success() {
        when(planRepository.findByOperatorIdAndIsActive(1L, true)).thenReturn(java.util.List.of(plan));

        java.util.List<PlanResponse> result = planService.getPlansByOperator(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getOperatorId());
        verify(planRepository, times(1)).findByOperatorIdAndIsActive(1L, true);
    }

    @Test
    void getPlansByOperatorAndStatus_Success() {
        when(planRepository.findByOperatorIdAndStatus(1L, true)).thenReturn(java.util.List.of(plan));

        java.util.List<PlanResponse> result = planService.getPlansByOperatorAndStatus(1L, true);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getOperatorId());
        verify(planRepository, times(1)).findByOperatorIdAndStatus(1L, true);
    }

    @Test
    void searchPlans_Success() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Plan> planPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of(plan));
        
        when(planRepository.searchActivePlans(1L, PlanCategory.UNLIMITED, new BigDecimal("200.00"), new BigDecimal("400.00"), pageable))
            .thenReturn(planPage);

        org.springframework.data.domain.Page<PlanResponse> result = planService.searchPlans(
            1L, PlanCategory.UNLIMITED, new BigDecimal("200.00"), new BigDecimal("400.00"), pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getOperatorId());
        verify(planRepository, times(1)).searchActivePlans(1L, PlanCategory.UNLIMITED, new BigDecimal("200.00"), new BigDecimal("400.00"), pageable);
    }

    @Test
    void getPlanById_Success() {
        when(planRepository.findActiveById(10L)).thenReturn(Optional.of(plan));

        PlanResponse result = planService.getPlanById(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(1L, result.getOperatorId());
        verify(planRepository, times(1)).findActiveById(10L);
    }

    @Test
    void getPlanById_NotFound() {
        when(planRepository.findActiveById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> planService.getPlanById(99L));
        verify(planRepository, times(1)).findActiveById(99L);
    }

    @Test
    void searchPlansWithStatus_Success() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Plan> planPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of(plan));
        
        when(planRepository.searchPlansWithStatus(1L, PlanCategory.UNLIMITED, true, pageable))
            .thenReturn(planPage);

        org.springframework.data.domain.Page<PlanResponse> result = planService.searchPlansWithStatus(
            1L, PlanCategory.UNLIMITED, true, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getOperatorId());
        verify(planRepository, times(1)).searchPlansWithStatus(1L, PlanCategory.UNLIMITED, true, pageable);
    }

    @Test
    void activatePlan_AlreadyActive() {
        plan.setIsActive(true);
        when(planRepository.findById(10L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(Plan.class))).thenReturn(plan);
        doNothing().when(operatorEventPublisher).publishPlanUpdatedEvent(1L);

        PlanResponse result = planService.activatePlan(10L);

        assertNotNull(result);
        assertTrue(result.getIsActive());
        verify(planRepository, times(1)).save(plan);
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
    }

    @Test
    void activatePlan_OperatorInactive() {
        plan.setIsActive(false);
        operator.setIsActive(false);
        when(planRepository.findById(10L)).thenReturn(Optional.of(plan));

        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> planService.activatePlan(10L));

        assertTrue(exception.getMessage().contains("Cannot activate plan"));
        assertTrue(exception.getMessage().contains("operator"));
        assertTrue(exception.getMessage().contains("inactive"));
        verify(planRepository, never()).save(any(Plan.class));
    }
}

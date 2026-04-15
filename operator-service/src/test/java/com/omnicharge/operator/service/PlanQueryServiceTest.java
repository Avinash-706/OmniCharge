package com.omnicharge.operator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.operator.dto.PlanResponse;
import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.entity.PlanCategory;
import com.omnicharge.operator.messaging.OperatorEventPublisher;
import com.omnicharge.operator.repository.PlanRepository;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanQueryServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private OperatorEventPublisher operatorEventPublisher;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PlanQueryService planQueryService;

    private Plan plan;
    private PlanResponse planResponse;

    @BeforeEach
    void setUp() {
        Operator operator = new Operator();
        operator.setId(1L);
        operator.setName("Jio");

        plan = new Plan();
        plan.setId(10L);
        plan.setOperator(operator);
        plan.setPrice(new BigDecimal("299.00"));
        plan.setCategory(PlanCategory.DATA);
        plan.setIsActive(true);

        planResponse = PlanResponse.builder()
                .id(10L)
                .price(new BigDecimal("299.00"))
                .operatorId(1L)
                .build();
    }

    @Test
    void getPlanById_CacheHit() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plan:detail:10")).thenReturn("{planJson}");
        when(objectMapper.readValue("{planJson}", PlanResponse.class)).thenReturn(planResponse);

        PlanResponse result = planQueryService.getPlanById(10L);

        assertNotNull(result);
        assertEquals(new BigDecimal("299.00"), result.getPrice());
        verify(planRepository, never()).findActiveById(anyLong()); // Validates Redis intercepted the query
    }

    @Test
    void getPlanById_FallbackToDB() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plan:detail:10")).thenReturn(null); // Cache Miss
        when(planRepository.findActiveById(10L)).thenReturn(Optional.of(plan));
        doNothing().when(operatorEventPublisher).publishPlanUpdatedEvent(1L);

        PlanResponse result = planQueryService.getPlanById(10L); // Evaluates fallback method implicitly inside the catch block conceptually

        assertNotNull(result);
        assertEquals(new BigDecimal("299.00"), result.getPrice());
        verify(planRepository, times(1)).findActiveById(10L);
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
    }

    @Test
    void searchPlansFromRedis_Success() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:1")).thenReturn("[{plansJson}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of(planResponse));

        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(1L, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(10L, result.getContent().get(0).getId());
    }

    @Test
    void fallbackSearchPlans_DatabaseExecution() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        Page<Plan> dbPage = new PageImpl<>(List.of(plan));

        when(planRepository.searchActivePlans(1L, null, null, null, pageable)).thenReturn(dbPage);
        doNothing().when(operatorEventPublisher).publishPlanUpdatedEvent(1L);

        Page<PlanResponse> result = planQueryService.fallbackSearchPlans(1L, null, null, null, pageable, new RuntimeException("Test Miss"));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
    }

    @Test
    void searchPlansFromRedis_WithCategoryFilter() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        PlanResponse dataResponse = PlanResponse.builder()
                .id(10L)
                .price(new BigDecimal("299.00"))
                .operatorId(1L)
                .category(PlanCategory.DATA)
                .validityDays(28)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:1")).thenReturn("[{plansJson}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of(dataResponse));

        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(1L, PlanCategory.DATA, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(PlanCategory.DATA, result.getContent().get(0).getCategory());
    }

    @Test
    void searchPlansFromRedis_WithMinPriceFilter() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        PlanResponse expensiveResponse = PlanResponse.builder()
                .id(10L)
                .price(new BigDecimal("500.00"))
                .operatorId(1L)
                .category(PlanCategory.DATA)
                .validityDays(28)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:1")).thenReturn("[{plansJson}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of(expensiveResponse));

        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(1L, null, new BigDecimal("400.00"), null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).getPrice().compareTo(new BigDecimal("400.00")) >= 0);
    }

    @Test
    void searchPlansFromRedis_WithMaxPriceFilter() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        PlanResponse cheapResponse = PlanResponse.builder()
                .id(10L)
                .price(new BigDecimal("199.00"))
                .operatorId(1L)
                .category(PlanCategory.DATA)
                .validityDays(28)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:1")).thenReturn("[{plansJson}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of(cheapResponse));

        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(1L, null, null, new BigDecimal("300.00"), pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).getPrice().compareTo(new BigDecimal("300.00")) <= 0);
    }

    @Test
    void getPlanById_CacheMiss_FallbackToDatabase() {
        when(planRepository.findActiveById(10L)).thenReturn(Optional.of(plan));
        doNothing().when(operatorEventPublisher).publishPlanUpdatedEvent(1L);

        PlanResponse result = planQueryService.fallbackGetPlanById(10L, new RuntimeException("Cache miss"));

        assertNotNull(result);
        assertEquals(10L, result.getId());
        verify(planRepository, times(1)).findActiveById(10L);
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
    }

    @Test
    void searchPlansFromRedis_CacheMiss_TriggersCircuitBreaker() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:1")).thenReturn(null); // Cache miss
        
        Page<Plan> dbPage = new PageImpl<>(List.of(plan));
        when(planRepository.searchActivePlans(1L, null, null, null, pageable)).thenReturn(dbPage);
        doNothing().when(operatorEventPublisher).publishPlanUpdatedEvent(1L);

        // This will trigger the cache miss branch and call fallback
        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(1L, null, null, null, pageable);

        assertNotNull(result);
        verify(operatorEventPublisher, times(1)).publishPlanUpdatedEvent(1L);
    }

    @Test
    void searchPlansFromRedis_WithAllFiltersApplied() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        
        PlanResponse plan1 = PlanResponse.builder()
                .id(10L)
                .price(new BigDecimal("299.00"))
                .operatorId(1L)
                .category(PlanCategory.DATA)
                .validityDays(28)
                .build();
        
        PlanResponse plan2 = PlanResponse.builder()
                .id(11L)
                .price(new BigDecimal("499.00"))
                .operatorId(1L)
                .category(PlanCategory.UNLIMITED)
                .validityDays(30)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:1")).thenReturn("[{plansJson}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of(plan1, plan2));

        // Apply all filters: category, minPrice, maxPrice
        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(
            1L, PlanCategory.DATA, new BigDecimal("200.00"), new BigDecimal("400.00"), pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(PlanCategory.DATA, result.getContent().get(0).getCategory());
        assertTrue(result.getContent().get(0).getPrice().compareTo(new BigDecimal("200.00")) >= 0);
        assertTrue(result.getContent().get(0).getPrice().compareTo(new BigDecimal("400.00")) <= 0);
    }

    @Test
    void searchPlansFromRedis_SortByValidityDaysDescending() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("validityDays").descending());
        
        PlanResponse plan1 = PlanResponse.builder()
                .id(10L)
                .price(new BigDecimal("299.00"))
                .operatorId(1L)
                .category(PlanCategory.DATA)
                .validityDays(28)
                .build();
        
        PlanResponse plan2 = PlanResponse.builder()
                .id(11L)
                .price(new BigDecimal("499.00"))
                .operatorId(1L)
                .category(PlanCategory.DATA)
                .validityDays(84)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:1")).thenReturn("[{plansJson}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of(plan1, plan2));

        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(1L, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        // Verify descending order by validityDays
        assertTrue(result.getContent().get(0).getValidityDays() >= result.getContent().get(1).getValidityDays());
    }

    @Test
    void searchPlansFromRedis_PaginationBeyondResults() throws Exception {
        Pageable pageable = PageRequest.of(5, 10, Sort.by("price").ascending()); // Page 5, but only 1 result
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:1")).thenReturn("[{plansJson}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of(planResponse));

        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(1L, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size()); // No results on page 5
        assertEquals(1, result.getTotalElements()); // But total is still 1
    }

    @Test
    void searchPlansFromRedis_FilterExcludesAllResults() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        
        PlanResponse expensivePlan = PlanResponse.builder()
                .id(10L)
                .price(new BigDecimal("999.00"))
                .operatorId(1L)
                .category(PlanCategory.DATA)
                .validityDays(28)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:1")).thenReturn("[{plansJson}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of(expensivePlan));

        // Filter with maxPrice that excludes the expensive plan
        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(
            1L, null, null, new BigDecimal("500.00"), pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getContent().size());
    }

    @Test
    void getPlanById_RedisException_ThrowsRuntimeException() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plan:detail:10")).thenThrow(new RuntimeException("Redis connection failed"));

        // In unit tests, circuit breaker doesn't work, so exception is thrown
        assertThrows(RuntimeException.class, () -> planQueryService.getPlanById(10L));
        
        verify(redisTemplate, times(1)).opsForValue();
    }

    @Test
    void searchPlansFromRedis_RedisException_ThrowsRuntimeException() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:1")).thenThrow(new RuntimeException("Redis connection failed"));

        // In unit tests, circuit breaker doesn't work, so exception is thrown
        assertThrows(RuntimeException.class, () -> 
            planQueryService.searchPlansFromRedis(1L, null, null, null, pageable));
        
        verify(redisTemplate, times(1)).opsForValue();
    }

    @Test
    void searchPlansFromRedis_MinPriceFilter_ExcludesCheaperPlans() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        
        PlanResponse plan1 = PlanResponse.builder().id(1L).operatorId(1L).operatorName("Airtel")
                .planName("Plan 1").price(new BigDecimal("100")).validityDays(30).category(PlanCategory.DATA).build();
        PlanResponse plan2 = PlanResponse.builder().id(2L).operatorId(1L).operatorName("Airtel")
                .planName("Plan 2").price(new BigDecimal("200")).validityDays(30).category(PlanCategory.DATA).build();
        PlanResponse plan3 = PlanResponse.builder().id(3L).operatorId(1L).operatorName("Airtel")
                .planName("Plan 3").price(new BigDecimal("300")).validityDays(30).category(PlanCategory.DATA).build();
        
        List<PlanResponse> allPlans = List.of(plan1, plan2, plan3);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:1")).thenReturn("[{plansJson}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(allPlans);
        
        // Filter with minPrice=150 should exclude plan1 (price=100)
        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(1L, null, new BigDecimal("150"), null, pageable);
        
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2L, result.getContent().get(0).getId()); // plan2
        assertEquals(3L, result.getContent().get(1).getId()); // plan3
    }

    @Test
    void searchPlansFromRedis_DescendingSort_ReversesOrder() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").descending());
        
        PlanResponse plan1 = PlanResponse.builder().id(1L).operatorId(1L).operatorName("Airtel")
                .planName("Plan 1").price(new BigDecimal("100")).validityDays(30).category(PlanCategory.DATA).build();
        PlanResponse plan2 = PlanResponse.builder().id(2L).operatorId(1L).operatorName("Airtel")
                .planName("Plan 2").price(new BigDecimal("200")).validityDays(30).category(PlanCategory.DATA).build();
        PlanResponse plan3 = PlanResponse.builder().id(3L).operatorId(1L).operatorName("Airtel")
                .planName("Plan 3").price(new BigDecimal("300")).validityDays(30).category(PlanCategory.DATA).build();
        
        List<PlanResponse> allPlans = List.of(plan1, plan2, plan3);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:1")).thenReturn("[{plansJson}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(allPlans);
        
        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(1L, null, null, null, pageable);
        
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        // Descending order: highest price first
        assertEquals(3L, result.getContent().get(0).getId()); // plan3 (300)
        assertEquals(2L, result.getContent().get(1).getId()); // plan2 (200)
        assertEquals(1L, result.getContent().get(2).getId()); // plan1 (100)
    }

    @Test
    void searchPlansFromRedis_SortByValidityDaysDescending_WorksCorrectly() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("validityDays").descending());
        
        PlanResponse plan1 = PlanResponse.builder().id(1L).operatorId(1L).operatorName("Airtel")
                .planName("Plan 1").price(new BigDecimal("100")).validityDays(30).category(PlanCategory.DATA).build();
        PlanResponse plan2 = PlanResponse.builder().id(2L).operatorId(1L).operatorName("Airtel")
                .planName("Plan 2").price(new BigDecimal("200")).validityDays(60).category(PlanCategory.DATA).build();
        PlanResponse plan3 = PlanResponse.builder().id(3L).operatorId(1L).operatorName("Airtel")
                .planName("Plan 3").price(new BigDecimal("300")).validityDays(90).category(PlanCategory.DATA).build();
        
        List<PlanResponse> allPlans = List.of(plan1, plan2, plan3);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:1")).thenReturn("[{plansJson}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(allPlans);
        
        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(1L, null, null, null, pageable);
        
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        // Descending order by validityDays: longest first
        assertEquals(3L, result.getContent().get(0).getId()); // plan3 (90 days)
        assertEquals(2L, result.getContent().get(1).getId()); // plan2 (60 days)
        assertEquals(1L, result.getContent().get(2).getId()); // plan1 (30 days)
    }

    @Test
    void searchPlansFromRedis_UnsupportedSortProperty_MaintainsOriginalOrder() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("planName").ascending());
        
        PlanResponse plan1 = PlanResponse.builder().id(1L).operatorId(1L).operatorName("Airtel")
                .planName("Z Plan").price(new BigDecimal("100")).validityDays(30).category(PlanCategory.DATA).build();
        PlanResponse plan2 = PlanResponse.builder().id(2L).operatorId(1L).operatorName("Airtel")
                .planName("A Plan").price(new BigDecimal("200")).validityDays(30).category(PlanCategory.DATA).build();
        
        List<PlanResponse> allPlans = List.of(plan1, plan2);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plans:operator:1")).thenReturn("[{plansJson}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(allPlans);
        
        Page<PlanResponse> result = planQueryService.searchPlansFromRedis(1L, null, null, null, pageable);
        
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        // Unsupported sort property (planName) - cmp stays 0, maintains original order
        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals(2L, result.getContent().get(1).getId());
    }
}

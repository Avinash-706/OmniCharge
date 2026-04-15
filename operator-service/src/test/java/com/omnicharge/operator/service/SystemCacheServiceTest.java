package com.omnicharge.operator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.operator.dto.PlanResponse;
import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.entity.OperatorCategory;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.entity.PlanCategory;
import com.omnicharge.operator.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemCacheServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ApplicationReadyEvent applicationReadyEvent;

    @InjectMocks
    private SystemCacheService systemCacheService;

    private Operator operator;
    private Plan plan1;
    private Plan plan2;

    @BeforeEach
    void setUp() {
        operator = new Operator();
        operator.setId(1L);
        operator.setName("Airtel");
        operator.setCode("AIRTEL");
        operator.setCategory(OperatorCategory.PREPAID);
        operator.setIsActive(true);

        plan1 = new Plan();
        plan1.setId(10L);
        plan1.setOperator(operator);
        plan1.setPlanName("Unlimited 5G");
        plan1.setPrice(new BigDecimal("299.00"));
        plan1.setValidityDays(28);
        plan1.setCategory(PlanCategory.UNLIMITED);
        plan1.setIsActive(true);

        plan2 = new Plan();
        plan2.setId(11L);
        plan2.setOperator(operator);
        plan2.setPlanName("Data Pack");
        plan2.setPrice(new BigDecimal("199.00"));
        plan2.setValidityDays(30);
        plan2.setCategory(PlanCategory.DATA);
        plan2.setIsActive(true);
    }

    @Test
    void testHandleApplicationReady_ColdStart() throws JsonProcessingException {
        // Arrange
        when(redisTemplate.hasKey("system:cache:initialized")).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(planRepository.findAll()).thenReturn(List.of(plan1, plan2));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        systemCacheService.handleApplicationReady();

        // Assert
        verify(planRepository, times(1)).findAll();
        verify(valueOperations, times(1)).set(
                eq("system:cache:initialized"),
                eq("true"),
                eq(Duration.ofHours(24))
        );
        verify(valueOperations, atLeastOnce()).set(eq("plans:operator:1"), anyString());
    }

    @Test
    void testHandleApplicationReady_CacheAlreadyInitialized() {
        // Arrange
        when(redisTemplate.hasKey("system:cache:initialized")).thenReturn(true);

        // Act
        systemCacheService.handleApplicationReady();

        // Assert
        verify(planRepository, never()).findAll();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void testRebuildRedisCache_Success() throws JsonProcessingException {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(planRepository.findAll()).thenReturn(List.of(plan1, plan2));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        systemCacheService.rebuildRedisCache();

        // Assert
        verify(planRepository, times(1)).findAll();
        verify(valueOperations, times(1)).set(eq("plans:operator:1"), anyString());
        verify(valueOperations, times(1)).set(eq("plan:detail:10"), anyString());
        verify(valueOperations, times(1)).set(eq("plan:detail:11"), anyString());
    }

    @Test
    void testRebuildRedisCache_MultipleOperators() throws JsonProcessingException {
        // Arrange
        Operator operator2 = new Operator();
        operator2.setId(2L);
        operator2.setName("Jio");
        operator2.setCode("JIO");
        operator2.setIsActive(true);

        Plan plan3 = new Plan();
        plan3.setId(12L);
        plan3.setOperator(operator2);
        plan3.setPlanName("Jio Plan");
        plan3.setPrice(new BigDecimal("399.00"));
        plan3.setIsActive(true);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(planRepository.findAll()).thenReturn(List.of(plan1, plan2, plan3));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        systemCacheService.rebuildRedisCache();

        // Assert
        verify(valueOperations, times(1)).set(eq("plans:operator:1"), anyString());
        verify(valueOperations, times(1)).set(eq("plans:operator:2"), anyString());
        verify(valueOperations, times(1)).set(eq("plan:detail:10"), anyString());
        verify(valueOperations, times(1)).set(eq("plan:detail:11"), anyString());
        verify(valueOperations, times(1)).set(eq("plan:detail:12"), anyString());
    }

    @Test
    void testRebuildRedisCache_OnlyActivePlans() throws JsonProcessingException {
        // Arrange
        plan2.setIsActive(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(planRepository.findAll()).thenReturn(List.of(plan1, plan2));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        systemCacheService.rebuildRedisCache();

        // Assert
        verify(valueOperations, times(1)).set(eq("plans:operator:1"), anyString());
        verify(valueOperations, times(1)).set(eq("plan:detail:10"), anyString());
        verify(valueOperations, never()).set(eq("plan:detail:11"), anyString());
    }

    @Test
    void testRebuildRedisCache_EmptyDatabase() {
        // Arrange
        when(planRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        systemCacheService.rebuildRedisCache();

        // Assert
        verify(planRepository, times(1)).findAll();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void testRebuildRedisCache_JsonProcessingException() throws JsonProcessingException {
        // Arrange
        when(planRepository.findAll()).thenReturn(List.of(plan1));
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("Serialization error") {});

        // Act - should not throw exception
        systemCacheService.rebuildRedisCache();

        // Assert
        verify(planRepository, times(1)).findAll();
    }

    @Test
    void testEvictPlanCache_Success() {
        // Arrange
        when(redisTemplate.delete("plans:operator:1")).thenReturn(true);

        // Act
        systemCacheService.evictPlanCache(1L);

        // Assert
        verify(redisTemplate, times(1)).delete("plans:operator:1");
    }

    @Test
    void testEvictPlanCache_MultipleOperators() {
        // Arrange
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // Act
        systemCacheService.evictPlanCache(1L);
        systemCacheService.evictPlanCache(2L);
        systemCacheService.evictPlanCache(3L);

        // Assert
        verify(redisTemplate, times(1)).delete("plans:operator:1");
        verify(redisTemplate, times(1)).delete("plans:operator:2");
        verify(redisTemplate, times(1)).delete("plans:operator:3");
    }

    @Test
    void testEvictPlanCache_RedisException() {
        // Arrange
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis connection error"));

        // Act - should not throw exception
        systemCacheService.evictPlanCache(1L);

        // Assert
        verify(redisTemplate, times(1)).delete("plans:operator:1");
    }

    @Test
    void testEvictPlanDetailCache_Success() {
        // Arrange
        when(redisTemplate.delete("plan:detail:10")).thenReturn(true);

        // Act
        systemCacheService.evictPlanDetailCache(10L);

        // Assert
        verify(redisTemplate, times(1)).delete("plan:detail:10");
    }

    @Test
    void testEvictPlanDetailCache_MultiplePlans() {
        // Arrange
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // Act
        systemCacheService.evictPlanDetailCache(10L);
        systemCacheService.evictPlanDetailCache(11L);
        systemCacheService.evictPlanDetailCache(12L);

        // Assert
        verify(redisTemplate, times(1)).delete("plan:detail:10");
        verify(redisTemplate, times(1)).delete("plan:detail:11");
        verify(redisTemplate, times(1)).delete("plan:detail:12");
    }

    @Test
    void testEvictPlanDetailCache_RedisException() {
        // Arrange
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis connection error"));

        // Act - should not throw exception
        systemCacheService.evictPlanDetailCache(10L);

        // Assert
        verify(redisTemplate, times(1)).delete("plan:detail:10");
    }

    @Test
    void testRebuildRedisCache_VerifyCacheKeyFormat() throws JsonProcessingException {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(planRepository.findAll()).thenReturn(List.of(plan1));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        systemCacheService.rebuildRedisCache();

        // Assert
        verify(valueOperations, atLeast(2)).set(keyCaptor.capture(), anyString());
        List<String> capturedKeys = keyCaptor.getAllValues();
        assertThat(capturedKeys).contains("plans:operator:1");
        assertThat(capturedKeys).contains("plan:detail:10");
    }

    @Test
    void testRebuildRedisCache_VerifyPlanResponseMapping() throws JsonProcessingException {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(planRepository.findAll()).thenReturn(List.of(plan1));
        ArgumentCaptor<List<PlanResponse>> responseCaptor = ArgumentCaptor.forClass(List.class);
        when(objectMapper.writeValueAsString(responseCaptor.capture())).thenReturn("{}");

        // Act
        systemCacheService.rebuildRedisCache();

        // Assert
        List<PlanResponse> capturedResponses = responseCaptor.getValue();
        assertThat(capturedResponses).hasSize(1);
        PlanResponse response = capturedResponses.get(0);
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getOperatorId()).isEqualTo(1L);
        assertThat(response.getOperatorName()).isEqualTo("Airtel");
        assertThat(response.getPlanName()).isEqualTo("Unlimited 5G");
        assertThat(response.getPrice()).isEqualTo(new BigDecimal("299.00"));
    }
}

package com.omnicharge.operator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.operator.client.NumverifyClient;
import com.omnicharge.operator.dto.NumverifyResponse;
import com.omnicharge.operator.dto.OperatorDetectionResponse;
import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.repository.OperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperatorDetectionServiceTest {

    @Mock
    private NumverifyClient numverifyClient;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private IPlanService planService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private com.omnicharge.common.logging.LogEventPublisher logEventPublisher;

    @InjectMocks
    private OperatorDetectionService detectionService;

    private Operator operator;
    private NumverifyResponse numverifyResponse;

    @BeforeEach
    void setUp() {
        operator = new Operator();
        operator.setId(1L);
        operator.setName("Airtel");
        operator.setCode("AIRTEL");
        operator.setIsActive(true);

        numverifyResponse = new NumverifyResponse();
        numverifyResponse.setValid(true);
        numverifyResponse.setCarrier("Bharti Airtel Ltd");
    }

    @Test
    void detectOperator_CacheHit() throws Exception {
        String cacheKey = "operator:detect:9876543210";
        OperatorDetectionResponse cachedResponse = OperatorDetectionResponse.builder()
                .operatorCode("AIRTEL")
                .operatorName("Airtel")
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn("{\"operatorCode\":\"AIRTEL\"}");
        when(objectMapper.readValue("{\"operatorCode\":\"AIRTEL\"}", OperatorDetectionResponse.class))
                .thenReturn(cachedResponse);

        OperatorDetectionResponse result = detectionService.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals("AIRTEL", result.getOperatorCode());
        verify(numverifyClient, never()).detectOperator(anyString());
    }

    @Test
    void detectOperator_NumverifySuccess() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9876543210")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(List.of(operator));
        lenient().when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.of(operator));
        lenient().when(planService.getPlansByOperator(1L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals(1L, result.getOperatorId());
        assertEquals("Airtel", result.getOperatorName());
        verify(valueOperations, times(1)).set(eq("operator:detect:9876543210"), anyString(), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void detectOperator_FallbackRegex() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        // Simulate API Failure / No Carrier Match
        numverifyResponse.setCarrier(null);
        when(numverifyClient.detectOperator("9876543210")).thenReturn(numverifyResponse);
        
        // Target Regex (9876) matches AIRTEL fallback
        lenient().when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.of(operator));
        lenient().when(planService.getPlansByOperator(1L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals("Airtel", result.getOperatorName());
    }

    @Test
    void matchCarrierToOperator_Airtel() throws Exception {
        Operator airtelOperator = new Operator();
        airtelOperator.setId(1L);
        airtelOperator.setName("Airtel");
        airtelOperator.setCode("AIRTEL");
        airtelOperator.setIsActive(true);

        numverifyResponse.setCarrier("Bharti Airtel Ltd");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9876543210")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(List.of(airtelOperator));
        lenient().when(planService.getPlansByOperator(1L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals("Airtel", result.getOperatorName());
        assertEquals("AIRTEL", result.getOperatorCode());
    }

    @Test
    void matchCarrierToOperator_Jio() throws Exception {
        Operator jioOperator = new Operator();
        jioOperator.setId(2L);
        jioOperator.setName("Jio");
        jioOperator.setCode("JIO");
        jioOperator.setIsActive(true);

        numverifyResponse.setCarrier("Reliance Jio");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9999123456")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(List.of(jioOperator));
        lenient().when(planService.getPlansByOperator(2L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9999123456");

        assertNotNull(result);
        assertEquals("Jio", result.getOperatorName());
        assertEquals("JIO", result.getOperatorCode());
    }

    @Test
    void matchCarrierToOperator_Vi() throws Exception {
        Operator viOperator = new Operator();
        viOperator.setId(3L);
        viOperator.setName("Vi");
        viOperator.setCode("VI");
        viOperator.setIsActive(true);

        numverifyResponse.setCarrier("Vodafone Idea");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9898123456")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(List.of(viOperator));
        when(operatorRepository.findByCode("VI")).thenReturn(Optional.of(viOperator));
        lenient().when(planService.getPlansByOperator(3L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9898123456");

        assertNotNull(result);
        assertEquals("Vi", result.getOperatorName());
        assertEquals("VI", result.getOperatorCode());
    }

    @Test
    void matchCarrierToOperator_BSNL() throws Exception {
        Operator bsnlOperator = new Operator();
        bsnlOperator.setId(4L);
        bsnlOperator.setName("BSNL");
        bsnlOperator.setCode("BSNL");
        bsnlOperator.setIsActive(true);

        numverifyResponse.setCarrier("BSNL Mobile");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9400123456")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(List.of(bsnlOperator));
        lenient().when(planService.getPlansByOperator(4L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9400123456");

        assertNotNull(result);
        assertEquals("BSNL", result.getOperatorName());
        assertEquals("BSNL", result.getOperatorCode());
    }

    @Test
    void detectByPrefix_UnknownPrefix() throws Exception {
        numverifyResponse.setCarrier(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("5555123456")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(List.of(operator));
        lenient().when(planService.getPlansByOperator(1L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("5555123456");

        assertNotNull(result);
        assertEquals("Airtel", result.getOperatorName()); // Falls back to first active operator
    }

    @Test
    void detectByPrefix_JioPrefix() throws Exception {
        Operator jioOperator = new Operator();
        jioOperator.setId(2L);
        jioOperator.setName("Jio");
        jioOperator.setCode("JIO");
        jioOperator.setIsActive(true);

        numverifyResponse.setCarrier(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9999123456")).thenReturn(numverifyResponse);
        when(operatorRepository.findByCode("JIO")).thenReturn(Optional.of(jioOperator));
        lenient().when(planService.getPlansByOperator(2L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9999123456");

        assertNotNull(result);
        assertEquals("Jio", result.getOperatorName());
    }

    @Test
    void detectByPrefix_ViPrefix() throws Exception {
        Operator viOperator = new Operator();
        viOperator.setId(3L);
        viOperator.setName("Vi");
        viOperator.setCode("VI");
        viOperator.setIsActive(true);

        numverifyResponse.setCarrier(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9898123456")).thenReturn(numverifyResponse);
        when(operatorRepository.findByCode("VI")).thenReturn(Optional.of(viOperator));
        lenient().when(planService.getPlansByOperator(3L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9898123456");

        assertNotNull(result);
        assertEquals("Vi", result.getOperatorName());
    }

    @Test
    void detectOperator_RedisException_SkipsCache() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis connection failed"));
        when(numverifyClient.detectOperator("9876543210")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(List.of(operator));
        lenient().when(planService.getPlansByOperator(1L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals("Airtel", result.getOperatorName());
        verify(numverifyClient, times(1)).detectOperator("9876543210");
    }

    @Test
    void detectOperator_CacheDeserializationFails_FallsBackToAPI() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("{invalid json}");
        when(objectMapper.readValue(anyString(), eq(OperatorDetectionResponse.class)))
                .thenThrow(new JsonProcessingException("Invalid JSON") {});
        when(numverifyClient.detectOperator("9876543210")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(List.of(operator));
        lenient().when(planService.getPlansByOperator(1L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals("Airtel", result.getOperatorName());
    }

    @Test
    void detectOperator_NumverifyReturnsNull_FallsBackToPrefix() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9876543210")).thenReturn(null);
        when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.of(operator));
        lenient().when(planService.getPlansByOperator(1L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals("Airtel", result.getOperatorName());
    }

    @Test
    void detectOperator_NumverifyInvalidResponse_FallsBackToPrefix() throws Exception {
        numverifyResponse.setValid(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9876543210")).thenReturn(numverifyResponse);
        when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.of(operator));
        lenient().when(planService.getPlansByOperator(1L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals("Airtel", result.getOperatorName());
    }

    @Test
    void detectOperator_NoOperatorFound_ReturnsNull() throws Exception {
        numverifyResponse.setCarrier("Unknown Carrier");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("1234567890")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(Collections.emptyList());

        OperatorDetectionResponse result = detectionService.detectOperator("1234567890");

        assertNull(result);
        verify(logEventPublisher, times(1)).publish(any());
    }

    @Test
    void detectOperator_CacheSerializationFails_StillReturnsResult() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9876543210")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(List.of(operator));
        when(planService.getPlansByOperator(1L)).thenReturn(Collections.emptyList());
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization failed") {});

        OperatorDetectionResponse result = detectionService.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals("Airtel", result.getOperatorName());
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void detectOperator_RedisSetFails_StillReturnsResult() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9876543210")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(List.of(operator));
        when(planService.getPlansByOperator(1L)).thenReturn(Collections.emptyList());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        doThrow(new RuntimeException("Redis connection failed")).when(valueOperations)
                .set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        OperatorDetectionResponse result = detectionService.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals("Airtel", result.getOperatorName());
    }

    @Test
    void matchCarrierToOperator_SpecificMapping_Bharti() throws Exception {
        numverifyResponse.setCarrier("Bharti Telecom");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9876543210")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(Collections.emptyList());
        when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.of(operator));
        lenient().when(planService.getPlansByOperator(1L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals("Airtel", result.getOperatorName());
    }

    @Test
    void matchCarrierToOperator_SpecificMapping_Reliance() throws Exception {
        Operator jioOperator = new Operator();
        jioOperator.setId(2L);
        jioOperator.setName("Jio");
        jioOperator.setCode("JIO");

        numverifyResponse.setCarrier("Reliance Communications");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9999123456")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(Collections.emptyList());
        when(operatorRepository.findByCode("JIO")).thenReturn(Optional.of(jioOperator));
        lenient().when(planService.getPlansByOperator(2L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9999123456");

        assertNotNull(result);
        assertEquals("Jio", result.getOperatorName());
    }

    @Test
    void matchCarrierToOperator_SpecificMapping_Vodafone() throws Exception {
        Operator viOperator = new Operator();
        viOperator.setId(3L);
        viOperator.setName("Vi");
        viOperator.setCode("VI");

        numverifyResponse.setCarrier("Vodafone Mobile");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9898123456")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(Collections.emptyList());
        when(operatorRepository.findByCode("VI")).thenReturn(Optional.of(viOperator));
        lenient().when(planService.getPlansByOperator(3L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9898123456");

        assertNotNull(result);
        assertEquals("Vi", result.getOperatorName());
    }

    @Test
    void matchCarrierToOperator_SpecificMapping_Idea() throws Exception {
        Operator viOperator = new Operator();
        viOperator.setId(3L);
        viOperator.setName("Vi");
        viOperator.setCode("VI");

        numverifyResponse.setCarrier("Idea Cellular");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9898123456")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(Collections.emptyList());
        when(operatorRepository.findByCode("VI")).thenReturn(Optional.of(viOperator));
        lenient().when(planService.getPlansByOperator(3L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9898123456");

        assertNotNull(result);
        assertEquals("Vi", result.getOperatorName());
    }

    @Test
    void matchCarrierToOperator_OperatorNotFoundInRepository() throws Exception {
        numverifyResponse.setCarrier("Bharti Telecom");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9876543210")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(Collections.emptyList());
        when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.empty());
        // Falls back to prefix detection
        when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.empty());

        OperatorDetectionResponse result = detectionService.detectOperator("9876543210");

        assertNull(result);
    }

    @Test
    void matchCarrierToOperator_MultipleOperatorsNoMatch() throws Exception {
        Operator operator1 = new Operator();
        operator1.setId(1L);
        operator1.setName("Operator1");
        operator1.setCode("OP1");

        Operator operator2 = new Operator();
        operator2.setId(2L);
        operator2.setName("Operator2");
        operator2.setCode("OP2");

        numverifyResponse.setCarrier("Unknown Carrier XYZ");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9999999999")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(List.of(operator1, operator2));
        // No specific mapping matches, falls back to prefix
        when(operatorRepository.findByCode("JIO")).thenReturn(Optional.empty());

        OperatorDetectionResponse result = detectionService.detectOperator("9999999999");

        assertNull(result);
    }

    @Test
    void matchCarrierToOperator_OperatorNameContainsCarrier() throws Exception {
        Operator operator = new Operator();
        operator.setId(1L);
        operator.setName("Airtel India");
        operator.setCode("AIRTEL");
        operator.setIsActive(true);

        numverifyResponse.setCarrier("Air");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9876543210")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(List.of(operator));
        lenient().when(planService.getPlansByOperator(1L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals("Airtel India", result.getOperatorName());
    }

    @Test
    void matchCarrierToOperator_CarrierContainsOperatorCode() throws Exception {
        Operator operator = new Operator();
        operator.setId(1L);
        operator.setName("Airtel");
        operator.setCode("AIRTEL");
        operator.setIsActive(true);

        numverifyResponse.setCarrier("AIRTEL Network Services");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(numverifyClient.detectOperator("9876543210")).thenReturn(numverifyResponse);
        when(operatorRepository.findByIsActive(true)).thenReturn(List.of(operator));
        lenient().when(planService.getPlansByOperator(1L)).thenReturn(Collections.emptyList());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OperatorDetectionResponse result = detectionService.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals("Airtel", result.getOperatorName());
    }
}

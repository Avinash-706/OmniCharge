package com.omnicharge.operator.service;

import com.omnicharge.common.exception.DuplicateResourceException;
import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.operator.dto.OperatorRequest;
import com.omnicharge.operator.dto.OperatorResponse;
import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.entity.OperatorCategory;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.messaging.OperatorEventPublisher;
import com.omnicharge.operator.repository.OperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperatorServiceTest {

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private OperatorEventPublisher operatorEventPublisher;

    @Mock
    private SystemCacheService systemCacheService;

    @Mock
    private com.omnicharge.common.logging.LogEventPublisher logEventPublisher;

    @Mock
    private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private OperatorService operatorService;

    private Operator operator;
    private OperatorRequest operatorRequest;

    @BeforeEach
    void setUp() {
        operator = new Operator();
        operator.setId(1L);
        operator.setName("Airtel");
        operator.setCode("AIRTEL");
        operator.setCategory(OperatorCategory.PREPAID);
        operator.setIsActive(true);
        operator.setPlans(new ArrayList<>());

        operatorRequest = new OperatorRequest();
        operatorRequest.setName("Airtel");
        operatorRequest.setCode("AIRTEL");
        operatorRequest.setCategory(OperatorCategory.PREPAID);
    }

    @Test
    void testGetOperatorById_Success() {
        // Arrange
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));

        // Act
        OperatorResponse result = operatorService.getOperatorById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Airtel");
        assertThat(result.getCode()).isEqualTo("AIRTEL");
        verify(operatorRepository, times(1)).findById(1L);
    }

    @Test
    void testGetOperatorById_NotFound() {
        // Arrange
        when(operatorRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> operatorService.getOperatorById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Operator not found with id: 99");
    }

    @Test
    void testGetActiveOperatorById_Success() {
        // Arrange
        when(operatorRepository.findActiveById(1L)).thenReturn(Optional.of(operator));

        // Act
        OperatorResponse result = operatorService.getActiveOperatorById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    void testGetActiveOperatorById_InactiveOperator() {
        // Arrange
        when(operatorRepository.findActiveById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> operatorService.getActiveOperatorById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Operator not found with id: 1");
    }

    @Test
    void testGetOperatorsByCategory_Success() {
        // Arrange
        when(operatorRepository.findByCategory(OperatorCategory.PREPAID))
                .thenReturn(List.of(operator));

        // Act
        List<OperatorResponse> result = operatorService.getOperatorsByCategory(OperatorCategory.PREPAID);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo(OperatorCategory.PREPAID);
    }

    @Test
    void testGetAllOperators_Success() {
        // Arrange
        when(operatorRepository.findAll()).thenReturn(List.of(operator));

        // Act
        List<OperatorResponse> result = operatorService.getAllOperators();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Airtel");
    }

    @Test
    void testGetActiveOperators_Success() {
        // Arrange
        when(operatorRepository.findByIsActive(true)).thenReturn(List.of(operator));

        // Act
        List<OperatorResponse> result = operatorService.getActiveOperators();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive()).isTrue();
    }

    @Test
    void testGetOperatorsByStatus_Active() {
        // Arrange
        when(operatorRepository.findByIsActive(true)).thenReturn(List.of(operator));

        // Act
        List<OperatorResponse> result = operatorService.getOperatorsByStatus(true);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive()).isTrue();
    }

    @Test
    void testGetOperatorsByStatus_Inactive() {
        // Arrange
        operator.setIsActive(false);
        when(operatorRepository.findByIsActive(false)).thenReturn(List.of(operator));

        // Act
        List<OperatorResponse> result = operatorService.getOperatorsByStatus(false);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive()).isFalse();
    }

    @Test
    void testCreateOperator_Success() {
        // Arrange
        when(operatorRepository.existsByCode("AIRTEL")).thenReturn(false);
        when(operatorRepository.existsByName("Airtel")).thenReturn(false);
        when(operatorRepository.save(any(Operator.class))).thenReturn(operator);

        // Act
        OperatorResponse result = operatorService.createOperator(operatorRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Airtel");
        verify(operatorRepository, times(1)).save(any(Operator.class));
    }

    @Test
    void testCreateOperator_DuplicateCode() {
        // Arrange
        when(operatorRepository.existsByCode("AIRTEL")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> operatorService.createOperator(operatorRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Operator with code")
                .hasMessageContaining("AIRTEL");
    }

    @Test
    void testCreateOperator_DuplicateName() {
        // Arrange
        when(operatorRepository.existsByCode("AIRTEL")).thenReturn(false);
        when(operatorRepository.existsByName("Airtel")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> operatorService.createOperator(operatorRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Operator with name")
                .hasMessageContaining("Airtel");
    }

    @Test
    void testUpdateOperator_Success() {
        // Arrange
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.save(any(Operator.class))).thenReturn(operator);

        operatorRequest.setName("Airtel Updated");

        // Act
        OperatorResponse result = operatorService.updateOperator(1L, operatorRequest);

        // Assert
        assertThat(result).isNotNull();
        verify(operatorRepository, times(1)).save(operator);
    }

    @Test
    void testUpdateOperator_WithAllFieldsPopulated() {
        // Arrange
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.save(any(Operator.class))).thenReturn(operator);

        operatorRequest.setName("Airtel Premium");
        operatorRequest.setCode("AIRTEL_PREMIUM");
        operatorRequest.setCategory(OperatorCategory.POSTPAID);
        operatorRequest.setLogoUrl("https://example.com/logo.png");

        // Act
        OperatorResponse result = operatorService.updateOperator(1L, operatorRequest);

        // Assert
        assertThat(result).isNotNull();
        verify(operatorRepository, times(1)).save(operator);
    }

    @Test
    void testUpdateOperator_WithSomeFieldsNull() {
        // Arrange
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.save(any(Operator.class))).thenReturn(operator);

        operatorRequest.setName("Airtel Updated");
        operatorRequest.setCode("AIRTEL");
        operatorRequest.setCategory(OperatorCategory.PREPAID);
        operatorRequest.setLogoUrl(null); // Null field to test Optional.ofNullable().ifPresent() branch

        // Act
        OperatorResponse result = operatorService.updateOperator(1L, operatorRequest);

        // Assert
        assertThat(result).isNotNull();
        verify(operatorRepository, times(1)).save(operator);
    }

    @Test
    void testUpdateOperator_SameCodeDifferentOperator() {
        // Arrange
        Operator existingOperator = new Operator();
        existingOperator.setId(2L); // Different ID
        existingOperator.setCode("AIRTEL");

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.of(existingOperator));

        // Act & Assert
        assertThatThrownBy(() -> operatorService.updateOperator(1L, operatorRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Operator with code")
                .hasMessageContaining("AIRTEL");
    }

    @Test
    void testUpdateOperator_SameNameDifferentOperator() {
        // Arrange
        Operator existingOperator = new Operator();
        existingOperator.setId(2L); // Different ID
        existingOperator.setName("Airtel");

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.empty());
        when(operatorRepository.findByName("Airtel")).thenReturn(Optional.of(existingOperator));

        // Act & Assert
        assertThatThrownBy(() -> operatorService.updateOperator(1L, operatorRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Operator with name")
                .hasMessageContaining("Airtel");
    }

    @Test
    void testUpdateOperator_SameCodeSameOperator() {
        // Arrange - updating operator with its own code (should be allowed)
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.of(operator)); // Same operator
        when(operatorRepository.findByName("Airtel")).thenReturn(Optional.empty());
        when(operatorRepository.save(any(Operator.class))).thenReturn(operator);

        // Act
        OperatorResponse result = operatorService.updateOperator(1L, operatorRequest);

        // Assert
        assertThat(result).isNotNull();
        verify(operatorRepository, times(1)).save(operator);
    }

    @Test
    void testUpdateOperator_SameNameSameOperator() {
        // Arrange - updating operator with its own name (should be allowed)
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.empty());
        when(operatorRepository.findByName("Airtel")).thenReturn(Optional.of(operator)); // Same operator
        when(operatorRepository.save(any(Operator.class))).thenReturn(operator);

        // Act
        OperatorResponse result = operatorService.updateOperator(1L, operatorRequest);

        // Assert
        assertThat(result).isNotNull();
        verify(operatorRepository, times(1)).save(operator);
    }

    @Test
    void testUpdateOperator_NotFound() {
        // Arrange
        when(operatorRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> operatorService.updateOperator(99L, operatorRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testDeleteOperator_WithAssociatedPlans() {
        // Arrange
        Plan plan1 = new Plan();
        plan1.setId(10L);
        plan1.setIsActive(true);
        Plan plan2 = new Plan();
        plan2.setId(11L);
        plan2.setIsActive(true);
        operator.setPlans(List.of(plan1, plan2));

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.save(any(Operator.class))).thenReturn(operator);
        doNothing().when(systemCacheService).evictPlanCache(1L);

        // Act
        operatorService.deleteOperator(1L);

        // Assert
        assertThat(operator.getIsActive()).isFalse();
        assertThat(plan1.getIsActive()).isFalse();
        assertThat(plan2.getIsActive()).isFalse();
        assertThat(plan1.getDeactivatedByOperator()).isTrue();
        assertThat(plan2.getDeactivatedByOperator()).isTrue();
        verify(operatorRepository, times(1)).save(operator);
        verify(systemCacheService, times(1)).evictPlanCache(1L);
    }

    @Test
    void testDeleteOperator_WithNoPlans() {
        // Arrange
        operator.setPlans(new ArrayList<>()); // Empty plans list

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.save(any(Operator.class))).thenReturn(operator);
        doNothing().when(systemCacheService).evictPlanCache(1L);

        // Act
        operatorService.deleteOperator(1L);

        // Assert
        assertThat(operator.getIsActive()).isFalse();
        verify(operatorRepository, times(1)).save(operator);
        verify(systemCacheService, times(1)).evictPlanCache(1L);
    }

    @Test
    void testDeleteOperator_Success() {
        // Arrange
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.save(any(Operator.class))).thenReturn(operator);
        doNothing().when(systemCacheService).evictPlanCache(1L);

        // Act
        operatorService.deleteOperator(1L);

        // Assert
        assertThat(operator.getIsActive()).isFalse();
        verify(operatorRepository, times(1)).save(operator);
        verify(systemCacheService, times(1)).evictPlanCache(1L);
    }

    @Test
    void testMapToResponse_WithNullPlans() {
        // Arrange
        operator.setPlans(null); // Null plans to test mapToResponse branch
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));

        // Act
        OperatorResponse result = operatorService.getOperatorById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPlanCount()).isEqualTo(0);
    }

    @Test
    void testMapToResponse_WithEmptyPlans() {
        // Arrange
        operator.setPlans(new ArrayList<>()); // Empty plans list
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));

        // Act
        OperatorResponse result = operatorService.getOperatorById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPlanCount()).isEqualTo(0);
    }

    @Test
    void testActivateOperator_Success() {
        // Arrange
        operator.setIsActive(false);
        Plan plan1 = new Plan();
        plan1.setId(10L);
        plan1.setIsActive(false);
        plan1.setDeactivatedByOperator(true);  // This plan was deactivated by operator
        Plan plan2 = new Plan();
        plan2.setId(11L);
        plan2.setIsActive(false);
        plan2.setDeactivatedByOperator(true);  // This plan was deactivated by operator
        operator.setPlans(List.of(plan1, plan2));

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.save(any(Operator.class))).thenReturn(operator);
        doNothing().when(systemCacheService).evictPlanCache(1L);

        // Act
        OperatorResponse result = operatorService.activateOperator(1L);

        // Assert
        assertThat(result.getIsActive()).isTrue();
        assertThat(plan1.getIsActive()).isTrue();
        assertThat(plan2.getIsActive()).isTrue();
        verify(operatorRepository, times(1)).save(operator);
        verify(systemCacheService, times(1)).evictPlanCache(1L);
    }

    @Test
    void testActivateOperator_AlreadyActive() {
        // Arrange
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.save(any(Operator.class))).thenReturn(operator);
        doNothing().when(systemCacheService).evictPlanCache(1L);

        // Act
        OperatorResponse result = operatorService.activateOperator(1L);

        // Assert
        assertThat(result.getIsActive()).isTrue();
        verify(operatorRepository, times(1)).save(operator);
    }

    @Test
    void testDeactivateOperator_Success() {
        // Arrange
        Plan plan1 = new Plan();
        plan1.setId(10L);
        plan1.setIsActive(true);
        Plan plan2 = new Plan();
        plan2.setId(11L);
        plan2.setIsActive(true);
        operator.setPlans(List.of(plan1, plan2));

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.save(any(Operator.class))).thenReturn(operator);
        doNothing().when(systemCacheService).evictPlanCache(1L);

        // Act
        OperatorResponse result = operatorService.deactivateOperator(1L);

        // Assert
        assertThat(result.getIsActive()).isFalse();
        assertThat(plan1.getIsActive()).isFalse();
        assertThat(plan2.getIsActive()).isFalse();
        verify(operatorRepository, times(1)).save(operator);
        verify(systemCacheService, times(1)).evictPlanCache(1L);
    }

    @Test
    void testDeactivateOperator_AlreadyInactive() {
        // Arrange
        operator.setIsActive(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.save(any(Operator.class))).thenReturn(operator);
        doNothing().when(systemCacheService).evictPlanCache(1L);

        // Act
        OperatorResponse result = operatorService.deactivateOperator(1L);

        // Assert
        assertThat(result.getIsActive()).isFalse();
        verify(operatorRepository, times(1)).save(operator);
    }

    @Test
    void testActivateOperator_NotFound() {
        // Arrange
        when(operatorRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> operatorService.activateOperator(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testDeactivateOperator_NotFound() {
        // Arrange
        when(operatorRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> operatorService.deactivateOperator(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

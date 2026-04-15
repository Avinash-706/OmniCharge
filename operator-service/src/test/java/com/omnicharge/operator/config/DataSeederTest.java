package com.omnicharge.operator.config;

import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.entity.OperatorCategory;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.repository.OperatorRepository;
import com.omnicharge.operator.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Captor
    private ArgumentCaptor<List<Operator>> operatorCaptor;

    @Captor
    private ArgumentCaptor<List<Plan>> planCaptor;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(operatorRepository, planRepository);
    }

    @Test
    void run_ShouldSeedOperatorsAndPlans_WhenDatabaseIsEmpty() {
        // Arrange
        when(operatorRepository.count()).thenReturn(0L);
        when(planRepository.count()).thenReturn(0L);
        
        Operator airtel = createMockOperator(1L, "Airtel", "AIRTEL");
        Operator jio = createMockOperator(2L, "Jio", "JIO");
        Operator vi = createMockOperator(3L, "Vi", "VI");
        
        when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.of(airtel));
        when(operatorRepository.findByCode("JIO")).thenReturn(Optional.of(jio));
        when(operatorRepository.findByCode("VI")).thenReturn(Optional.of(vi));

        // Act
        dataSeeder.run();

        // Assert
        verify(operatorRepository).saveAll(operatorCaptor.capture());
        List<Operator> savedOperators = operatorCaptor.getValue();
        assertThat(savedOperators).hasSize(4);
        assertThat(savedOperators).extracting(Operator::getName)
            .containsExactly("Airtel", "Jio", "Vi", "BSNL");
        assertThat(savedOperators).extracting(Operator::getCode)
            .containsExactly("AIRTEL", "JIO", "VI", "BSNL");
        assertThat(savedOperators).allMatch(Operator::getIsActive);

        verify(planRepository, times(3)).saveAll(anyList());
    }

    @Test
    void run_ShouldSkipOperatorSeeding_WhenOperatorsAlreadyExist() {
        // Arrange
        when(operatorRepository.count()).thenReturn(4L);

        // Act
        dataSeeder.run();

        // Assert
        verify(operatorRepository, never()).saveAll(anyList());
    }

    @Test
    void run_ShouldSkipPlanSeeding_WhenPlansAlreadyExist() {
        // Arrange
        when(operatorRepository.count()).thenReturn(0L);
        when(planRepository.count()).thenReturn(10L);

        // Act
        dataSeeder.run();

        // Assert
        verify(operatorRepository).saveAll(anyList());
        verify(planRepository, never()).saveAll(anyList());
    }

    @Test
    void run_ShouldSeedAirtelPlans_WhenAirtelExists() {
        // Arrange
        when(operatorRepository.count()).thenReturn(0L);
        when(planRepository.count()).thenReturn(0L);
        
        Operator airtel = createMockOperator(1L, "Airtel", "AIRTEL");
        when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.of(airtel));
        when(operatorRepository.findByCode("JIO")).thenReturn(Optional.empty());
        when(operatorRepository.findByCode("VI")).thenReturn(Optional.empty());

        // Act
        dataSeeder.run();

        // Assert
        verify(planRepository, times(1)).saveAll(planCaptor.capture());
        List<Plan> savedPlans = planCaptor.getValue();
        assertThat(savedPlans).hasSize(3);
        assertThat(savedPlans).allMatch(plan -> plan.getOperator().equals(airtel));
        assertThat(savedPlans).extracting(Plan::getPlanName)
            .contains("Unlimited 84 Days", "Data Booster", "Talktime Special");
    }

    @Test
    void run_ShouldSeedJioPlans_WhenJioExists() {
        // Arrange
        when(operatorRepository.count()).thenReturn(0L);
        when(planRepository.count()).thenReturn(0L);
        
        Operator jio = createMockOperator(2L, "Jio", "JIO");
        when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.empty());
        when(operatorRepository.findByCode("JIO")).thenReturn(Optional.of(jio));
        when(operatorRepository.findByCode("VI")).thenReturn(Optional.empty());

        // Act
        dataSeeder.run();

        // Assert
        verify(planRepository, times(1)).saveAll(planCaptor.capture());
        List<Plan> savedPlans = planCaptor.getValue();
        assertThat(savedPlans).hasSize(2);
        assertThat(savedPlans).allMatch(plan -> plan.getOperator().equals(jio));
        assertThat(savedPlans).extracting(Plan::getPlanName)
            .contains("Jio Unlimited", "Data Pack");
    }

    @Test
    void run_ShouldSeedViPlans_WhenViExists() {
        // Arrange
        when(operatorRepository.count()).thenReturn(0L);
        when(planRepository.count()).thenReturn(0L);
        
        Operator vi = createMockOperator(3L, "Vi", "VI");
        when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.empty());
        when(operatorRepository.findByCode("JIO")).thenReturn(Optional.empty());
        when(operatorRepository.findByCode("VI")).thenReturn(Optional.of(vi));

        // Act
        dataSeeder.run();

        // Assert
        verify(planRepository, times(1)).saveAll(planCaptor.capture());
        List<Plan> savedPlans = planCaptor.getValue();
        assertThat(savedPlans).hasSize(2);
        assertThat(savedPlans).allMatch(plan -> plan.getOperator().equals(vi));
        assertThat(savedPlans).extracting(Plan::getPlanName)
            .contains("Vi Hero Unlimited", "Weekend Data");
    }

    @Test
    void run_ShouldSetCorrectOperatorProperties() {
        // Arrange
        when(operatorRepository.count()).thenReturn(0L);
        when(planRepository.count()).thenReturn(10L);

        // Act
        dataSeeder.run();

        // Assert
        verify(operatorRepository).saveAll(operatorCaptor.capture());
        List<Operator> savedOperators = operatorCaptor.getValue();
        
        Operator airtel = savedOperators.stream()
            .filter(op -> op.getCode().equals("AIRTEL"))
            .findFirst()
            .orElseThrow();
        
        assertThat(airtel.getName()).isEqualTo("Airtel");
        assertThat(airtel.getCategory()).isEqualTo(OperatorCategory.PREPAID);
        assertThat(airtel.getLogoUrl()).isEqualTo("https://example.com/airtel-logo.png");
        assertThat(airtel.getIsActive()).isTrue();
    }

    @Test
    void run_ShouldSetCorrectPlanProperties() {
        // Arrange
        when(operatorRepository.count()).thenReturn(0L);
        when(planRepository.count()).thenReturn(0L);
        
        Operator airtel = createMockOperator(1L, "Airtel", "AIRTEL");
        when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.of(airtel));
        when(operatorRepository.findByCode("JIO")).thenReturn(Optional.empty());
        when(operatorRepository.findByCode("VI")).thenReturn(Optional.empty());

        // Act
        dataSeeder.run();

        // Assert
        verify(planRepository).saveAll(planCaptor.capture());
        List<Plan> savedPlans = planCaptor.getValue();
        
        Plan unlimitedPlan = savedPlans.stream()
            .filter(plan -> plan.getPlanName().equals("Unlimited 84 Days"))
            .findFirst()
            .orElseThrow();
        
        assertThat(unlimitedPlan.getPrice()).isEqualByComparingTo("719");
        assertThat(unlimitedPlan.getValidityDays()).isEqualTo(84);
        assertThat(unlimitedPlan.getDataLimit()).isEqualTo("2GB/day");
        assertThat(unlimitedPlan.getCallBenefit()).isEqualTo("Unlimited");
        assertThat(unlimitedPlan.getSmsBenefit()).isEqualTo("100 SMS/day");
        assertThat(unlimitedPlan.getAdditionalBenefits()).isEqualTo("Free Hellotunes");
        assertThat(unlimitedPlan.getIsActive()).isTrue();
        assertThat(unlimitedPlan.getDeactivatedByOperator()).isFalse();
    }

    @Test
    void run_ShouldHandleNullAdditionalBenefits() {
        // Arrange
        when(operatorRepository.count()).thenReturn(0L);
        when(planRepository.count()).thenReturn(0L);
        
        Operator airtel = createMockOperator(1L, "Airtel", "AIRTEL");
        when(operatorRepository.findByCode("AIRTEL")).thenReturn(Optional.of(airtel));
        when(operatorRepository.findByCode("JIO")).thenReturn(Optional.empty());
        when(operatorRepository.findByCode("VI")).thenReturn(Optional.empty());

        // Act
        dataSeeder.run();

        // Assert
        verify(planRepository).saveAll(planCaptor.capture());
        List<Plan> savedPlans = planCaptor.getValue();
        
        Plan talktimePlan = savedPlans.stream()
            .filter(plan -> plan.getPlanName().equals("Talktime Special"))
            .findFirst()
            .orElseThrow();
        
        assertThat(talktimePlan.getAdditionalBenefits()).isNull();
    }

    private Operator createMockOperator(Long id, String name, String code) {
        Operator operator = new Operator();
        operator.setId(id);
        operator.setName(name);
        operator.setCode(code);
        operator.setCategory(OperatorCategory.PREPAID);
        operator.setIsActive(true);
        return operator;
    }
}

package com.omnicharge.operator.repository;

import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.entity.OperatorCategory;
import com.omnicharge.operator.entity.Plan;
import com.omnicharge.operator.entity.PlanCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class PlanRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PlanRepository planRepository;

    private Operator activeOperator;
    private Operator inactiveOperator;
    private Plan activePlan1;
    private Plan activePlan2;
    private Plan inactivePlan;

    @BeforeEach
    void setUp() {
        // Create active operator
        activeOperator = new Operator();
        activeOperator.setCode("AIRTEL");
        activeOperator.setName("Airtel");
        activeOperator.setCategory(OperatorCategory.PREPAID);
        activeOperator.setIsActive(true);
        entityManager.persist(activeOperator);

        // Create inactive operator
        inactiveOperator = new Operator();
        inactiveOperator.setCode("VODAFONE");
        inactiveOperator.setName("Vodafone");
        inactiveOperator.setCategory(OperatorCategory.POSTPAID);
        inactiveOperator.setIsActive(false);
        entityManager.persist(inactiveOperator);

        // Create active plan 1 (DATA)
        activePlan1 = new Plan();
        activePlan1.setPlanName("1GB Data");
        activePlan1.setPrice(new BigDecimal("199.00"));
        activePlan1.setValidityDays(28);
        activePlan1.setDataLimit("1GB");
        activePlan1.setCategory(PlanCategory.DATA);
        activePlan1.setIsActive(true);
        activePlan1.setOperator(activeOperator);
        entityManager.persist(activePlan1);

        // Create active plan 2 (UNLIMITED)
        activePlan2 = new Plan();
        activePlan2.setPlanName("Unlimited Plan");
        activePlan2.setPrice(new BigDecimal("299.00"));
        activePlan2.setValidityDays(30);
        activePlan2.setCallBenefit("Unlimited");
        activePlan2.setCategory(PlanCategory.UNLIMITED);
        activePlan2.setIsActive(true);
        activePlan2.setOperator(activeOperator);
        entityManager.persist(activePlan2);

        // Create inactive plan
        inactivePlan = new Plan();
        inactivePlan.setPlanName("Old Plan");
        inactivePlan.setPrice(new BigDecimal("99.00"));
        inactivePlan.setValidityDays(7);
        inactivePlan.setDataLimit("500MB");
        inactivePlan.setCategory(PlanCategory.DATA);
        inactivePlan.setIsActive(false);
        inactivePlan.setOperator(activeOperator);
        entityManager.persist(inactivePlan);

        // Create plan for inactive operator
        Plan planForInactiveOperator = new Plan();
        planForInactiveOperator.setPlanName("Vodafone Plan");
        planForInactiveOperator.setPrice(new BigDecimal("149.00"));
        planForInactiveOperator.setValidityDays(14);
        planForInactiveOperator.setDataLimit("2GB");
        planForInactiveOperator.setCategory(PlanCategory.DATA);
        planForInactiveOperator.setIsActive(true);
        planForInactiveOperator.setOperator(inactiveOperator);
        entityManager.persist(planForInactiveOperator);

        entityManager.flush();
    }

    @Test
    void testFindByOperatorIdAndIsActive_ShouldReturnActivePlans() {
        // When
        List<Plan> activePlans = planRepository.findByOperatorIdAndIsActive(activeOperator.getId(), true);

        // Then
        assertThat(activePlans).hasSize(2);
        assertThat(activePlans).extracting(Plan::getPlanName)
                .containsExactlyInAnyOrder("1GB Data", "Unlimited Plan");
    }

    @Test
    void testFindByOperatorIdAndIsActive_ShouldReturnInactivePlans() {
        // When
        List<Plan> inactivePlans = planRepository.findByOperatorIdAndIsActive(activeOperator.getId(), false);

        // Then
        assertThat(inactivePlans).hasSize(1);
        assertThat(inactivePlans.get(0).getPlanName()).isEqualTo("Old Plan");
    }

    @Test
    void testFindByOperatorId_ShouldReturnAllPlans() {
        // When
        List<Plan> allPlans = planRepository.findByOperatorId(activeOperator.getId());

        // Then
        assertThat(allPlans).hasSize(3);
    }

    @Test
    void testFindByOperatorIdWithPageable_ShouldReturnPagedPlans() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<Plan> planPage = planRepository.findByOperatorId(activeOperator.getId(), pageable);

        // Then
        assertThat(planPage.getContent()).hasSize(2);
        assertThat(planPage.getTotalElements()).isEqualTo(3);
        assertThat(planPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testFindActiveById_WhenPlanAndOperatorAreActive_ShouldReturnPlan() {
        // When
        Optional<Plan> result = planRepository.findActiveById(activePlan1.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getPlanName()).isEqualTo("1GB Data");
    }

    @Test
    void testFindActiveById_WhenPlanIsInactive_ShouldReturnEmpty() {
        // When
        Optional<Plan> result = planRepository.findActiveById(inactivePlan.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindActiveById_WhenOperatorIsInactive_ShouldReturnEmpty() {
        // Given
        Plan planForInactiveOp = planRepository.findByOperatorId(inactiveOperator.getId()).get(0);

        // When
        Optional<Plan> result = planRepository.findActiveById(planForInactiveOp.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testSearchActivePlans_WithAllFilters_ShouldReturnFilteredPlans() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Plan> result = planRepository.searchActivePlans(
                activeOperator.getId(),
                PlanCategory.DATA,
                new BigDecimal("100.00"),
                new BigDecimal("200.00"),
                pageable
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPlanName()).isEqualTo("1GB Data");
    }

    @Test
    void testSearchActivePlans_WithNullCategory_ShouldReturnAllCategories() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Plan> result = planRepository.searchActivePlans(
                activeOperator.getId(),
                null,
                null,
                null,
                pageable
        );

        // Then
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void testSearchActivePlans_WithMinPrice_ShouldFilterByMinPrice() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Plan> result = planRepository.searchActivePlans(
                activeOperator.getId(),
                null,
                new BigDecimal("250.00"),
                null,
                pageable
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPlanName()).isEqualTo("Unlimited Plan");
    }

    @Test
    void testSearchActivePlans_WithMaxPrice_ShouldFilterByMaxPrice() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Plan> result = planRepository.searchActivePlans(
                activeOperator.getId(),
                null,
                null,
                new BigDecimal("200.00"),
                pageable
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPlanName()).isEqualTo("1GB Data");
    }

    @Test
    void testFindByOperatorIdAndStatus_WithActiveStatus_ShouldReturnActivePlans() {
        // When
        List<Plan> activePlans = planRepository.findByOperatorIdAndStatus(activeOperator.getId(), true);

        // Then
        assertThat(activePlans).hasSize(2);
    }

    @Test
    void testFindByOperatorIdAndStatus_WithInactiveStatus_ShouldReturnInactivePlans() {
        // When
        List<Plan> inactivePlans = planRepository.findByOperatorIdAndStatus(activeOperator.getId(), false);

        // Then
        assertThat(inactivePlans).hasSize(1);
        assertThat(inactivePlans.get(0).getPlanName()).isEqualTo("Old Plan");
    }

    @Test
    void testFindByOperatorIdAndStatus_WithNullStatus_ShouldReturnAllPlans() {
        // When
        List<Plan> allPlans = planRepository.findByOperatorIdAndStatus(activeOperator.getId(), null);

        // Then
        assertThat(allPlans).hasSize(3);
    }

    @Test
    void testSearchPlansWithStatus_WithAllFilters_ShouldReturnFilteredPlans() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Plan> result = planRepository.searchPlansWithStatus(
                activeOperator.getId(),
                PlanCategory.DATA,
                true,
                pageable
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPlanName()).isEqualTo("1GB Data");
    }

    @Test
    void testSearchPlansWithStatus_WithNullOperatorId_ShouldReturnAllOperators() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Plan> result = planRepository.searchPlansWithStatus(
                null,
                null,
                true,
                pageable
        );

        // Then
        assertThat(result.getContent()).hasSize(3); // 2 from active operator + 1 from inactive operator
    }

    @Test
    void testSearchPlansWithStatus_WithNullCategory_ShouldReturnAllCategories() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Plan> result = planRepository.searchPlansWithStatus(
                activeOperator.getId(),
                null,
                true,
                pageable
        );

        // Then
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void testSearchPlansWithStatus_WithNullStatus_ShouldReturnAllStatuses() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Plan> result = planRepository.searchPlansWithStatus(
                activeOperator.getId(),
                null,
                null,
                pageable
        );

        // Then
        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    void testCountByIsActive_ShouldReturnActiveCount() {
        // When
        long activeCount = planRepository.countByIsActive(true);

        // Then
        assertThat(activeCount).isEqualTo(3); // 2 from active operator + 1 from inactive operator
    }

    @Test
    void testCountByIsActive_ShouldReturnInactiveCount() {
        // When
        long inactiveCount = planRepository.countByIsActive(false);

        // Then
        assertThat(inactiveCount).isEqualTo(1);
    }

    @Test
    void testCountActivePlansByCategory_ShouldReturnCategoryCounts() {
        // When
        List<Object[]> categoryCounts = planRepository.countActivePlansByCategory();

        // Then
        assertThat(categoryCounts).hasSize(2);
        
        // Verify DATA category count
        Object[] dataCount = categoryCounts.stream()
                .filter(arr -> arr[0] == PlanCategory.DATA)
                .findFirst()
                .orElseThrow();
        assertThat(dataCount[1]).isEqualTo(2L); // 1 from active operator + 1 from inactive operator

        // Verify UNLIMITED category count
        Object[] unlimitedCount = categoryCounts.stream()
                .filter(arr -> arr[0] == PlanCategory.UNLIMITED)
                .findFirst()
                .orElseThrow();
        assertThat(unlimitedCount[1]).isEqualTo(1L);
    }
}

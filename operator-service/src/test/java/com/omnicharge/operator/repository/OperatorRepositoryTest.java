package com.omnicharge.operator.repository;

import com.omnicharge.operator.entity.Operator;
import com.omnicharge.operator.entity.OperatorCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class OperatorRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OperatorRepository operatorRepository;

    private Operator activeOperator;
    private Operator inactiveOperator;

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

        entityManager.flush();
    }

    @Test
    void testFindByCode_WhenOperatorExists_ShouldReturnOperator() {
        // When
        Optional<Operator> result = operatorRepository.findByCode("AIRTEL");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Airtel");
        assertThat(result.get().getCategory()).isEqualTo(OperatorCategory.PREPAID);
    }

    @Test
    void testFindByCode_WhenOperatorDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<Operator> result = operatorRepository.findByCode("NONEXISTENT");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindByName_WhenOperatorExists_ShouldReturnOperator() {
        // When
        Optional<Operator> result = operatorRepository.findByName("Airtel");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("AIRTEL");
    }

    @Test
    void testFindByName_WhenOperatorDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<Operator> result = operatorRepository.findByName("NonExistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindByCategory_ShouldReturnOperatorsOfCategory() {
        // When
        List<Operator> prepaidOperators = operatorRepository.findByCategory(OperatorCategory.PREPAID);
        List<Operator> postpaidOperators = operatorRepository.findByCategory(OperatorCategory.POSTPAID);

        // Then
        assertThat(prepaidOperators).hasSize(1);
        assertThat(prepaidOperators.get(0).getCode()).isEqualTo("AIRTEL");
        
        assertThat(postpaidOperators).hasSize(1);
        assertThat(postpaidOperators.get(0).getCode()).isEqualTo("VODAFONE");
    }

    @Test
    void testFindByCategory_WhenNoCategoryMatch_ShouldReturnEmptyList() {
        // When
        List<Operator> dthOperators = operatorRepository.findByCategory(OperatorCategory.DTH);

        // Then
        assertThat(dthOperators).isEmpty();
    }

    @Test
    void testFindByIsActive_WhenActiveTrue_ShouldReturnActiveOperators() {
        // When
        List<Operator> activeOperators = operatorRepository.findByIsActive(true);

        // Then
        assertThat(activeOperators).hasSize(1);
        assertThat(activeOperators.get(0).getCode()).isEqualTo("AIRTEL");
        assertThat(activeOperators.get(0).getIsActive()).isTrue();
    }

    @Test
    void testFindByIsActive_WhenActiveFalse_ShouldReturnInactiveOperators() {
        // When
        List<Operator> inactiveOperators = operatorRepository.findByIsActive(false);

        // Then
        assertThat(inactiveOperators).hasSize(1);
        assertThat(inactiveOperators.get(0).getCode()).isEqualTo("VODAFONE");
        assertThat(inactiveOperators.get(0).getIsActive()).isFalse();
    }

    @Test
    void testExistsByCode_WhenCodeExists_ShouldReturnTrue() {
        // When
        boolean exists = operatorRepository.existsByCode("AIRTEL");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByCode_WhenCodeDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = operatorRepository.existsByCode("NONEXISTENT");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testExistsByName_WhenNameExists_ShouldReturnTrue() {
        // When
        boolean exists = operatorRepository.existsByName("Airtel");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByName_WhenNameDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = operatorRepository.existsByName("NonExistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testFindActiveById_WhenOperatorIsActive_ShouldReturnOperator() {
        // When
        Optional<Operator> result = operatorRepository.findActiveById(activeOperator.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("AIRTEL");
        assertThat(result.get().getIsActive()).isTrue();
    }

    @Test
    void testFindActiveById_WhenOperatorIsInactive_ShouldReturnEmpty() {
        // When
        Optional<Operator> result = operatorRepository.findActiveById(inactiveOperator.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindActiveById_WhenOperatorDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<Operator> result = operatorRepository.findActiveById(999L);

        // Then
        assertThat(result).isEmpty();
    }
}

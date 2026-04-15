package com.omnicharge.recharge.repository;

import com.omnicharge.recharge.entity.Recharge;
import com.omnicharge.recharge.entity.RechargeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RechargeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RechargeRepository rechargeRepository;

    private Recharge testRecharge;

    @BeforeEach
    void setUp() {
        testRecharge = new Recharge();
        testRecharge.setRechargeId("OMNI-TEST123");
        testRecharge.setUserId(100L);
        testRecharge.setMobileNumber("9876543210");
        testRecharge.setOperatorId(1L);
        testRecharge.setOperatorName("Airtel");
        testRecharge.setPlanId(10L);
        testRecharge.setPlanName("Unlimited 84 Days");
        testRecharge.setAmount(new BigDecimal("599.00"));
        testRecharge.setPlanValidityDays(84);
        testRecharge.setPlanExpiryDate(LocalDate.now().plusDays(84));
        testRecharge.setStatus(RechargeStatus.SUCCESS);
        testRecharge.setTransactionId("TXN123");
    }

    @Test
    void testFindByRechargeId() {
        // Given
        entityManager.persist(testRecharge);
        entityManager.flush();

        // When
        Optional<Recharge> found = rechargeRepository.findByRechargeId("OMNI-TEST123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getRechargeId()).isEqualTo("OMNI-TEST123");
        assertThat(found.get().getUserId()).isEqualTo(100L);
    }

    @Test
    void testFindByRechargeId_NotFound() {
        // When
        Optional<Recharge> found = rechargeRepository.findByRechargeId("NONEXISTENT");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void testFindByUserId() {
        // Given
        entityManager.persist(testRecharge);
        
        Recharge anotherRecharge = new Recharge();
        anotherRecharge.setRechargeId("OMNI-TEST456");
        anotherRecharge.setUserId(100L);
        anotherRecharge.setMobileNumber("9123456789");
        anotherRecharge.setOperatorId(2L);
        anotherRecharge.setOperatorName("Jio");
        anotherRecharge.setPlanId(20L);
        anotherRecharge.setPlanName("Data Booster");
        anotherRecharge.setAmount(new BigDecimal("199.00"));
        anotherRecharge.setPlanValidityDays(28);
        anotherRecharge.setPlanExpiryDate(LocalDate.now().plusDays(28));
        anotherRecharge.setStatus(RechargeStatus.SUCCESS);
        entityManager.persist(anotherRecharge);
        entityManager.flush();

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Recharge> found = rechargeRepository.findByUserId(100L, pageable);

        // Then
        assertThat(found.getContent()).hasSize(2);
        assertThat(found.getContent()).extracting(Recharge::getUserId).containsOnly(100L);
    }

    @Test
    void testCountByStatus() {
        // Given
        entityManager.persist(testRecharge);
        
        Recharge failedRecharge = new Recharge();
        failedRecharge.setRechargeId("OMNI-FAILED");
        failedRecharge.setUserId(200L);
        failedRecharge.setMobileNumber("9111111111");
        failedRecharge.setOperatorId(1L);
        failedRecharge.setOperatorName("Airtel");
        failedRecharge.setPlanId(10L);
        failedRecharge.setPlanName("Plan");
        failedRecharge.setAmount(new BigDecimal("100.00"));
        failedRecharge.setPlanValidityDays(28);
        failedRecharge.setPlanExpiryDate(LocalDate.now().plusDays(28));
        failedRecharge.setStatus(RechargeStatus.FAILED);
        entityManager.persist(failedRecharge);
        entityManager.flush();

        // When
        long successCount = rechargeRepository.countByStatus(RechargeStatus.SUCCESS);
        long failedCount = rechargeRepository.countByStatus(RechargeStatus.FAILED);

        // Then
        assertThat(successCount).isEqualTo(1);
        assertThat(failedCount).isEqualTo(1);
    }

    @Test
    void testFindByStatusAndPlanExpiryDate() {
        // Given
        LocalDate expiryDate = LocalDate.now().plusDays(5);
        testRecharge.setPlanExpiryDate(expiryDate);
        entityManager.persist(testRecharge);
        entityManager.flush();

        // When
        List<Recharge> found = rechargeRepository.findByStatusAndPlanExpiryDate(
                RechargeStatus.SUCCESS, expiryDate);

        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getPlanExpiryDate()).isEqualTo(expiryDate);
    }

    @Test
    void testFindByStatusAndPlanExpiryDateBefore() {
        // Given
        LocalDate pastDate = LocalDate.now().minusDays(5);
        testRecharge.setPlanExpiryDate(pastDate);
        entityManager.persist(testRecharge);
        entityManager.flush();

        // When
        List<Recharge> found = rechargeRepository.findByStatusAndPlanExpiryDateBefore(
                RechargeStatus.SUCCESS, LocalDate.now());

        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getPlanExpiryDate()).isBefore(LocalDate.now());
    }

    @Test
    void testCountActiveRecharges() {
        // Given
        testRecharge.setPlanExpiryDate(LocalDate.now().plusDays(10));
        entityManager.persist(testRecharge);
        entityManager.flush();

        // When
        Long count = rechargeRepository.countActiveRecharges(RechargeStatus.SUCCESS, LocalDate.now());

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testCountExpiredRecharges() {
        // Given
        testRecharge.setPlanExpiryDate(LocalDate.now().minusDays(10));
        entityManager.persist(testRecharge);
        entityManager.flush();

        // When
        Long count = rechargeRepository.countExpiredRecharges(RechargeStatus.SUCCESS, LocalDate.now());

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testFindAllByPlanId() {
        // Given
        entityManager.persist(testRecharge);
        
        Recharge anotherRecharge = new Recharge();
        anotherRecharge.setRechargeId("OMNI-TEST789");
        anotherRecharge.setUserId(200L);
        anotherRecharge.setMobileNumber("9222222222");
        anotherRecharge.setOperatorId(1L);
        anotherRecharge.setOperatorName("Airtel");
        anotherRecharge.setPlanId(10L);
        anotherRecharge.setPlanName("Unlimited 84 Days");
        anotherRecharge.setAmount(new BigDecimal("599.00"));
        anotherRecharge.setPlanValidityDays(84);
        anotherRecharge.setPlanExpiryDate(LocalDate.now().plusDays(84));
        anotherRecharge.setStatus(RechargeStatus.SUCCESS);
        entityManager.persist(anotherRecharge);
        entityManager.flush();

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Recharge> found = rechargeRepository.findAllByPlanId(10L, pageable);

        // Then
        assertThat(found.getContent()).hasSize(2);
        assertThat(found.getContent()).extracting(Recharge::getPlanId).containsOnly(10L);
    }

    @Test
    void testFindAllByOperatorId() {
        // Given
        entityManager.persist(testRecharge);
        entityManager.flush();

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Recharge> found = rechargeRepository.findAllByOperatorId(1L, pageable);

        // Then
        assertThat(found.getContent()).hasSize(1);
        assertThat(found.getContent().get(0).getOperatorId()).isEqualTo(1L);
    }

    @Test
    void testFindByPlanIdAndStatus() {
        // Given
        entityManager.persist(testRecharge);
        entityManager.flush();

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Recharge> found = rechargeRepository.findByPlanIdAndStatus(10L, RechargeStatus.SUCCESS, pageable);

        // Then
        assertThat(found.getContent()).hasSize(1);
        assertThat(found.getContent().get(0).getStatus()).isEqualTo(RechargeStatus.SUCCESS);
    }

    @Test
    void testSearchByPlanId() {
        // Given
        entityManager.persist(testRecharge);
        entityManager.flush();

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Recharge> found = rechargeRepository.searchByPlanId(10L, "9876", pageable);

        // Then
        assertThat(found.getContent()).hasSize(1);
        assertThat(found.getContent().get(0).getMobileNumber()).contains("9876");
    }

    @Test
    void testSearchByUserId() {
        // Given
        entityManager.persist(testRecharge);
        entityManager.flush();

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Recharge> found = rechargeRepository.searchByUserId(100L, "Airtel", pageable);

        // Then
        assertThat(found.getContent()).hasSize(1);
        assertThat(found.getContent().get(0).getOperatorName()).contains("Airtel");
    }

    @Test
    void testCountRechargesBetweenDates() {
        // Given
        entityManager.persist(testRecharge);
        entityManager.flush();

        // When
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        Long count = rechargeRepository.countRechargesBetweenDates(start, end, RechargeStatus.SUCCESS);

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testSumAmountBetweenDates() {
        // Given
        entityManager.persist(testRecharge);
        entityManager.flush();

        // When
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        BigDecimal sum = rechargeRepository.sumAmountBetweenDates(start, end, RechargeStatus.SUCCESS);

        // Then
        assertThat(sum).isEqualByComparingTo(new BigDecimal("599.00"));
    }
}

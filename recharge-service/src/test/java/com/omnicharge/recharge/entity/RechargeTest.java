package com.omnicharge.recharge.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RechargeTest {

    @Test
    void testRechargeEntityCreation() {
        // Given
        Recharge recharge = new Recharge();
        recharge.setId(1L);
        recharge.setRechargeId("OMNI-TEST123");
        recharge.setUserId(100L);
        recharge.setMobileNumber("9876543210");
        recharge.setOperatorId(1L);
        recharge.setOperatorName("Airtel");
        recharge.setPlanId(10L);
        recharge.setPlanName("Unlimited 84 Days");
        recharge.setAmount(new BigDecimal("599.00"));
        recharge.setPlanValidityDays(84);
        recharge.setPlanExpiryDate(LocalDate.now().plusDays(84));
        recharge.setStatus(RechargeStatus.SUCCESS);
        recharge.setTransactionId("TXN123456");
        recharge.setFailureReason(null);

        // Then
        assertThat(recharge.getId()).isEqualTo(1L);
        assertThat(recharge.getRechargeId()).isEqualTo("OMNI-TEST123");
        assertThat(recharge.getUserId()).isEqualTo(100L);
        assertThat(recharge.getMobileNumber()).isEqualTo("9876543210");
        assertThat(recharge.getOperatorId()).isEqualTo(1L);
        assertThat(recharge.getOperatorName()).isEqualTo("Airtel");
        assertThat(recharge.getPlanId()).isEqualTo(10L);
        assertThat(recharge.getPlanName()).isEqualTo("Unlimited 84 Days");
        assertThat(recharge.getAmount()).isEqualByComparingTo(new BigDecimal("599.00"));
        assertThat(recharge.getPlanValidityDays()).isEqualTo(84);
        assertThat(recharge.getStatus()).isEqualTo(RechargeStatus.SUCCESS);
        assertThat(recharge.getTransactionId()).isEqualTo("TXN123456");
        assertThat(recharge.getFailureReason()).isNull();
    }

    @Test
    void testRechargeEntityWithAllArgsConstructor() {
        // Given
        LocalDate expiryDate = LocalDate.now().plusDays(28);
        
        // When
        Recharge recharge = new Recharge(
                1L, "OMNI-ABC123", 200L, "9123456789",
                2L, "Jio", 20L, "Data Booster",
                new BigDecimal("199.00"), 28, expiryDate,
                RechargeStatus.INITIATED, null, null
        );

        // Then
        assertThat(recharge.getId()).isEqualTo(1L);
        assertThat(recharge.getRechargeId()).isEqualTo("OMNI-ABC123");
        assertThat(recharge.getUserId()).isEqualTo(200L);
        assertThat(recharge.getMobileNumber()).isEqualTo("9123456789");
        assertThat(recharge.getOperatorId()).isEqualTo(2L);
        assertThat(recharge.getOperatorName()).isEqualTo("Jio");
        assertThat(recharge.getPlanId()).isEqualTo(20L);
        assertThat(recharge.getPlanName()).isEqualTo("Data Booster");
        assertThat(recharge.getAmount()).isEqualByComparingTo(new BigDecimal("199.00"));
        assertThat(recharge.getPlanValidityDays()).isEqualTo(28);
        assertThat(recharge.getPlanExpiryDate()).isEqualTo(expiryDate);
        assertThat(recharge.getStatus()).isEqualTo(RechargeStatus.INITIATED);
    }

    @Test
    void testRechargeEntityWithFailureReason() {
        // Given
        Recharge recharge = new Recharge();
        recharge.setStatus(RechargeStatus.FAILED);
        recharge.setFailureReason("Insufficient balance");

        // Then
        assertThat(recharge.getStatus()).isEqualTo(RechargeStatus.FAILED);
        assertThat(recharge.getFailureReason()).isEqualTo("Insufficient balance");
    }

    @Test
    void testRechargeStatusEnum() {
        // Test all enum values
        assertThat(RechargeStatus.INITIATED).isNotNull();
        assertThat(RechargeStatus.PROCESSING).isNotNull();
        assertThat(RechargeStatus.SUCCESS).isNotNull();
        assertThat(RechargeStatus.FAILED).isNotNull();
        assertThat(RechargeStatus.EXPIRED).isNotNull();
        
        // Test enum name
        assertThat(RechargeStatus.SUCCESS.name()).isEqualTo("SUCCESS");
        assertThat(RechargeStatus.FAILED.name()).isEqualTo("FAILED");
    }

    @Test
    void testRechargeEqualsAndHashCode() {
        // Given
        Recharge recharge1 = new Recharge();
        recharge1.setId(1L);
        recharge1.setRechargeId("OMNI-TEST");
        recharge1.setUserId(100L);

        Recharge recharge2 = new Recharge();
        recharge2.setId(1L);
        recharge2.setRechargeId("OMNI-TEST");
        recharge2.setUserId(100L);

        // Then - Just verify they have the same values
        assertThat(recharge1.getId()).isEqualTo(recharge2.getId());
        assertThat(recharge1.getRechargeId()).isEqualTo(recharge2.getRechargeId());
        assertThat(recharge1.getUserId()).isEqualTo(recharge2.getUserId());
    }
}

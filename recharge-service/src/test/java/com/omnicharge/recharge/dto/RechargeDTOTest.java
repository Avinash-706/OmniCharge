package com.omnicharge.recharge.dto;

import com.omnicharge.recharge.entity.RechargeStatus;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RechargeDTOTest {

    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    @Test
    void testRechargeRequestValidation_Success() {
        // Given
        RechargeRequest request = new RechargeRequest();
        request.setMobileNumber("9876543210");
        request.setOperatorId(1L);
        request.setPlanId(10L);
        request.setPaymentMethod("RAZORPAY");

        // When
        Set<ConstraintViolation<RechargeRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void testRechargeRequestValidation_InvalidMobileNumber() {
        // Given
        RechargeRequest request = new RechargeRequest();
        request.setMobileNumber("123456"); // Invalid
        request.setOperatorId(1L);
        request.setPlanId(10L);
        request.setPaymentMethod("RAZORPAY");

        // When
        Set<ConstraintViolation<RechargeRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("Invalid mobile number"));
    }

    @Test
    void testRechargeRequestValidation_BlankMobileNumber() {
        // Given
        RechargeRequest request = new RechargeRequest();
        request.setMobileNumber("");
        request.setOperatorId(1L);
        request.setPlanId(10L);
        request.setPaymentMethod("RAZORPAY");

        // When
        Set<ConstraintViolation<RechargeRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void testRechargeRequestValidation_NullOperatorId() {
        // Given
        RechargeRequest request = new RechargeRequest();
        request.setMobileNumber("9876543210");
        request.setOperatorId(null);
        request.setPlanId(10L);
        request.setPaymentMethod("RAZORPAY");

        // When
        Set<ConstraintViolation<RechargeRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("Operator ID is required"));
    }

    @Test
    void testRechargeRequestValidation_NullPlanId() {
        // Given
        RechargeRequest request = new RechargeRequest();
        request.setMobileNumber("9876543210");
        request.setOperatorId(1L);
        request.setPlanId(null);
        request.setPaymentMethod("RAZORPAY");

        // When
        Set<ConstraintViolation<RechargeRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("Plan ID is required"));
    }

    @Test
    void testRechargeRequestValidation_BlankPaymentMethod() {
        // Given
        RechargeRequest request = new RechargeRequest();
        request.setMobileNumber("9876543210");
        request.setOperatorId(1L);
        request.setPlanId(10L);
        request.setPaymentMethod("");

        // When
        Set<ConstraintViolation<RechargeRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("Payment method is required"));
    }

    @Test
    void testRechargeResponseBuilder() {
        // Given
        LocalDate expiryDate = LocalDate.now().plusDays(28);
        LocalDateTime createdDate = LocalDateTime.now();

        // When
        RechargeResponse response = RechargeResponse.builder()
                .id(1L)
                .rechargeId("OMNI-TEST123")
                .userId(100L)
                .userFullName("John Doe")
                .mobileNumber("9876543210")
                .operatorId(1L)
                .operatorName("Airtel")
                .planId(10L)
                .planName("Unlimited Plan")
                .amount(new BigDecimal("599.00"))
                .planValidityDays(28)
                .planExpiryDate(expiryDate)
                .status(RechargeStatus.SUCCESS)
                .transactionId("TXN123")
                .createdDate(createdDate)
                .build();

        // Then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRechargeId()).isEqualTo("OMNI-TEST123");
        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getUserFullName()).isEqualTo("John Doe");
        assertThat(response.getMobileNumber()).isEqualTo("9876543210");
        assertThat(response.getOperatorName()).isEqualTo("Airtel");
        assertThat(response.getPlanName()).isEqualTo("Unlimited Plan");
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("599.00"));
        assertThat(response.getStatus()).isEqualTo(RechargeStatus.SUCCESS);
        assertThat(response.getTransactionId()).isEqualTo("TXN123");
    }

    @Test
    void testPaymentRequestBuilder() {
        // When
        PaymentRequest request = PaymentRequest.builder()
                .rechargeId("OMNI-TEST123")
                .userId(100L)
                .amount(new BigDecimal("599.00"))
                .paymentMethod("RAZORPAY")
                .userEmail("test@example.com")
                .userMobile("9876543210")
                .build();

        // Then
        assertThat(request.getRechargeId()).isEqualTo("OMNI-TEST123");
        assertThat(request.getUserId()).isEqualTo(100L);
        assertThat(request.getAmount()).isEqualByComparingTo(new BigDecimal("599.00"));
        assertThat(request.getPaymentMethod()).isEqualTo("RAZORPAY");
        assertThat(request.getUserEmail()).isEqualTo("test@example.com");
        assertThat(request.getUserMobile()).isEqualTo("9876543210");
    }

    @Test
    void testPaymentResponseBuilder() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();

        // When
        PaymentResponse response = PaymentResponse.builder()
                .transactionId("TXN123")
                .status("SUCCESS")
                .razorpayOrderId("order_123")
                .amount(new BigDecimal("599.00"))
                .timestamp(timestamp)
                .build();

        // Then
        assertThat(response.getTransactionId()).isEqualTo("TXN123");
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getRazorpayOrderId()).isEqualTo("order_123");
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("599.00"));
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void testRechargeAnalyticsResponseBuilder() {
        // When
        RechargeAnalyticsResponse response = RechargeAnalyticsResponse.builder()
                .totalRecharges(1000L)
                .todayRecharges(50L)
                .monthRecharges(300L)
                .totalRevenue(new BigDecimal("500000.00"))
                .todayRevenue(new BigDecimal("25000.00"))
                .monthRevenue(new BigDecimal("150000.00"))
                .successRate(95.5)
                .successCount(955L)
                .failedCount(45L)
                .pendingCount(10L)
                .activeRecharges(800L)
                .expiredRecharges(200L)
                .activeRatio(80.0)
                .build();

        // Then
        assertThat(response.getTotalRecharges()).isEqualTo(1000L);
        assertThat(response.getTodayRecharges()).isEqualTo(50L);
        assertThat(response.getSuccessRate()).isEqualTo(95.5);
        assertThat(response.getActiveRatio()).isEqualTo(80.0);
    }

    @Test
    void testOperatorMarketShareBuilder() {
        // When
        OperatorMarketShare share = OperatorMarketShare.builder()
                .operatorId(1L)
                .operatorName("Airtel")
                .rechargeCount(500L)
                .totalRevenue(new BigDecimal("250000.00"))
                .marketSharePercentage(50.0)
                .build();

        // Then
        assertThat(share.getOperatorId()).isEqualTo(1L);
        assertThat(share.getOperatorName()).isEqualTo("Airtel");
        assertThat(share.getRechargeCount()).isEqualTo(500L);
        assertThat(share.getMarketSharePercentage()).isEqualTo(50.0);
    }

    @Test
    void testPlanPerformanceStatsBuilder() {
        // When
        PlanPerformanceStats stats = PlanPerformanceStats.builder()
                .planId(10L)
                .planName("Unlimited 84 Days")
                .operatorId(1L)
                .operatorName("Airtel")
                .rechargeCount(200L)
                .totalRevenue(new BigDecimal("119800.00"))
                .averageAmount(new BigDecimal("599.00"))
                .build();

        // Then
        assertThat(stats.getPlanId()).isEqualTo(10L);
        assertThat(stats.getPlanName()).isEqualTo("Unlimited 84 Days");
        assertThat(stats.getRechargeCount()).isEqualTo(200L);
        assertThat(stats.getAverageAmount()).isEqualByComparingTo(new BigDecimal("599.00"));
    }

    @Test
    void testExpiringRechargeResponseBuilder() {
        // Given
        LocalDate expiryDate = LocalDate.now().plusDays(5);

        // When
        ExpiringRechargeResponse response = ExpiringRechargeResponse.builder()
                .rechargeId("OMNI-TEST123")
                .userId(100L)
                .userEmail("test@example.com")
                .userMobile("9876543210")
                .mobileNumber("9123456789")
                .operatorName("Airtel")
                .planName("Unlimited Plan")
                .amount(new BigDecimal("599.00"))
                .expiryDate(expiryDate)
                .build();

        // Then
        assertThat(response.getRechargeId()).isEqualTo("OMNI-TEST123");
        assertThat(response.getUserEmail()).isEqualTo("test@example.com");
        assertThat(response.getExpiryDate()).isEqualTo(expiryDate);
    }

    @Test
    void testRechargeStatsResponseBuilder() {
        // When
        RechargeStatsResponse stats = RechargeStatsResponse.builder()
                .totalRecharges(1000L)
                .successCount(950L)
                .failedCount(50L)
                .totalAmount(new BigDecimal("500000.00"))
                .build();

        // Then
        assertThat(stats.getTotalRecharges()).isEqualTo(1000L);
        assertThat(stats.getSuccessCount()).isEqualTo(950L);
        assertThat(stats.getFailedCount()).isEqualTo(50L);
        assertThat(stats.getTotalAmount()).isEqualByComparingTo(new BigDecimal("500000.00"));
    }
}

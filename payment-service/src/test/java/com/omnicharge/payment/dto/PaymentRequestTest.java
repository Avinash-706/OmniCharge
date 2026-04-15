package com.omnicharge.payment.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PaymentRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testNoArgsConstructor() {
        PaymentRequest request = new PaymentRequest();
        assertNotNull(request);
    }

    @Test
    void testAllArgsConstructor() {
        PaymentRequest request = new PaymentRequest(
                "RECH123",
                1L,
                new BigDecimal("100.00"),
                "UPI",
                "user@test.com",
                "1234567890",
                "9876543210",
                "Airtel",
                "Unlimited Plan"
        );

        assertEquals("RECH123", request.getRechargeId());
        assertEquals(1L, request.getUserId());
        assertEquals(new BigDecimal("100.00"), request.getAmount());
        assertEquals("UPI", request.getPaymentMethod());
        assertEquals("user@test.com", request.getUserEmail());
        assertEquals("1234567890", request.getUserMobile());
        assertEquals("9876543210", request.getMobileNumber());
        assertEquals("Airtel", request.getOperatorName());
        assertEquals("Unlimited Plan", request.getPlanName());
    }

    @Test
    void testBuilder() {
        PaymentRequest request = PaymentRequest.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("UPI")
                .userEmail("user@test.com")
                .userMobile("1234567890")
                .mobileNumber("9876543210")
                .operatorName("Airtel")
                .planName("Unlimited Plan")
                .build();

        assertEquals("RECH123", request.getRechargeId());
        assertEquals(1L, request.getUserId());
        assertEquals(new BigDecimal("100.00"), request.getAmount());
        assertEquals("UPI", request.getPaymentMethod());
    }

    @Test
    void testValidRequest() {
        PaymentRequest request = PaymentRequest.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("UPI")
                .build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testBlankRechargeId() {
        PaymentRequest request = PaymentRequest.builder()
                .rechargeId("")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("UPI")
                .build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Recharge ID is required")));
    }

    @Test
    void testNullRechargeId() {
        PaymentRequest request = PaymentRequest.builder()
                .rechargeId(null)
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("UPI")
                .build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Recharge ID is required")));
    }

    @Test
    void testNullUserId() {
        PaymentRequest request = PaymentRequest.builder()
                .rechargeId("RECH123")
                .userId(null)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("UPI")
                .build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("User ID is required")));
    }

    @Test
    void testNullAmount() {
        PaymentRequest request = PaymentRequest.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(null)
                .paymentMethod("UPI")
                .build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Amount is required")));
    }

    @Test
    void testZeroAmount() {
        PaymentRequest request = PaymentRequest.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(BigDecimal.ZERO)
                .paymentMethod("UPI")
                .build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Amount must be greater than 0")));
    }

    @Test
    void testNegativeAmount() {
        PaymentRequest request = PaymentRequest.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("-10.00"))
                .paymentMethod("UPI")
                .build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Amount must be greater than 0")));
    }

    @Test
    void testMinimumValidAmount() {
        PaymentRequest request = PaymentRequest.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("0.01"))
                .paymentMethod("UPI")
                .build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testBlankPaymentMethod() {
        PaymentRequest request = PaymentRequest.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("")
                .build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Payment method is required")));
    }

    @Test
    void testNullPaymentMethod() {
        PaymentRequest request = PaymentRequest.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod(null)
                .build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Payment method is required")));
    }

    @Test
    void testOptionalFieldsNull() {
        PaymentRequest request = PaymentRequest.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("UPI")
                .userEmail(null)
                .userMobile(null)
                .mobileNumber(null)
                .operatorName(null)
                .planName(null)
                .build();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testGettersAndSetters() {
        PaymentRequest request = new PaymentRequest();
        
        request.setRechargeId("RECH123");
        request.setUserId(1L);
        request.setAmount(new BigDecimal("100.00"));
        request.setPaymentMethod("UPI");
        request.setUserEmail("user@test.com");
        request.setUserMobile("1234567890");
        request.setMobileNumber("9876543210");
        request.setOperatorName("Airtel");
        request.setPlanName("Unlimited Plan");

        assertEquals("RECH123", request.getRechargeId());
        assertEquals(1L, request.getUserId());
        assertEquals(new BigDecimal("100.00"), request.getAmount());
        assertEquals("UPI", request.getPaymentMethod());
        assertEquals("user@test.com", request.getUserEmail());
        assertEquals("1234567890", request.getUserMobile());
        assertEquals("9876543210", request.getMobileNumber());
        assertEquals("Airtel", request.getOperatorName());
        assertEquals("Unlimited Plan", request.getPlanName());
    }

    @Test
    void testToString() {
        PaymentRequest request = PaymentRequest.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("UPI")
                .build();

        String toString = request.toString();
        assertTrue(toString.contains("RECH123"));
        assertTrue(toString.contains("100.00"));
        assertTrue(toString.contains("UPI"));
    }
}

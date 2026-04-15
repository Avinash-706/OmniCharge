package com.omnicharge.operator.dto;

import com.omnicharge.operator.entity.PlanCategory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PlanRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testNoArgsConstructor() {
        PlanRequest request = new PlanRequest();
        assertThat(request).isNotNull();
    }

    @Test
    void testAllArgsConstructor() {
        PlanRequest request = new PlanRequest(
                "Unlimited Plan",
                new BigDecimal("299.00"),
                28,
                "2GB/day",
                "Unlimited",
                "100 SMS/day",
                "Free subscription",
                PlanCategory.UNLIMITED
        );

        assertThat(request.getPlanName()).isEqualTo("Unlimited Plan");
        assertThat(request.getPrice()).isEqualByComparingTo(new BigDecimal("299.00"));
        assertThat(request.getValidityDays()).isEqualTo(28);
        assertThat(request.getDataLimit()).isEqualTo("2GB/day");
        assertThat(request.getCallBenefit()).isEqualTo("Unlimited");
        assertThat(request.getSmsBenefit()).isEqualTo("100 SMS/day");
        assertThat(request.getAdditionalBenefits()).isEqualTo("Free subscription");
        assertThat(request.getCategory()).isEqualTo(PlanCategory.UNLIMITED);
    }

    @Test
    void testValidRequest() {
        PlanRequest request = new PlanRequest(
                "Data Plan",
                new BigDecimal("199.00"),
                30,
                "1.5GB/day",
                "Unlimited",
                "100 SMS/day",
                null,
                PlanCategory.DATA
        );

        Set<ConstraintViolation<PlanRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void testBlankPlanName() {
        PlanRequest request = new PlanRequest(
                "",
                new BigDecimal("299.00"),
                28,
                "2GB/day",
                "Unlimited",
                "100 SMS/day",
                null,
                PlanCategory.UNLIMITED
        );

        Set<ConstraintViolation<PlanRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Plan name is required");
    }

    @Test
    void testNullPrice() {
        PlanRequest request = new PlanRequest(
                "Plan",
                null,
                28,
                "2GB/day",
                "Unlimited",
                "100 SMS/day",
                null,
                PlanCategory.UNLIMITED
        );

        Set<ConstraintViolation<PlanRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Price is required");
    }

    @Test
    void testNegativePrice() {
        PlanRequest request = new PlanRequest(
                "Plan",
                new BigDecimal("-10.00"),
                28,
                "2GB/day",
                "Unlimited",
                "100 SMS/day",
                null,
                PlanCategory.UNLIMITED
        );

        Set<ConstraintViolation<PlanRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Price must be positive");
    }

    @Test
    void testZeroPrice() {
        PlanRequest request = new PlanRequest(
                "Plan",
                BigDecimal.ZERO,
                28,
                "2GB/day",
                "Unlimited",
                "100 SMS/day",
                null,
                PlanCategory.UNLIMITED
        );

        Set<ConstraintViolation<PlanRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Price must be positive");
    }

    @Test
    void testNullValidityDays() {
        PlanRequest request = new PlanRequest(
                "Plan",
                new BigDecimal("299.00"),
                null,
                "2GB/day",
                "Unlimited",
                "100 SMS/day",
                null,
                PlanCategory.UNLIMITED
        );

        Set<ConstraintViolation<PlanRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Validity days is required");
    }

    @Test
    void testNegativeValidityDays() {
        PlanRequest request = new PlanRequest(
                "Plan",
                new BigDecimal("299.00"),
                -5,
                "2GB/day",
                "Unlimited",
                "100 SMS/day",
                null,
                PlanCategory.UNLIMITED
        );

        Set<ConstraintViolation<PlanRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Validity days must be positive");
    }

    @Test
    void testZeroValidityDays() {
        PlanRequest request = new PlanRequest(
                "Plan",
                new BigDecimal("299.00"),
                0,
                "2GB/day",
                "Unlimited",
                "100 SMS/day",
                null,
                PlanCategory.UNLIMITED
        );

        Set<ConstraintViolation<PlanRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Validity days must be positive");
    }

    @Test
    void testNullCategory() {
        PlanRequest request = new PlanRequest(
                "Plan",
                new BigDecimal("299.00"),
                28,
                "2GB/day",
                "Unlimited",
                "100 SMS/day",
                null,
                null
        );

        Set<ConstraintViolation<PlanRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Category is required");
    }

    @Test
    void testOptionalFields() {
        PlanRequest request = new PlanRequest(
                "Basic Plan",
                new BigDecimal("99.00"),
                7,
                null,
                null,
                null,
                null,
                PlanCategory.TALKTIME
        );

        Set<ConstraintViolation<PlanRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void testMultipleViolations() {
        PlanRequest request = new PlanRequest(null, null, null, null, null, null, null, null);

        Set<ConstraintViolation<PlanRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(4); // planName, price, validityDays, category
    }

    @Test
    void testEqualsAndHashCode() {
        PlanRequest request1 = new PlanRequest("Plan", new BigDecimal("299.00"), 28, "2GB", "Unlimited", "100 SMS", "Benefits", PlanCategory.UNLIMITED);
        PlanRequest request2 = new PlanRequest("Plan", new BigDecimal("299.00"), 28, "2GB", "Unlimited", "100 SMS", "Benefits", PlanCategory.UNLIMITED);
        PlanRequest request3 = new PlanRequest("Other", new BigDecimal("199.00"), 30, "1GB", "Limited", "50 SMS", "None", PlanCategory.DATA);

        assertThat(request1).isEqualTo(request2);
        assertThat(request1).isNotEqualTo(request3);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    void testToString() {
        PlanRequest request = new PlanRequest("Plan", new BigDecimal("299.00"), 28, "2GB", "Unlimited", "100 SMS", "Benefits", PlanCategory.UNLIMITED);
        String toString = request.toString();

        assertThat(toString).contains("planName=Plan");
        assertThat(toString).contains("price=299");
        assertThat(toString).contains("validityDays=28");
        assertThat(toString).contains("category=UNLIMITED");
    }
}

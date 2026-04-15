package com.omnicharge.operator.dto;

import com.omnicharge.operator.entity.OperatorCategory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OperatorRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testNoArgsConstructor() {
        OperatorRequest request = new OperatorRequest();
        assertThat(request).isNotNull();
        assertThat(request.getName()).isNull();
        assertThat(request.getCode()).isNull();
        assertThat(request.getCategory()).isNull();
        assertThat(request.getLogoUrl()).isNull();
    }

    @Test
    void testAllArgsConstructor() {
        OperatorRequest request = new OperatorRequest(
                "Airtel",
                "AIRTEL",
                OperatorCategory.PREPAID,
                "https://logo.url"
        );

        assertThat(request.getName()).isEqualTo("Airtel");
        assertThat(request.getCode()).isEqualTo("AIRTEL");
        assertThat(request.getCategory()).isEqualTo(OperatorCategory.PREPAID);
        assertThat(request.getLogoUrl()).isEqualTo("https://logo.url");
    }

    @Test
    void testSettersAndGetters() {
        OperatorRequest request = new OperatorRequest();
        
        request.setName("Jio");
        request.setCode("JIO");
        request.setCategory(OperatorCategory.POSTPAID);
        request.setLogoUrl("https://jio.logo");

        assertThat(request.getName()).isEqualTo("Jio");
        assertThat(request.getCode()).isEqualTo("JIO");
        assertThat(request.getCategory()).isEqualTo(OperatorCategory.POSTPAID);
        assertThat(request.getLogoUrl()).isEqualTo("https://jio.logo");
    }

    @Test
    void testValidRequest() {
        OperatorRequest request = new OperatorRequest(
                "Airtel",
                "AIRTEL",
                OperatorCategory.PREPAID,
                "https://logo.url"
        );

        Set<ConstraintViolation<OperatorRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void testBlankName() {
        OperatorRequest request = new OperatorRequest(
                "",
                "AIRTEL",
                OperatorCategory.PREPAID,
                "https://logo.url"
        );

        Set<ConstraintViolation<OperatorRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Operator name is required");
    }

    @Test
    void testNullName() {
        OperatorRequest request = new OperatorRequest(
                null,
                "AIRTEL",
                OperatorCategory.PREPAID,
                "https://logo.url"
        );

        Set<ConstraintViolation<OperatorRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Operator name is required");
    }

    @Test
    void testBlankCode() {
        OperatorRequest request = new OperatorRequest(
                "Airtel",
                "",
                OperatorCategory.PREPAID,
                "https://logo.url"
        );

        Set<ConstraintViolation<OperatorRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Operator code is required");
    }

    @Test
    void testNullCode() {
        OperatorRequest request = new OperatorRequest(
                "Airtel",
                null,
                OperatorCategory.PREPAID,
                "https://logo.url"
        );

        Set<ConstraintViolation<OperatorRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Operator code is required");
    }

    @Test
    void testNullCategory() {
        OperatorRequest request = new OperatorRequest(
                "Airtel",
                "AIRTEL",
                null,
                "https://logo.url"
        );

        Set<ConstraintViolation<OperatorRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Category is required");
    }

    @Test
    void testMultipleViolations() {
        OperatorRequest request = new OperatorRequest(null, null, null, null);

        Set<ConstraintViolation<OperatorRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(3);
    }

    @Test
    void testLogoUrlOptional() {
        OperatorRequest request = new OperatorRequest(
                "Airtel",
                "AIRTEL",
                OperatorCategory.PREPAID,
                null
        );

        Set<ConstraintViolation<OperatorRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void testEqualsAndHashCode() {
        OperatorRequest request1 = new OperatorRequest("Airtel", "AIRTEL", OperatorCategory.PREPAID, "url");
        OperatorRequest request2 = new OperatorRequest("Airtel", "AIRTEL", OperatorCategory.PREPAID, "url");
        OperatorRequest request3 = new OperatorRequest("Jio", "JIO", OperatorCategory.POSTPAID, "url2");

        assertThat(request1).isEqualTo(request2);
        assertThat(request1).isNotEqualTo(request3);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    void testToString() {
        OperatorRequest request = new OperatorRequest("Airtel", "AIRTEL", OperatorCategory.PREPAID, "url");
        String toString = request.toString();

        assertThat(toString).contains("name=Airtel");
        assertThat(toString).contains("code=AIRTEL");
        assertThat(toString).contains("category=PREPAID");
    }

    @Test
    void testAllCategories() {
        for (OperatorCategory category : OperatorCategory.values()) {
            OperatorRequest request = new OperatorRequest("Test", "TEST", category, "url");
            Set<ConstraintViolation<OperatorRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }
    }
}

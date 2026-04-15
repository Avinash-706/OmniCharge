package com.omnicharge.operator.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PlanTest {

    @Test
    void testNoArgsConstructor() {
        Plan plan = new Plan();
        assertThat(plan).isNotNull();
    }

    @Test
    void testAllArgsConstructor() {
        Operator operator = new Operator();
        operator.setId(1L);
        operator.setName("Airtel");

        Plan plan = new Plan(
                1L,
                operator,
                "Unlimited Plan",
                new BigDecimal("299.00"),
                28,
                "2GB/day",
                "Unlimited",
                "100 SMS/day",
                "Free subscription",
                PlanCategory.UNLIMITED,
                true,
                false
        );

        assertThat(plan.getId()).isEqualTo(1L);
        assertThat(plan.getOperator()).isEqualTo(operator);
        assertThat(plan.getPlanName()).isEqualTo("Unlimited Plan");
        assertThat(plan.getPrice()).isEqualByComparingTo(new BigDecimal("299.00"));
        assertThat(plan.getValidityDays()).isEqualTo(28);
        assertThat(plan.getDataLimit()).isEqualTo("2GB/day");
        assertThat(plan.getCallBenefit()).isEqualTo("Unlimited");
        assertThat(plan.getSmsBenefit()).isEqualTo("100 SMS/day");
        assertThat(plan.getAdditionalBenefits()).isEqualTo("Free subscription");
        assertThat(plan.getCategory()).isEqualTo(PlanCategory.UNLIMITED);
        assertThat(plan.getIsActive()).isTrue();
        assertThat(plan.getDeactivatedByOperator()).isFalse();
    }

    @Test
    void testSettersAndGetters() {
        Operator operator = new Operator();
        operator.setId(5L);
        operator.setName("Jio");

        Plan plan = new Plan();
        plan.setId(10L);
        plan.setOperator(operator);
        plan.setPlanName("Data Plan");
        plan.setPrice(new BigDecimal("199.00"));
        plan.setValidityDays(30);
        plan.setDataLimit("1.5GB/day");
        plan.setCallBenefit("Unlimited");
        plan.setSmsBenefit("100 SMS/day");
        plan.setAdditionalBenefits("Free OTT");
        plan.setCategory(PlanCategory.DATA);
        plan.setIsActive(false);
        plan.setDeactivatedByOperator(true);

        assertThat(plan.getId()).isEqualTo(10L);
        assertThat(plan.getOperator()).isEqualTo(operator);
        assertThat(plan.getPlanName()).isEqualTo("Data Plan");
        assertThat(plan.getPrice()).isEqualByComparingTo(new BigDecimal("199.00"));
        assertThat(plan.getValidityDays()).isEqualTo(30);
        assertThat(plan.getDataLimit()).isEqualTo("1.5GB/day");
        assertThat(plan.getCallBenefit()).isEqualTo("Unlimited");
        assertThat(plan.getSmsBenefit()).isEqualTo("100 SMS/day");
        assertThat(plan.getAdditionalBenefits()).isEqualTo("Free OTT");
        assertThat(plan.getCategory()).isEqualTo(PlanCategory.DATA);
        assertThat(plan.getIsActive()).isFalse();
        assertThat(plan.getDeactivatedByOperator()).isTrue();
    }

    @Test
    void testDefaultIsActive() {
        Plan plan = new Plan();
        plan.setIsActive(true);
        assertThat(plan.getIsActive()).isTrue();
    }

    @Test
    void testDefaultDeactivatedByOperator() {
        Plan plan = new Plan();
        plan.setDeactivatedByOperator(false);
        assertThat(plan.getDeactivatedByOperator()).isFalse();
    }

    @Test
    void testOperatorRelationship() {
        Operator operator = new Operator();
        operator.setId(1L);
        operator.setName("Airtel");
        operator.setCode("AIRTEL");

        Plan plan = new Plan();
        plan.setId(1L);
        plan.setPlanName("Test Plan");
        plan.setOperator(operator);

        assertThat(plan.getOperator()).isNotNull();
        assertThat(plan.getOperator().getName()).isEqualTo("Airtel");
        assertThat(plan.getOperator().getCode()).isEqualTo("AIRTEL");
    }

    @Test
    void testAllCategories() {
        for (PlanCategory category : PlanCategory.values()) {
            Plan plan = new Plan();
            plan.setCategory(category);
            assertThat(plan.getCategory()).isEqualTo(category);
        }
    }

    @Test
    void testWithNullOptionalFields() {
        Plan plan = new Plan();
        plan.setPlanName("Basic Plan");
        plan.setPrice(new BigDecimal("99.00"));
        plan.setValidityDays(7);
        plan.setDataLimit(null);
        plan.setCallBenefit(null);
        plan.setSmsBenefit(null);
        plan.setAdditionalBenefits(null);
        plan.setCategory(PlanCategory.TALKTIME);

        assertThat(plan.getDataLimit()).isNull();
        assertThat(plan.getCallBenefit()).isNull();
        assertThat(plan.getSmsBenefit()).isNull();
        assertThat(plan.getAdditionalBenefits()).isNull();
    }

    @Test
    void testInactivePlan() {
        Plan plan = new Plan();
        plan.setPlanName("Inactive Plan");
        plan.setIsActive(false);

        assertThat(plan.getIsActive()).isFalse();
    }

    @Test
    void testDeactivatedByOperatorFlag() {
        Plan plan = new Plan();
        plan.setPlanName("Deactivated Plan");
        plan.setIsActive(false);
        plan.setDeactivatedByOperator(true);

        assertThat(plan.getIsActive()).isFalse();
        assertThat(plan.getDeactivatedByOperator()).isTrue();
    }

    @Test
    void testPriceWithDecimals() {
        Plan plan = new Plan();
        plan.setPrice(new BigDecimal("299.99"));

        assertThat(plan.getPrice()).isEqualByComparingTo(new BigDecimal("299.99"));
    }

    @Test
    void testLongValidityDays() {
        Plan plan = new Plan();
        plan.setValidityDays(365);

        assertThat(plan.getValidityDays()).isEqualTo(365);
    }

    @Test
    void testLongAdditionalBenefits() {
        Plan plan = new Plan();
        String longBenefits = "This is a very long additional benefits description that contains multiple features and benefits for the plan including free OTT subscriptions, unlimited data rollover, international roaming, and many more features that make this plan attractive to customers.";
        plan.setAdditionalBenefits(longBenefits);

        assertThat(plan.getAdditionalBenefits()).isEqualTo(longBenefits);
        assertThat(plan.getAdditionalBenefits().length()).isLessThanOrEqualTo(500);
    }
}

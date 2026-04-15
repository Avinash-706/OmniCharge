package com.omnicharge.operator.entity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OperatorTest {

    @Test
    void testNoArgsConstructor() {
        Operator operator = new Operator();
        assertThat(operator).isNotNull();
        assertThat(operator.getPlans()).isNotNull().isEmpty();
    }

    @Test
    void testAllArgsConstructor() {
        List<Plan> plans = new ArrayList<>();
        Operator operator = new Operator(
                1L,
                "Airtel",
                "AIRTEL",
                OperatorCategory.PREPAID,
                "https://logo.url",
                true,
                plans
        );

        assertThat(operator.getId()).isEqualTo(1L);
        assertThat(operator.getName()).isEqualTo("Airtel");
        assertThat(operator.getCode()).isEqualTo("AIRTEL");
        assertThat(operator.getCategory()).isEqualTo(OperatorCategory.PREPAID);
        assertThat(operator.getLogoUrl()).isEqualTo("https://logo.url");
        assertThat(operator.getIsActive()).isTrue();
        assertThat(operator.getPlans()).isEmpty();
    }

    @Test
    void testSettersAndGetters() {
        Operator operator = new Operator();
        List<Plan> plans = new ArrayList<>();

        operator.setId(5L);
        operator.setName("Jio");
        operator.setCode("JIO");
        operator.setCategory(OperatorCategory.POSTPAID);
        operator.setLogoUrl("https://jio.logo");
        operator.setIsActive(false);
        operator.setPlans(plans);

        assertThat(operator.getId()).isEqualTo(5L);
        assertThat(operator.getName()).isEqualTo("Jio");
        assertThat(operator.getCode()).isEqualTo("JIO");
        assertThat(operator.getCategory()).isEqualTo(OperatorCategory.POSTPAID);
        assertThat(operator.getLogoUrl()).isEqualTo("https://jio.logo");
        assertThat(operator.getIsActive()).isFalse();
        assertThat(operator.getPlans()).isEmpty();
    }

    @Test
    void testDefaultIsActive() {
        Operator operator = new Operator();
        operator.setIsActive(true);
        assertThat(operator.getIsActive()).isTrue();
    }

    @Test
    void testPlansRelationship() {
        Operator operator = new Operator();
        operator.setId(1L);
        operator.setName("Airtel");
        operator.setCode("AIRTEL");
        operator.setCategory(OperatorCategory.PREPAID);
        
        List<Plan> plans = new ArrayList<>();
        Plan plan1 = new Plan();
        plan1.setId(1L);
        plan1.setPlanName("Plan 1");
        plan1.setOperator(operator);
        
        Plan plan2 = new Plan();
        plan2.setId(2L);
        plan2.setPlanName("Plan 2");
        plan2.setOperator(operator);
        
        plans.add(plan1);
        plans.add(plan2);
        operator.setPlans(plans);

        assertThat(operator.getPlans()).hasSize(2);
        assertThat(operator.getPlans().get(0).getPlanName()).isEqualTo("Plan 1");
        assertThat(operator.getPlans().get(1).getPlanName()).isEqualTo("Plan 2");
    }

    @Test
    void testAllCategories() {
        for (OperatorCategory category : OperatorCategory.values()) {
            Operator operator = new Operator();
            operator.setCategory(category);
            assertThat(operator.getCategory()).isEqualTo(category);
        }
    }

    @Test
    void testWithNullLogoUrl() {
        Operator operator = new Operator();
        operator.setName("Airtel");
        operator.setCode("AIRTEL");
        operator.setCategory(OperatorCategory.PREPAID);
        operator.setLogoUrl(null);

        assertThat(operator.getLogoUrl()).isNull();
    }

    @Test
    void testInactiveOperator() {
        Operator operator = new Operator();
        operator.setName("Inactive Operator");
        operator.setCode("INACTIVE");
        operator.setCategory(OperatorCategory.DTH);
        operator.setIsActive(false);

        assertThat(operator.getIsActive()).isFalse();
    }

    @Test
    void testEmptyPlansList() {
        Operator operator = new Operator();
        operator.setPlans(new ArrayList<>());
        
        assertThat(operator.getPlans()).isEmpty();
    }

    @Test
    void testAddPlanToOperator() {
        Operator operator = new Operator();
        operator.setId(1L);
        operator.setPlans(new ArrayList<>());
        
        Plan plan = new Plan();
        plan.setId(1L);
        plan.setPlanName("New Plan");
        plan.setOperator(operator);
        
        operator.getPlans().add(plan);
        
        assertThat(operator.getPlans()).hasSize(1);
        assertThat(operator.getPlans().get(0).getPlanName()).isEqualTo("New Plan");
    }
}

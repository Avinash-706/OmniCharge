package com.omnicharge.operator.dto;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PlanStatsResponseTest {

    @Test
    void testNoArgsConstructor() {
        PlanStatsResponse response = new PlanStatsResponse();
        assertThat(response).isNotNull();
        assertThat(response.getTotalPlans()).isNull();
        assertThat(response.getActivePlans()).isNull();
        assertThat(response.getInactivePlans()).isNull();
        assertThat(response.getPlansByCategory()).isNull();
    }

    @Test
    void testAllArgsConstructor() {
        Map<String, Long> plansByCategory = new HashMap<>();
        plansByCategory.put("RECOMMENDED", 10L);
        plansByCategory.put("DATA", 15L);
        plansByCategory.put("UNLIMITED", 20L);
        plansByCategory.put("TALKTIME", 5L);

        PlanStatsResponse response = new PlanStatsResponse(50L, 45L, 5L, plansByCategory);

        assertThat(response.getTotalPlans()).isEqualTo(50L);
        assertThat(response.getActivePlans()).isEqualTo(45L);
        assertThat(response.getInactivePlans()).isEqualTo(5L);
        assertThat(response.getPlansByCategory()).hasSize(4);
        assertThat(response.getPlansByCategory().get("RECOMMENDED")).isEqualTo(10L);
    }

    @Test
    void testBuilder() {
        Map<String, Long> plansByCategory = new HashMap<>();
        plansByCategory.put("DATA", 25L);
        plansByCategory.put("UNLIMITED", 30L);

        PlanStatsResponse response = PlanStatsResponse.builder()
                .totalPlans(100L)
                .activePlans(90L)
                .inactivePlans(10L)
                .plansByCategory(plansByCategory)
                .build();

        assertThat(response.getTotalPlans()).isEqualTo(100L);
        assertThat(response.getActivePlans()).isEqualTo(90L);
        assertThat(response.getInactivePlans()).isEqualTo(10L);
        assertThat(response.getPlansByCategory()).hasSize(2);
        assertThat(response.getPlansByCategory().get("DATA")).isEqualTo(25L);
        assertThat(response.getPlansByCategory().get("UNLIMITED")).isEqualTo(30L);
    }

    @Test
    void testSettersAndGetters() {
        PlanStatsResponse response = new PlanStatsResponse();
        Map<String, Long> plansByCategory = new HashMap<>();
        plansByCategory.put("RECOMMENDED", 5L);

        response.setTotalPlans(20L);
        response.setActivePlans(18L);
        response.setInactivePlans(2L);
        response.setPlansByCategory(plansByCategory);

        assertThat(response.getTotalPlans()).isEqualTo(20L);
        assertThat(response.getActivePlans()).isEqualTo(18L);
        assertThat(response.getInactivePlans()).isEqualTo(2L);
        assertThat(response.getPlansByCategory()).hasSize(1);
        assertThat(response.getPlansByCategory().get("RECOMMENDED")).isEqualTo(5L);
    }

    @Test
    void testEqualsAndHashCode() {
        Map<String, Long> plansByCategory1 = new HashMap<>();
        plansByCategory1.put("DATA", 10L);

        Map<String, Long> plansByCategory2 = new HashMap<>();
        plansByCategory2.put("DATA", 10L);

        Map<String, Long> plansByCategory3 = new HashMap<>();
        plansByCategory3.put("UNLIMITED", 20L);

        PlanStatsResponse response1 = new PlanStatsResponse(50L, 45L, 5L, plansByCategory1);
        PlanStatsResponse response2 = new PlanStatsResponse(50L, 45L, 5L, plansByCategory2);
        PlanStatsResponse response3 = new PlanStatsResponse(100L, 90L, 10L, plansByCategory3);

        assertThat(response1).isEqualTo(response2);
        assertThat(response1).isNotEqualTo(response3);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    void testToString() {
        Map<String, Long> plansByCategory = new HashMap<>();
        plansByCategory.put("DATA", 10L);

        PlanStatsResponse response = new PlanStatsResponse(50L, 45L, 5L, plansByCategory);
        String toString = response.toString();

        assertThat(toString).contains("totalPlans=50");
        assertThat(toString).contains("activePlans=45");
        assertThat(toString).contains("inactivePlans=5");
        assertThat(toString).contains("plansByCategory");
    }

    @Test
    void testWithEmptyPlansByCategory() {
        PlanStatsResponse response = PlanStatsResponse.builder()
                .totalPlans(0L)
                .activePlans(0L)
                .inactivePlans(0L)
                .plansByCategory(new HashMap<>())
                .build();

        assertThat(response.getTotalPlans()).isZero();
        assertThat(response.getActivePlans()).isZero();
        assertThat(response.getInactivePlans()).isZero();
        assertThat(response.getPlansByCategory()).isEmpty();
    }

    @Test
    void testWithNullPlansByCategory() {
        PlanStatsResponse response = PlanStatsResponse.builder()
                .totalPlans(10L)
                .activePlans(8L)
                .inactivePlans(2L)
                .plansByCategory(null)
                .build();

        assertThat(response.getPlansByCategory()).isNull();
    }

    @Test
    void testWithAllCategories() {
        Map<String, Long> plansByCategory = new HashMap<>();
        plansByCategory.put("RECOMMENDED", 10L);
        plansByCategory.put("DATA", 15L);
        plansByCategory.put("UNLIMITED", 20L);
        plansByCategory.put("TALKTIME", 5L);

        PlanStatsResponse response = PlanStatsResponse.builder()
                .totalPlans(50L)
                .activePlans(45L)
                .inactivePlans(5L)
                .plansByCategory(plansByCategory)
                .build();

        assertThat(response.getPlansByCategory()).hasSize(4);
        assertThat(response.getPlansByCategory().get("RECOMMENDED")).isEqualTo(10L);
        assertThat(response.getPlansByCategory().get("DATA")).isEqualTo(15L);
        assertThat(response.getPlansByCategory().get("UNLIMITED")).isEqualTo(20L);
        assertThat(response.getPlansByCategory().get("TALKTIME")).isEqualTo(5L);
    }

    @Test
    void testZeroValues() {
        PlanStatsResponse response = PlanStatsResponse.builder()
                .totalPlans(0L)
                .activePlans(0L)
                .inactivePlans(0L)
                .plansByCategory(new HashMap<>())
                .build();

        assertThat(response.getTotalPlans()).isZero();
        assertThat(response.getActivePlans()).isZero();
        assertThat(response.getInactivePlans()).isZero();
    }
}

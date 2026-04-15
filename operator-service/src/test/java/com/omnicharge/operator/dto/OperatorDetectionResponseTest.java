package com.omnicharge.operator.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OperatorDetectionResponseTest {

    @Test
    void testNoArgsConstructor() {
        OperatorDetectionResponse response = new OperatorDetectionResponse();
        assertThat(response).isNotNull();
        assertThat(response.getOperatorId()).isNull();
        assertThat(response.getOperatorName()).isNull();
        assertThat(response.getOperatorCode()).isNull();
        assertThat(response.getLogoUrl()).isNull();
        assertThat(response.getPlans()).isNull();
    }

    @Test
    void testAllArgsConstructor() {
        PlanResponse plan = PlanResponse.builder()
                .id(1L)
                .planName("Unlimited")
                .price(new BigDecimal("299.00"))
                .build();
        
        List<PlanResponse> plans = Collections.singletonList(plan);
        
        OperatorDetectionResponse response = new OperatorDetectionResponse(
                1L,
                "Airtel",
                "AIRTEL",
                "https://logo.url",
                plans
        );

        assertThat(response.getOperatorId()).isEqualTo(1L);
        assertThat(response.getOperatorName()).isEqualTo("Airtel");
        assertThat(response.getOperatorCode()).isEqualTo("AIRTEL");
        assertThat(response.getLogoUrl()).isEqualTo("https://logo.url");
        assertThat(response.getPlans()).hasSize(1);
    }

    @Test
    void testBuilder() {
        PlanResponse plan1 = PlanResponse.builder().id(1L).planName("Plan 1").build();
        PlanResponse plan2 = PlanResponse.builder().id(2L).planName("Plan 2").build();
        
        OperatorDetectionResponse response = OperatorDetectionResponse.builder()
                .operatorId(5L)
                .operatorName("Jio")
                .operatorCode("JIO")
                .logoUrl("https://jio.logo")
                .plans(Arrays.asList(plan1, plan2))
                .build();

        assertThat(response.getOperatorId()).isEqualTo(5L);
        assertThat(response.getOperatorName()).isEqualTo("Jio");
        assertThat(response.getOperatorCode()).isEqualTo("JIO");
        assertThat(response.getLogoUrl()).isEqualTo("https://jio.logo");
        assertThat(response.getPlans()).hasSize(2);
    }

    @Test
    void testSettersAndGetters() {
        OperatorDetectionResponse response = new OperatorDetectionResponse();
        
        response.setOperatorId(10L);
        response.setOperatorName("Vodafone");
        response.setOperatorCode("VODAFONE");
        response.setLogoUrl("https://vodafone.logo");
        response.setPlans(Collections.emptyList());

        assertThat(response.getOperatorId()).isEqualTo(10L);
        assertThat(response.getOperatorName()).isEqualTo("Vodafone");
        assertThat(response.getOperatorCode()).isEqualTo("VODAFONE");
        assertThat(response.getLogoUrl()).isEqualTo("https://vodafone.logo");
        assertThat(response.getPlans()).isEmpty();
    }

    @Test
    void testEqualsAndHashCode() {
        List<PlanResponse> plans = Collections.singletonList(
                PlanResponse.builder().id(1L).planName("Test").build()
        );
        
        OperatorDetectionResponse response1 = new OperatorDetectionResponse(1L, "Airtel", "AIRTEL", "url", plans);
        OperatorDetectionResponse response2 = new OperatorDetectionResponse(1L, "Airtel", "AIRTEL", "url", plans);
        OperatorDetectionResponse response3 = new OperatorDetectionResponse(2L, "Jio", "JIO", "url2", plans);

        assertThat(response1).isEqualTo(response2);
        assertThat(response1).isNotEqualTo(response3);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    void testToString() {
        OperatorDetectionResponse response = OperatorDetectionResponse.builder()
                .operatorId(1L)
                .operatorName("Airtel")
                .operatorCode("AIRTEL")
                .logoUrl("https://logo.url")
                .plans(Collections.emptyList())
                .build();
        
        String toString = response.toString();
        assertThat(toString).contains("operatorId=1");
        assertThat(toString).contains("operatorName=Airtel");
        assertThat(toString).contains("operatorCode=AIRTEL");
    }

    @Test
    void testWithEmptyPlans() {
        OperatorDetectionResponse response = OperatorDetectionResponse.builder()
                .operatorId(1L)
                .operatorName("Airtel")
                .plans(Collections.emptyList())
                .build();
        
        assertThat(response.getPlans()).isEmpty();
    }

    @Test
    void testWithMultiplePlans() {
        List<PlanResponse> plans = Arrays.asList(
                PlanResponse.builder().id(1L).planName("Plan 1").build(),
                PlanResponse.builder().id(2L).planName("Plan 2").build(),
                PlanResponse.builder().id(3L).planName("Plan 3").build()
        );
        
        OperatorDetectionResponse response = OperatorDetectionResponse.builder()
                .operatorId(1L)
                .plans(plans)
                .build();
        
        assertThat(response.getPlans()).hasSize(3);
    }
}

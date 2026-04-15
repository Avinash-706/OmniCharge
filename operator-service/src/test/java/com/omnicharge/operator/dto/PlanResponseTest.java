package com.omnicharge.operator.dto;

import com.omnicharge.operator.entity.PlanCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PlanResponseTest {

    @Test
    void testNoArgsConstructor() {
        PlanResponse response = new PlanResponse();
        assertThat(response).isNotNull();
    }

    @Test
    void testAllArgsConstructor() {
        PlanResponse response = new PlanResponse(
                1L, 10L, "Airtel", "Unlimited Plan", new BigDecimal("299.00"),
                28, "2GB/day", "Unlimited", "100 SMS/day", "Free subscription",
                PlanCategory.UNLIMITED, true
        );

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getOperatorId()).isEqualTo(10L);
        assertThat(response.getOperatorName()).isEqualTo("Airtel");
        assertThat(response.getPlanName()).isEqualTo("Unlimited Plan");
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("299.00"));
        assertThat(response.getValidityDays()).isEqualTo(28);
        assertThat(response.getDataLimit()).isEqualTo("2GB/day");
        assertThat(response.getCallBenefit()).isEqualTo("Unlimited");
        assertThat(response.getSmsBenefit()).isEqualTo("100 SMS/day");
        assertThat(response.getAdditionalBenefits()).isEqualTo("Free subscription");
        assertThat(response.getCategory()).isEqualTo(PlanCategory.UNLIMITED);
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    void testBuilder() {
        PlanResponse response = PlanResponse.builder()
                .id(5L)
                .operatorId(20L)
                .operatorName("Jio")
                .planName("Data Plan")
                .price(new BigDecimal("199.00"))
                .validityDays(30)
                .dataLimit("1.5GB/day")
                .callBenefit("Unlimited")
                .smsBenefit("100 SMS/day")
                .additionalBenefits("Free OTT")
                .category(PlanCategory.DATA)
                .isActive(false)
                .build();

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getOperatorId()).isEqualTo(20L);
        assertThat(response.getOperatorName()).isEqualTo("Jio");
        assertThat(response.getPlanName()).isEqualTo("Data Plan");
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("199.00"));
        assertThat(response.getValidityDays()).isEqualTo(30);
        assertThat(response.getDataLimit()).isEqualTo("1.5GB/day");
        assertThat(response.getCallBenefit()).isEqualTo("Unlimited");
        assertThat(response.getSmsBenefit()).isEqualTo("100 SMS/day");
        assertThat(response.getAdditionalBenefits()).isEqualTo("Free OTT");
        assertThat(response.getCategory()).isEqualTo(PlanCategory.DATA);
        assertThat(response.getIsActive()).isFalse();
    }

    @Test
    void testSettersAndGetters() {
        PlanResponse response = new PlanResponse();
        
        response.setId(15L);
        response.setOperatorId(30L);
        response.setOperatorName("Vodafone");
        response.setPlanName("Recommended Plan");
        response.setPrice(new BigDecimal("399.00"));
        response.setValidityDays(84);
        response.setDataLimit("3GB/day");
        response.setCallBenefit("Unlimited");
        response.setSmsBenefit("100 SMS/day");
        response.setAdditionalBenefits("Free roaming");
        response.setCategory(PlanCategory.RECOMMENDED);
        response.setIsActive(true);

        assertThat(response.getId()).isEqualTo(15L);
        assertThat(response.getOperatorId()).isEqualTo(30L);
        assertThat(response.getOperatorName()).isEqualTo("Vodafone");
        assertThat(response.getPlanName()).isEqualTo("Recommended Plan");
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("399.00"));
        assertThat(response.getValidityDays()).isEqualTo(84);
        assertThat(response.getDataLimit()).isEqualTo("3GB/day");
        assertThat(response.getCallBenefit()).isEqualTo("Unlimited");
        assertThat(response.getSmsBenefit()).isEqualTo("100 SMS/day");
        assertThat(response.getAdditionalBenefits()).isEqualTo("Free roaming");
        assertThat(response.getCategory()).isEqualTo(PlanCategory.RECOMMENDED);
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    void testEqualsAndHashCode() {
        PlanResponse response1 = new PlanResponse(1L, 10L, "Airtel", "Plan", new BigDecimal("299.00"), 28, "2GB", "Unlimited", "100 SMS", "Benefits", PlanCategory.UNLIMITED, true);
        PlanResponse response2 = new PlanResponse(1L, 10L, "Airtel", "Plan", new BigDecimal("299.00"), 28, "2GB", "Unlimited", "100 SMS", "Benefits", PlanCategory.UNLIMITED, true);
        PlanResponse response3 = new PlanResponse(2L, 20L, "Jio", "Other", new BigDecimal("199.00"), 30, "1GB", "Limited", "50 SMS", "None", PlanCategory.DATA, false);

        assertThat(response1).isEqualTo(response2);
        assertThat(response1).isNotEqualTo(response3);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    void testToString() {
        PlanResponse response = PlanResponse.builder()
                .id(1L)
                .operatorId(10L)
                .operatorName("Airtel")
                .planName("Unlimited Plan")
                .price(new BigDecimal("299.00"))
                .validityDays(28)
                .category(PlanCategory.UNLIMITED)
                .isActive(true)
                .build();
        
        String toString = response.toString();
        assertThat(toString).contains("id=1");
        assertThat(toString).contains("operatorId=10");
        assertThat(toString).contains("operatorName=Airtel");
        assertThat(toString).contains("planName=Unlimited Plan");
        assertThat(toString).contains("validityDays=28");
        assertThat(toString).contains("isActive=true");
    }

    @Test
    void testWithNullOptionalFields() {
        PlanResponse response = PlanResponse.builder()
                .id(1L)
                .operatorId(10L)
                .operatorName("Airtel")
                .planName("Basic Plan")
                .price(new BigDecimal("99.00"))
                .validityDays(7)
                .dataLimit(null)
                .callBenefit(null)
                .smsBenefit(null)
                .additionalBenefits(null)
                .category(PlanCategory.TALKTIME)
                .isActive(true)
                .build();
        
        assertThat(response.getDataLimit()).isNull();
        assertThat(response.getCallBenefit()).isNull();
        assertThat(response.getSmsBenefit()).isNull();
        assertThat(response.getAdditionalBenefits()).isNull();
    }

    @Test
    void testAllCategories() {
        for (PlanCategory category : PlanCategory.values()) {
            PlanResponse response = PlanResponse.builder()
                    .id(1L)
                    .planName("Test Plan")
                    .category(category)
                    .build();
            
            assertThat(response.getCategory()).isEqualTo(category);
        }
    }
}

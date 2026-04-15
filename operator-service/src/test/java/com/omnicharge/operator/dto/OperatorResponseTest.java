package com.omnicharge.operator.dto;

import com.omnicharge.operator.entity.OperatorCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperatorResponseTest {

    @Test
    void testNoArgsConstructor() {
        OperatorResponse response = new OperatorResponse();
        assertThat(response).isNotNull();
    }

    @Test
    void testAllArgsConstructor() {
        OperatorResponse response = new OperatorResponse(
                1L, "Airtel", "AIRTEL", OperatorCategory.PREPAID, "https://logo.url", true, 10
        );

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Airtel");
        assertThat(response.getCode()).isEqualTo("AIRTEL");
        assertThat(response.getCategory()).isEqualTo(OperatorCategory.PREPAID);
        assertThat(response.getLogoUrl()).isEqualTo("https://logo.url");
        assertThat(response.getIsActive()).isTrue();
        assertThat(response.getPlanCount()).isEqualTo(10);
    }

    @Test
    void testBuilder() {
        OperatorResponse response = OperatorResponse.builder()
                .id(5L)
                .name("Jio")
                .code("JIO")
                .category(OperatorCategory.POSTPAID)
                .logoUrl("https://jio.logo")
                .isActive(false)
                .planCount(25)
                .build();

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("Jio");
        assertThat(response.getCode()).isEqualTo("JIO");
        assertThat(response.getCategory()).isEqualTo(OperatorCategory.POSTPAID);
        assertThat(response.getLogoUrl()).isEqualTo("https://jio.logo");
        assertThat(response.getIsActive()).isFalse();
        assertThat(response.getPlanCount()).isEqualTo(25);
    }

    @Test
    void testSettersAndGetters() {
        OperatorResponse response = new OperatorResponse();
        
        response.setId(10L);
        response.setName("Vodafone");
        response.setCode("VODAFONE");
        response.setCategory(OperatorCategory.DTH);
        response.setLogoUrl("https://vodafone.logo");
        response.setIsActive(true);
        response.setPlanCount(15);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Vodafone");
        assertThat(response.getCode()).isEqualTo("VODAFONE");
        assertThat(response.getCategory()).isEqualTo(OperatorCategory.DTH);
        assertThat(response.getLogoUrl()).isEqualTo("https://vodafone.logo");
        assertThat(response.getIsActive()).isTrue();
        assertThat(response.getPlanCount()).isEqualTo(15);
    }

    @Test
    void testEqualsAndHashCode() {
        OperatorResponse response1 = new OperatorResponse(1L, "Airtel", "AIRTEL", OperatorCategory.PREPAID, "url", true, 10);
        OperatorResponse response2 = new OperatorResponse(1L, "Airtel", "AIRTEL", OperatorCategory.PREPAID, "url", true, 10);
        OperatorResponse response3 = new OperatorResponse(2L, "Jio", "JIO", OperatorCategory.POSTPAID, "url2", false, 20);

        assertThat(response1).isEqualTo(response2);
        assertThat(response1).isNotEqualTo(response3);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    void testToString() {
        OperatorResponse response = OperatorResponse.builder()
                .id(1L)
                .name("Airtel")
                .code("AIRTEL")
                .category(OperatorCategory.PREPAID)
                .isActive(true)
                .planCount(10)
                .build();
        
        String toString = response.toString();
        assertThat(toString).contains("id=1");
        assertThat(toString).contains("name=Airtel");
        assertThat(toString).contains("code=AIRTEL");
        assertThat(toString).contains("isActive=true");
        assertThat(toString).contains("planCount=10");
    }

    @Test
    void testWithZeroPlanCount() {
        OperatorResponse response = OperatorResponse.builder()
                .id(1L)
                .name("Airtel")
                .planCount(0)
                .build();
        
        assertThat(response.getPlanCount()).isZero();
    }

    @Test
    void testWithNullValues() {
        OperatorResponse response = OperatorResponse.builder()
                .id(null)
                .name(null)
                .code(null)
                .category(null)
                .logoUrl(null)
                .isActive(null)
                .planCount(null)
                .build();
        
        assertThat(response.getId()).isNull();
        assertThat(response.getName()).isNull();
        assertThat(response.getCode()).isNull();
        assertThat(response.getCategory()).isNull();
        assertThat(response.getLogoUrl()).isNull();
        assertThat(response.getIsActive()).isNull();
        assertThat(response.getPlanCount()).isNull();
    }
}

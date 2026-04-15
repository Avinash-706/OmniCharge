package com.omnicharge.operator.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NumverifyResponseTest {

    @Test
    void testNoArgsConstructor() {
        NumverifyResponse response = new NumverifyResponse();
        assertThat(response).isNotNull();
        assertThat(response.getValid()).isNull();
        assertThat(response.getNumber()).isNull();
        assertThat(response.getCountryCode()).isNull();
        assertThat(response.getCarrier()).isNull();
        assertThat(response.getLineType()).isNull();
    }

    @Test
    void testAllArgsConstructor() {
        NumverifyResponse response = new NumverifyResponse(
                true,
                "+919876543210",
                "IN",
                "Airtel",
                "mobile"
        );

        assertThat(response.getValid()).isTrue();
        assertThat(response.getNumber()).isEqualTo("+919876543210");
        assertThat(response.getCountryCode()).isEqualTo("IN");
        assertThat(response.getCarrier()).isEqualTo("Airtel");
        assertThat(response.getLineType()).isEqualTo("mobile");
    }

    @Test
    void testSettersAndGetters() {
        NumverifyResponse response = new NumverifyResponse();
        
        response.setValid(false);
        response.setNumber("+911234567890");
        response.setCountryCode("US");
        response.setCarrier("Verizon");
        response.setLineType("landline");

        assertThat(response.getValid()).isFalse();
        assertThat(response.getNumber()).isEqualTo("+911234567890");
        assertThat(response.getCountryCode()).isEqualTo("US");
        assertThat(response.getCarrier()).isEqualTo("Verizon");
        assertThat(response.getLineType()).isEqualTo("landline");
    }

    @Test
    void testEqualsAndHashCode() {
        NumverifyResponse response1 = new NumverifyResponse(true, "+919876543210", "IN", "Airtel", "mobile");
        NumverifyResponse response2 = new NumverifyResponse(true, "+919876543210", "IN", "Airtel", "mobile");
        NumverifyResponse response3 = new NumverifyResponse(false, "+911234567890", "US", "Verizon", "landline");

        assertThat(response1).isEqualTo(response2);
        assertThat(response1).isNotEqualTo(response3);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    void testToString() {
        NumverifyResponse response = new NumverifyResponse(true, "+919876543210", "IN", "Airtel", "mobile");
        String toString = response.toString();

        assertThat(toString).contains("valid=true");
        assertThat(toString).contains("number=+919876543210");
        assertThat(toString).contains("countryCode=IN");
        assertThat(toString).contains("carrier=Airtel");
        assertThat(toString).contains("lineType=mobile");
    }

    @Test
    void testNullValues() {
        NumverifyResponse response = new NumverifyResponse(null, null, null, null, null);
        
        assertThat(response.getValid()).isNull();
        assertThat(response.getNumber()).isNull();
        assertThat(response.getCountryCode()).isNull();
        assertThat(response.getCarrier()).isNull();
        assertThat(response.getLineType()).isNull();
    }
}

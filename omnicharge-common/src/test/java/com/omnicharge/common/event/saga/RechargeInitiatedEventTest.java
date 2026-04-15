package com.omnicharge.common.event.saga;

import org.junit.jupiter.api.Test;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class RechargeInitiatedEventTest {
    @Test
    void event_shouldImplementSerializable() {
        assertThat(Serializable.class).isAssignableFrom(RechargeInitiatedEvent.class);
    }

    @Test
    void builder_shouldCreateEvent() {
        RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                .rechargeId("RCH123").userId(1L).amount(new BigDecimal("99.99"))
                .paymentMethod("RAZORPAY").mobileNumber("+1234567890")
                .operatorName("Airtel").planName("Unlimited")
                .userEmail("user@test.com").userMobile("+9876543210")
                .timestamp(LocalDateTime.now()).build();
        assertThat(event.getRechargeId()).isEqualTo("RCH123");
        assertThat(event.getPaymentMethod()).isEqualTo("RAZORPAY");
    }
}

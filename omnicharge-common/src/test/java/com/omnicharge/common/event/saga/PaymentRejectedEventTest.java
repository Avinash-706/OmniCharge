package com.omnicharge.common.event.saga;

import org.junit.jupiter.api.Test;
import java.io.Serializable;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentRejectedEventTest {
    @Test
    void event_shouldImplementSerializable() {
        assertThat(Serializable.class).isAssignableFrom(PaymentRejectedEvent.class);
    }

    @Test
    void builder_shouldCreateEvent() {
        PaymentRejectedEvent event = PaymentRejectedEvent.builder()
                .rechargeId("RCH123").failureReason("Insufficient funds")
                .timestamp(LocalDateTime.now()).build();
        assertThat(event.getRechargeId()).isEqualTo("RCH123");
        assertThat(event.getFailureReason()).isEqualTo("Insufficient funds");
    }
}

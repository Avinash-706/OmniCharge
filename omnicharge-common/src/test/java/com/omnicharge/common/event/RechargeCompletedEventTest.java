package com.omnicharge.common.event;

import org.junit.jupiter.api.Test;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class RechargeCompletedEventTest {
    @Test
    void event_shouldImplementSerializable() {
        assertThat(Serializable.class).isAssignableFrom(RechargeCompletedEvent.class);
    }

    @Test
    void builder_shouldCreateEvent() {
        RechargeCompletedEvent event = RechargeCompletedEvent.builder()
                .rechargeId("RCH123").userId(1L).amount(new BigDecimal("99.99"))
                .status("COMPLETED").build();
        assertThat(event.getRechargeId()).isEqualTo("RCH123");
        assertThat(event.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
    }
}

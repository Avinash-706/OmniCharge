package com.omnicharge.common.event;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PlanExpiryEventTest {

    @Test
    void event_shouldImplementSerializable() {
        assertThat(Serializable.class).isAssignableFrom(PlanExpiryEvent.class);
    }

    @Test
    void allArgsConstructor_shouldCreateEventWithAllFields() {
        LocalDate expiryDate = LocalDate.of(2024, 12, 31);
        LocalDateTime timestamp = LocalDateTime.now();
        
        PlanExpiryEvent event = new PlanExpiryEvent(
                "RCH123", 1L, "user@test.com", "+1234567890",
                "+9876543210", "Airtel", "Monthly", new BigDecimal("99.99"),
                expiryDate, "REMINDER", timestamp
        );

        assertThat(event.getRechargeId()).isEqualTo("RCH123");
        assertThat(event.getUserId()).isEqualTo(1L);
        assertThat(event.getExpiryDate()).isEqualTo(expiryDate);
        assertThat(event.getCategory()).isEqualTo("REMINDER");
    }
}

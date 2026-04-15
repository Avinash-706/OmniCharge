package com.omnicharge.common.event.saga;

import org.junit.jupiter.api.Test;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentApprovedEventTest {
    @Test
    void event_shouldImplementSerializable() {
        assertThat(Serializable.class).isAssignableFrom(PaymentApprovedEvent.class);
    }

    @Test
    void builder_shouldCreateEvent() {
        PaymentApprovedEvent event = PaymentApprovedEvent.builder()
                .rechargeId("RCH123").transactionId("TXN456")
                .razorpayPaymentId("pay_123").razorpayOrderId("order_456")
                .amount(new BigDecimal("99.99")).status("APPROVED")
                .timestamp(LocalDateTime.now()).build();
        assertThat(event.getRechargeId()).isEqualTo("RCH123");
        assertThat(event.getStatus()).isEqualTo("APPROVED");
    }
}

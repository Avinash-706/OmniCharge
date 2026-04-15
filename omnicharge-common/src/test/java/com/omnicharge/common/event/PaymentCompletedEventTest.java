package com.omnicharge.common.event;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PaymentCompletedEvent.
 */
class PaymentCompletedEventTest {

    @Test
    void event_shouldImplementSerializable() {
        assertThat(Serializable.class).isAssignableFrom(PaymentCompletedEvent.class);
    }

    @Test
    void builder_shouldCreateEventWithAllFields() {
        // Arrange & Act
        LocalDateTime now = LocalDateTime.now();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .transactionId("TXN123")
                .rechargeId("RCH456")
                .userId(1L)
                .userEmail("user@test.com")
                .userMobile("+1234567890")
                .mobileNumber("+9876543210")
                .operatorName("Airtel")
                .planName("Unlimited")
                .amount(new BigDecimal("99.99"))
                .status("COMPLETED")
                .paymentMethod("RAZORPAY")
                .timestamp(now)
                .build();

        // Assert
        assertThat(event.getTransactionId()).isEqualTo("TXN123");
        assertThat(event.getRechargeId()).isEqualTo("RCH456");
        assertThat(event.getUserId()).isEqualTo(1L);
        assertThat(event.getUserEmail()).isEqualTo("user@test.com");
        assertThat(event.getUserMobile()).isEqualTo("+1234567890");
        assertThat(event.getMobileNumber()).isEqualTo("+9876543210");
        assertThat(event.getOperatorName()).isEqualTo("Airtel");
        assertThat(event.getPlanName()).isEqualTo("Unlimited");
        assertThat(event.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(event.getStatus()).isEqualTo("COMPLETED");
        assertThat(event.getPaymentMethod()).isEqualTo("RAZORPAY");
        assertThat(event.getTimestamp()).isEqualTo(now);
    }

    @Test
    void setters_shouldUpdateFields() {
        // Arrange
        PaymentCompletedEvent event = new PaymentCompletedEvent();

        // Act
        event.setTransactionId("TXN789");
        event.setAmount(new BigDecimal("50.00"));
        event.setStatus("PENDING");

        // Assert
        assertThat(event.getTransactionId()).isEqualTo("TXN789");
        assertThat(event.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(event.getStatus()).isEqualTo("PENDING");
    }
}

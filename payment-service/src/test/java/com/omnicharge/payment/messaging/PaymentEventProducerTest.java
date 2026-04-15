package com.omnicharge.payment.messaging;

import com.omnicharge.common.event.PaymentCompletedEvent;
import com.omnicharge.common.event.saga.PaymentApprovedEvent;
import com.omnicharge.common.event.saga.PaymentRejectedEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private LogEventPublisher logEventPublisher;

    private PaymentEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new PaymentEventProducer(rabbitTemplate, logEventPublisher);
    }

    @Test
    void testPublishPaymentCompleted_ShouldSendToRabbitMQ() {
        // Arrange
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .transactionId("TXN123")
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        producer.publishPaymentCompleted(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq("omnicharge.exchange"),
                eq("payment.completed"),
                eq(event)
        );
        verify(logEventPublisher).publish(any());
    }

    @Test
    void testPublishPaymentCompleted_RabbitMQFailure_ShouldNotThrowException() {
        // Arrange
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .transactionId("TXN123")
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();

        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // Act - Should not throw exception
        producer.publishPaymentCompleted(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void testPublishPaymentApproved_ShouldSendToRabbitMQ() {
        // Arrange
        PaymentApprovedEvent event = PaymentApprovedEvent.builder()
                .rechargeId("RECH123")
                .transactionId("TXN123")
                .razorpayOrderId("order_123")
                .amount(new BigDecimal("100.00"))
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        producer.publishPaymentApproved(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq("omnicharge.exchange"),
                eq("saga.payment.approved"),
                eq(event)
        );
        verify(logEventPublisher).publish(any());
    }

    @Test
    void testPublishPaymentApproved_RabbitMQFailure_ShouldNotThrowException() {
        // Arrange
        PaymentApprovedEvent event = PaymentApprovedEvent.builder()
                .rechargeId("RECH123")
                .transactionId("TXN123")
                .razorpayOrderId("order_123")
                .amount(new BigDecimal("100.00"))
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();

        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // Act - Should not throw exception
        producer.publishPaymentApproved(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void testPublishPaymentRejected_ShouldSendToRabbitMQ() {
        // Arrange
        PaymentRejectedEvent event = PaymentRejectedEvent.builder()
                .rechargeId("RECH123")
                .failureReason("Payment gateway timeout")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        producer.publishPaymentRejected(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq("omnicharge.exchange"),
                eq("saga.payment.rejected"),
                eq(event)
        );
        verify(logEventPublisher).publish(any());
    }

    @Test
    void testPublishPaymentRejected_RabbitMQFailure_ShouldNotThrowException() {
        // Arrange
        PaymentRejectedEvent event = PaymentRejectedEvent.builder()
                .rechargeId("RECH123")
                .failureReason("Payment gateway timeout")
                .timestamp(LocalDateTime.now())
                .build();

        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // Act - Should not throw exception
        producer.publishPaymentRejected(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void testPublishPaymentCompleted_ShouldLogEvent() {
        // Arrange
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .transactionId("TXN123")
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        producer.publishPaymentCompleted(event);

        // Assert
        verify(logEventPublisher).publish(any());
    }

    @Test
    void testPublishPaymentApproved_ShouldLogEvent() {
        // Arrange
        PaymentApprovedEvent event = PaymentApprovedEvent.builder()
                .rechargeId("RECH123")
                .transactionId("TXN123")
                .razorpayOrderId("order_123")
                .amount(new BigDecimal("100.00"))
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        producer.publishPaymentApproved(event);

        // Assert
        verify(logEventPublisher).publish(any());
    }

    @Test
    void testPublishPaymentRejected_ShouldLogEvent() {
        // Arrange
        PaymentRejectedEvent event = PaymentRejectedEvent.builder()
                .rechargeId("RECH123")
                .failureReason("Payment gateway timeout")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        producer.publishPaymentRejected(event);

        // Assert
        verify(logEventPublisher).publish(any());
    }

    @Test
    void testPublishPaymentCompleted_WithAllFields_ShouldSendCorrectly() {
        // Arrange
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .transactionId("TXN123")
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("250.50"))
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        producer.publishPaymentCompleted(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq("omnicharge.exchange"),
                eq("payment.completed"),
                eq(event)
        );
    }

    @Test
    void testPublishPaymentApproved_WithAllFields_ShouldSendCorrectly() {
        // Arrange
        PaymentApprovedEvent event = PaymentApprovedEvent.builder()
                .rechargeId("RECH123")
                .transactionId("TXN123")
                .razorpayOrderId("order_123")
                .amount(new BigDecimal("250.50"))
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        producer.publishPaymentApproved(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq("omnicharge.exchange"),
                eq("saga.payment.approved"),
                eq(event)
        );
    }

    @Test
    void testPublishPaymentRejected_WithFailureReason_ShouldSendCorrectly() {
        // Arrange
        PaymentRejectedEvent event = PaymentRejectedEvent.builder()
                .rechargeId("RECH123")
                .failureReason("Insufficient funds")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        producer.publishPaymentRejected(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq("omnicharge.exchange"),
                eq("saga.payment.rejected"),
                eq(event)
        );
    }
}

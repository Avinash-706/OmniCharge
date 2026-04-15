package com.omnicharge.payment.consumer;

import com.omnicharge.common.event.saga.PaymentApprovedEvent;
import com.omnicharge.common.event.saga.PaymentRejectedEvent;
import com.omnicharge.common.event.saga.RechargeInitiatedEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;
import com.omnicharge.payment.messaging.PaymentEventProducer;
import com.omnicharge.payment.service.IPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentSagaConsumerTest {

    @Mock
    private IPaymentService paymentService;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private LogEventPublisher logEventPublisher;

    @Captor
    private ArgumentCaptor<PaymentRequest> paymentRequestCaptor;

    @Captor
    private ArgumentCaptor<PaymentApprovedEvent> approvedEventCaptor;

    @Captor
    private ArgumentCaptor<PaymentRejectedEvent> rejectedEventCaptor;

    private PaymentSagaConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PaymentSagaConsumer(paymentService, paymentEventProducer, logEventPublisher);
    }

    @Test
    void testConsumeRechargeInitiatedEvent_Success_ShouldPublishApprovedEvent() {
        // Arrange
        RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("UPI")
                .userEmail("user@test.com")
                .userMobile("9876543210")
                .mobileNumber("9876543210")
                .operatorName("Airtel")
                .planName("Unlimited")
                .timestamp(LocalDateTime.now())
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .transactionId("TXN123")
                .status("SUCCESS")
                .razorpayOrderId("order_123")
                .amount(new BigDecimal("100.00"))
                .timestamp(LocalDateTime.now())
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(response);

        // Act
        consumer.consumeRechargeInitiatedEvent(event);

        // Assert
        verify(paymentService).processPayment(paymentRequestCaptor.capture());
        PaymentRequest capturedRequest = paymentRequestCaptor.getValue();
        assertThat(capturedRequest.getRechargeId()).isEqualTo("RECH123");
        assertThat(capturedRequest.getUserId()).isEqualTo(1L);
        assertThat(capturedRequest.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(capturedRequest.getUserEmail()).isEqualTo("user@test.com");
        assertThat(capturedRequest.getOperatorName()).isEqualTo("Airtel");

        verify(paymentEventProducer).publishPaymentApproved(approvedEventCaptor.capture());
        PaymentApprovedEvent approvedEvent = approvedEventCaptor.getValue();
        assertThat(approvedEvent.getRechargeId()).isEqualTo("RECH123");
        assertThat(approvedEvent.getTransactionId()).isEqualTo("TXN123");
        assertThat(approvedEvent.getStatus()).isEqualTo("SUCCESS");

        verify(paymentEventProducer, never()).publishPaymentRejected(any());
        verify(logEventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void testConsumeRechargeInitiatedEvent_Pending_ShouldNotPublishEvents() {
        // Arrange
        RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("UPI")
                .userEmail("user@test.com")
                .userMobile("9876543210")
                .mobileNumber("9876543210")
                .operatorName("Airtel")
                .planName("Unlimited")
                .timestamp(LocalDateTime.now())
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .transactionId("TXN123")
                .status("PENDING")
                .razorpayOrderId("order_123")
                .amount(new BigDecimal("100.00"))
                .timestamp(LocalDateTime.now())
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(response);

        // Act
        consumer.consumeRechargeInitiatedEvent(event);

        // Assert
        verify(paymentService).processPayment(any(PaymentRequest.class));
        verify(paymentEventProducer, never()).publishPaymentApproved(any());
        verify(paymentEventProducer, never()).publishPaymentRejected(any());
        verify(logEventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void testConsumeRechargeInitiatedEvent_Failed_ShouldPublishRejectedEvent() {
        // Arrange
        RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("UPI")
                .userEmail("user@test.com")
                .userMobile("9876543210")
                .mobileNumber("9876543210")
                .operatorName("Airtel")
                .planName("Unlimited")
                .timestamp(LocalDateTime.now())
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .transactionId("TXN123")
                .status("FAILED")
                .amount(new BigDecimal("100.00"))
                .timestamp(LocalDateTime.now())
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(response);

        // Act
        consumer.consumeRechargeInitiatedEvent(event);

        // Assert
        verify(paymentService).processPayment(any(PaymentRequest.class));
        verify(paymentEventProducer).publishPaymentRejected(rejectedEventCaptor.capture());
        PaymentRejectedEvent rejectedEvent = rejectedEventCaptor.getValue();
        assertThat(rejectedEvent.getRechargeId()).isEqualTo("RECH123");
        assertThat(rejectedEvent.getFailureReason()).isEqualTo("Payment creation failed via Razorpay");

        verify(paymentEventProducer, never()).publishPaymentApproved(any());
        verify(logEventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void testConsumeRechargeInitiatedEvent_Exception_ShouldPublishRejectedEvent() {
        // Arrange
        RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("UPI")
                .userEmail("user@test.com")
                .userMobile("9876543210")
                .mobileNumber("9876543210")
                .operatorName("Airtel")
                .planName("Unlimited")
                .timestamp(LocalDateTime.now())
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act
        consumer.consumeRechargeInitiatedEvent(event);

        // Assert
        verify(paymentService).processPayment(any(PaymentRequest.class));
        verify(paymentEventProducer).publishPaymentRejected(rejectedEventCaptor.capture());
        PaymentRejectedEvent rejectedEvent = rejectedEventCaptor.getValue();
        assertThat(rejectedEvent.getRechargeId()).isEqualTo("RECH123");
        assertThat(rejectedEvent.getFailureReason()).isEqualTo("Database connection failed");

        verify(paymentEventProducer, never()).publishPaymentApproved(any());
        verify(logEventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void testConsumeRechargeInitiatedEvent_ShouldMapAllFields() {
        // Arrange
        RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CARD")
                .userEmail("user@test.com")
                .userMobile("9876543210")
                .mobileNumber("9123456789")
                .operatorName("Jio")
                .planName("Premium")
                .timestamp(LocalDateTime.now())
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .transactionId("TXN123")
                .status("SUCCESS")
                .razorpayOrderId("order_123")
                .amount(new BigDecimal("100.00"))
                .timestamp(LocalDateTime.now())
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(response);

        // Act
        consumer.consumeRechargeInitiatedEvent(event);

        // Assert
        verify(paymentService).processPayment(paymentRequestCaptor.capture());
        PaymentRequest capturedRequest = paymentRequestCaptor.getValue();
        assertThat(capturedRequest.getRechargeId()).isEqualTo("RECH123");
        assertThat(capturedRequest.getUserId()).isEqualTo(1L);
        assertThat(capturedRequest.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(capturedRequest.getPaymentMethod()).isEqualTo("CARD");
        assertThat(capturedRequest.getUserEmail()).isEqualTo("user@test.com");
        assertThat(capturedRequest.getUserMobile()).isEqualTo("9876543210");
        assertThat(capturedRequest.getMobileNumber()).isEqualTo("9123456789");
        assertThat(capturedRequest.getOperatorName()).isEqualTo("Jio");
        assertThat(capturedRequest.getPlanName()).isEqualTo("Premium");
    }

    @Test
    void testConsumeRechargeInitiatedEvent_Success_ShouldLogSagaEvents() {
        // Arrange
        RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("UPI")
                .userEmail("user@test.com")
                .userMobile("9876543210")
                .mobileNumber("9876543210")
                .operatorName("Airtel")
                .planName("Unlimited")
                .timestamp(LocalDateTime.now())
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .transactionId("TXN123")
                .status("SUCCESS")
                .razorpayOrderId("order_123")
                .amount(new BigDecimal("100.00"))
                .timestamp(LocalDateTime.now())
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(response);

        // Act
        consumer.consumeRechargeInitiatedEvent(event);

        // Assert
        verify(logEventPublisher, times(2)).publish(any()); // SAGA_EVENT_CONSUMED + SAGA_PROCESSING_SUCCESS
    }

    @Test
    void testConsumeRechargeInitiatedEvent_Pending_ShouldLogPendingEvent() {
        // Arrange
        RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("UPI")
                .userEmail("user@test.com")
                .userMobile("9876543210")
                .mobileNumber("9876543210")
                .operatorName("Airtel")
                .planName("Unlimited")
                .timestamp(LocalDateTime.now())
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .transactionId("TXN123")
                .status("PENDING")
                .razorpayOrderId("order_123")
                .amount(new BigDecimal("100.00"))
                .timestamp(LocalDateTime.now())
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(response);

        // Act
        consumer.consumeRechargeInitiatedEvent(event);

        // Assert
        verify(logEventPublisher, times(2)).publish(any()); // SAGA_EVENT_CONSUMED + SAGA_PROCESSING_PENDING
    }

    @Test
    void testConsumeRechargeInitiatedEvent_Failed_ShouldLogFailureEvent() {
        // Arrange
        RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("UPI")
                .userEmail("user@test.com")
                .userMobile("9876543210")
                .mobileNumber("9876543210")
                .operatorName("Airtel")
                .planName("Unlimited")
                .timestamp(LocalDateTime.now())
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .transactionId("TXN123")
                .status("FAILED")
                .amount(new BigDecimal("100.00"))
                .timestamp(LocalDateTime.now())
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(response);

        // Act
        consumer.consumeRechargeInitiatedEvent(event);

        // Assert
        verify(logEventPublisher, times(2)).publish(any()); // SAGA_EVENT_CONSUMED + SAGA_PROCESSING_FAILED
    }

    @Test
    void testConsumeRechargeInitiatedEvent_Exception_ShouldLogExceptionEvent() {
        // Arrange
        RechargeInitiatedEvent event = RechargeInitiatedEvent.builder()
                .rechargeId("RECH123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("UPI")
                .userEmail("user@test.com")
                .userMobile("9876543210")
                .mobileNumber("9876543210")
                .operatorName("Airtel")
                .planName("Unlimited")
                .timestamp(LocalDateTime.now())
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act
        consumer.consumeRechargeInitiatedEvent(event);

        // Assert
        verify(logEventPublisher, times(2)).publish(any()); // SAGA_EVENT_CONSUMED + SAGA_PROCESSING_EXCEPTION
    }
}

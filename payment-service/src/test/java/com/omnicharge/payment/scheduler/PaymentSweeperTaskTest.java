package com.omnicharge.payment.scheduler;

import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.payment.entity.PaymentMethod;
import com.omnicharge.payment.entity.PaymentStatus;
import com.omnicharge.payment.entity.Transaction;
import com.omnicharge.payment.messaging.PaymentEventProducer;
import com.omnicharge.payment.repository.TransactionRepository;
import com.omnicharge.payment.service.IPaymentService;
import com.omnicharge.payment.service.IRazorpayPaymentService;
import com.razorpay.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentSweeperTaskTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private LogEventPublisher logEventPublisher;

    @Mock
    private IPaymentService paymentService;

    @Mock
    private IRazorpayPaymentService razorpayPaymentService;

    @Mock
    private Order razorpayOrder;

    private PaymentSweeperTask sweeperTask;

    @BeforeEach
    void setUp() {
        sweeperTask = new PaymentSweeperTask(
                transactionRepository,
                paymentEventProducer,
                logEventPublisher,
                paymentService,
                razorpayPaymentService
        );
    }

    @Test
    void testSweepZombieTransactions_NoZombies_ShouldReturnSilently() {
        // Arrange
        when(transactionRepository.findByStatusAndCreatedDateBefore(any(PaymentStatus.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Act
        sweeperTask.sweepZombieTransactions();

        // Assert
        verify(transactionRepository).findByStatusAndCreatedDateBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class));
        verify(paymentService, never()).failPayment(anyString(), anyString());
        verify(paymentService, never()).confirmPayment(anyString(), anyString(), anyString());
    }

    @Test
    void testSweepZombieTransactions_WithZombiesNoRazorpayOrderId_ShouldFailPayment() throws Exception {
        // Arrange
        Transaction zombie = createZombieTransaction("TXN123", null);
        when(transactionRepository.findByStatusAndCreatedDateBefore(any(PaymentStatus.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(zombie));

        // Act
        sweeperTask.sweepZombieTransactions();

        // Assert
        verify(transactionRepository).findByStatusAndCreatedDateBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class));
        verify(paymentService).failPayment("TXN123", "Payment session expired. Please try again.");
        verify(razorpayPaymentService, never()).fetchOrder(anyString());
    }

    @Test
    void testSweepZombieTransactions_WithRazorpayOrderPaid_ShouldConfirmPayment() throws Exception {
        // Arrange
        Transaction zombie = createZombieTransaction("TXN123", "order_123");
        when(transactionRepository.findByStatusAndCreatedDateBefore(any(PaymentStatus.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(zombie));
        doReturn(razorpayOrder).when(razorpayPaymentService).fetchOrder("order_123");
        when(razorpayOrder.get("status")).thenReturn("paid");
        doReturn("pay_123").when(razorpayPaymentService).fetchPaymentIdForOrder("order_123");

        // Act
        sweeperTask.sweepZombieTransactions();

        // Assert
        verify(razorpayPaymentService).fetchOrder("order_123");
        verify(razorpayPaymentService).fetchPaymentIdForOrder("order_123");
        verify(paymentService).confirmPayment("TXN123", "pay_123", "SYSTEM_AUTO_RECOVERY");
        verify(paymentService, never()).failPayment(anyString(), anyString());
    }

    @Test
    void testSweepZombieTransactions_WithRazorpayOrderCaptured_ShouldConfirmPayment() throws Exception {
        // Arrange
        Transaction zombie = createZombieTransaction("TXN123", "order_123");
        when(transactionRepository.findByStatusAndCreatedDateBefore(any(PaymentStatus.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(zombie));
        doReturn(razorpayOrder).when(razorpayPaymentService).fetchOrder("order_123");
        when(razorpayOrder.get("status")).thenReturn("captured");
        doReturn("pay_456").when(razorpayPaymentService).fetchPaymentIdForOrder("order_123");

        // Act
        sweeperTask.sweepZombieTransactions();

        // Assert
        verify(razorpayPaymentService).fetchOrder("order_123");
        verify(paymentService).confirmPayment("TXN123", "pay_456", "SYSTEM_AUTO_RECOVERY");
        verify(paymentService, never()).failPayment(anyString(), anyString());
    }

    @Test
    void testSweepZombieTransactions_WithRazorpayOrderPaidButNoPaymentId_ShouldUseRecoveredId() throws Exception {
        // Arrange
        Transaction zombie = createZombieTransaction("TXN123", "order_123");
        when(transactionRepository.findByStatusAndCreatedDateBefore(any(PaymentStatus.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(zombie));
        doReturn(razorpayOrder).when(razorpayPaymentService).fetchOrder("order_123");
        when(razorpayOrder.get("status")).thenReturn("paid");
        doReturn(null).when(razorpayPaymentService).fetchPaymentIdForOrder("order_123");

        // Act
        sweeperTask.sweepZombieTransactions();

        // Assert
        verify(razorpayPaymentService).fetchOrder("order_123");
        verify(paymentService).confirmPayment("TXN123", "RECOVERED_order_123", "SYSTEM_AUTO_RECOVERY");
    }

    @Test
    void testSweepZombieTransactions_WithRazorpayOrderCreated_ShouldFailPayment() throws Exception {
        // Arrange
        Transaction zombie = createZombieTransaction("TXN123", "order_123");
        when(transactionRepository.findByStatusAndCreatedDateBefore(any(PaymentStatus.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(zombie));
        doReturn(razorpayOrder).when(razorpayPaymentService).fetchOrder("order_123");
        when(razorpayOrder.get("status")).thenReturn("created");

        // Act
        sweeperTask.sweepZombieTransactions();

        // Assert
        verify(razorpayPaymentService).fetchOrder("order_123");
        verify(paymentService).failPayment("TXN123", "Payment session expired. Please try again.");
        verify(paymentService, never()).confirmPayment(anyString(), anyString(), anyString());
    }

    @Test
    void testSweepZombieTransactions_WithRazorpayOrderAttempted_ShouldFailPayment() throws Exception {
        // Arrange
        Transaction zombie = createZombieTransaction("TXN123", "order_123");
        when(transactionRepository.findByStatusAndCreatedDateBefore(any(PaymentStatus.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(zombie));
        doReturn(razorpayOrder).when(razorpayPaymentService).fetchOrder("order_123");
        when(razorpayOrder.get("status")).thenReturn("attempted");

        // Act
        sweeperTask.sweepZombieTransactions();

        // Assert
        verify(razorpayPaymentService).fetchOrder("order_123");
        verify(paymentService).failPayment("TXN123", "Payment session expired. Please try again.");
    }

    @Test
    void testSweepZombieTransactions_RazorpayApiFails_ShouldFallbackToFailPayment() throws Exception {
        // Arrange
        Transaction zombie = createZombieTransaction("TXN123", "order_123");
        when(transactionRepository.findByStatusAndCreatedDateBefore(any(PaymentStatus.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(zombie));
        doThrow(new RuntimeException("Razorpay API error")).when(razorpayPaymentService).fetchOrder("order_123");

        // Act
        sweeperTask.sweepZombieTransactions();

        // Assert
        verify(razorpayPaymentService).fetchOrder("order_123");
        verify(paymentService).failPayment("TXN123", "Payment gateway timeout. Please try again.");
    }

    @Test
    void testSweepZombieTransactions_FailPaymentThrowsException_ShouldContinue() throws Exception {
        // Arrange
        Transaction zombie = createZombieTransaction("TXN123", "order_123");
        when(transactionRepository.findByStatusAndCreatedDateBefore(any(PaymentStatus.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(zombie));
        doThrow(new RuntimeException("Razorpay API error")).when(razorpayPaymentService).fetchOrder("order_123");
        doThrow(new RuntimeException("Database error")).when(paymentService).failPayment(anyString(), anyString());

        // Act
        sweeperTask.sweepZombieTransactions();

        // Assert - Should not throw exception, just log error
        verify(paymentService).failPayment("TXN123", "Payment gateway timeout. Please try again.");
    }

    @Test
    void testSweepZombieTransactions_MultipleZombies_ShouldProcessAll() throws Exception {
        // Arrange
        Transaction zombie1 = createZombieTransaction("TXN123", "order_123");
        Transaction zombie2 = createZombieTransaction("TXN456", null);
        Transaction zombie3 = createZombieTransaction("TXN789", "order_789");
        
        when(transactionRepository.findByStatusAndCreatedDateBefore(any(PaymentStatus.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(zombie1, zombie2, zombie3));
        
        // Create separate mock orders for different order IDs
        Order razorpayOrder123 = mock(Order.class);
        Order razorpayOrder789 = mock(Order.class);
        
        doReturn(razorpayOrder123).when(razorpayPaymentService).fetchOrder("order_123");
        when(razorpayOrder123.get("status")).thenReturn("paid");
        doReturn("pay_123").when(razorpayPaymentService).fetchPaymentIdForOrder("order_123");
        
        doReturn(razorpayOrder789).when(razorpayPaymentService).fetchOrder("order_789");
        when(razorpayOrder789.get("status")).thenReturn("created");

        // Act
        sweeperTask.sweepZombieTransactions();

        // Assert
        verify(paymentService).confirmPayment("TXN123", "pay_123", "SYSTEM_AUTO_RECOVERY");
        verify(paymentService).failPayment("TXN456", "Payment session expired. Please try again.");
        verify(paymentService).failPayment("TXN789", "Payment session expired. Please try again.");
    }

    @Test
    void testSweepZombieTransactions_ChecksCutoffTime() {
        // Arrange
        when(transactionRepository.findByStatusAndCreatedDateBefore(any(PaymentStatus.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Act
        sweeperTask.sweepZombieTransactions();

        // Assert - Verify cutoff time is approximately 2 minutes ago (TIMEOUT_MINUTES = 2)
        verify(transactionRepository).findByStatusAndCreatedDateBefore(
                eq(PaymentStatus.PENDING), 
                argThat(cutoff -> {
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime expectedCutoff = now.minusMinutes(2);
                    // Allow 1 second tolerance for test execution time
                    return cutoff.isAfter(expectedCutoff.minusSeconds(1)) && 
                           cutoff.isBefore(expectedCutoff.plusSeconds(1));
                })
        );
    }

    private Transaction createZombieTransaction(String transactionId, String razorpayOrderId) {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setTransactionId(transactionId);
        transaction.setRechargeId("RECH123");
        transaction.setUserId(1L);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setPaymentMethod(PaymentMethod.UPI);
        transaction.setStatus(PaymentStatus.PENDING);
        transaction.setRazorpayOrderId(razorpayOrderId);
        return transaction;
    }
}

package com.omnicharge.payment.service;

import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.payment.dto.PaymentRequest;
import com.omnicharge.payment.dto.PaymentResponse;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.Payment;
import com.razorpay.PaymentClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RazorpayPaymentServiceTest {

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private OrderClient orderClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private RazorpayPaymentService razorpayPaymentService;

    @BeforeEach
    void setUp() {
        // Set up the razorpayClient to return mocked clients
        razorpayClient.orders = orderClient;
        razorpayClient.payments = paymentClient;
    }

    @Test
    void processRazorpayPayment_Success() throws RazorpayException {
        PaymentRequest request = new PaymentRequest();
        request.setRechargeId("OMNI-A1");
        request.setAmount(new BigDecimal("499.00"));

        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_123");
        when(orderClient.create(any(JSONObject.class))).thenReturn(mockOrder);

        PaymentResponse response = razorpayPaymentService.processRazorpayPayment(request);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        assertEquals("order_123", response.getRazorpayOrderId());
        assertTrue(response.getTransactionId().startsWith("TXN-"));
        assertEquals(new BigDecimal("499.00"), response.getAmount());
        verify(orderClient, times(1)).create(any(JSONObject.class));
    }

    @Test
    void processRazorpayPayment_Failure() throws RazorpayException {
        PaymentRequest request = new PaymentRequest();
        request.setRechargeId("OMNI-A2");
        request.setAmount(new BigDecimal("299.00"));

        when(orderClient.create(any(JSONObject.class)))
                .thenThrow(new RazorpayException("API Error"));

        PaymentResponse response = razorpayPaymentService.processRazorpayPayment(request);

        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        assertNull(response.getRazorpayOrderId());
        assertEquals(new BigDecimal("299.00"), response.getAmount());
    }

    @Test
    void processPaymentFallback_TriggeredOnCircuitBreak() {
        PaymentRequest request = new PaymentRequest();
        request.setRechargeId("OMNI-A2");
        request.setAmount(new BigDecimal("199.00"));

        RuntimeException mockException = new RuntimeException("API Timeout");
        PaymentResponse response = razorpayPaymentService.processPaymentFallback(request, mockException);

        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        assertNull(response.getRazorpayOrderId());
        assertEquals(new BigDecimal("199.00"), response.getAmount());
    }

    @Test
    void processRefund_Success() throws RazorpayException {
        String paymentId = "pay_123";
        BigDecimal amount = new BigDecimal("100.00");

        // PaymentClient.refund returns a Refund object, not void
        when(paymentClient.refund(anyString(), any(JSONObject.class))).thenReturn(null);

        assertDoesNotThrow(() -> {
            razorpayPaymentService.processRefund(paymentId, amount);
        });

        verify(paymentClient, times(1)).refund(eq(paymentId), any(JSONObject.class));
    }

    @Test
    void processRefund_Failure() throws RazorpayException {
        String paymentId = "pay_456";
        BigDecimal amount = new BigDecimal("50.00");

        when(paymentClient.refund(anyString(), any(JSONObject.class)))
                .thenThrow(new RazorpayException("Refund failed"));

        assertDoesNotThrow(() -> {
            razorpayPaymentService.processRefund(paymentId, amount);
        });

        verify(paymentClient, times(1)).refund(eq(paymentId), any(JSONObject.class));
    }

    @Test
    void fetchOrder_Success() throws RazorpayException {
        String orderId = "order_test123";
        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn(orderId);
        when(mockOrder.get("status")).thenReturn("created");
        when(orderClient.fetch(orderId)).thenReturn(mockOrder);

        Order result = razorpayPaymentService.fetchOrder(orderId);

        assertNotNull(result);
        assertEquals(orderId, result.get("id"));
        assertEquals("created", result.get("status"));
        verify(orderClient, times(1)).fetch(orderId);
    }

    @Test
    void fetchOrder_ThrowsException() throws RazorpayException {
        String orderId = "order_invalid";
        when(orderClient.fetch(orderId)).thenThrow(new RazorpayException("Order not found"));

        assertThrows(RazorpayException.class, () -> {
            razorpayPaymentService.fetchOrder(orderId);
        });
    }

    @Test
    void fetchPaymentIdForOrder_ReturnsCapturedPayment() throws RazorpayException {
        String orderId = "order_test123";
        String expectedPaymentId = "pay_captured123";

        Payment mockPayment = mock(Payment.class);
        when(mockPayment.get("status")).thenReturn("captured");
        when(mockPayment.get("id")).thenReturn(expectedPaymentId);

        List<Payment> paymentsList = new ArrayList<>();
        paymentsList.add(mockPayment);

        when(orderClient.fetchPayments(orderId)).thenReturn(paymentsList);

        String result = razorpayPaymentService.fetchPaymentIdForOrder(orderId);

        assertEquals(expectedPaymentId, result);
        verify(orderClient, times(1)).fetchPayments(orderId);
    }

    @Test
    void fetchPaymentIdForOrder_ReturnsAuthorizedPayment() throws RazorpayException {
        String orderId = "order_auth123";
        String expectedPaymentId = "pay_authorized123";

        Payment mockPayment = mock(Payment.class);
        when(mockPayment.get("status")).thenReturn("authorized");
        when(mockPayment.get("id")).thenReturn(expectedPaymentId);

        List<Payment> paymentsList = new ArrayList<>();
        paymentsList.add(mockPayment);

        when(orderClient.fetchPayments(orderId)).thenReturn(paymentsList);

        String result = razorpayPaymentService.fetchPaymentIdForOrder(orderId);

        assertEquals(expectedPaymentId, result);
    }

    @Test
    void fetchPaymentIdForOrder_ReturnsPaidPayment() throws RazorpayException {
        String orderId = "order_paid123";
        String expectedPaymentId = "pay_paid123";

        Payment mockPayment = mock(Payment.class);
        when(mockPayment.get("status")).thenReturn("paid");
        when(mockPayment.get("id")).thenReturn(expectedPaymentId);

        List<Payment> paymentsList = new ArrayList<>();
        paymentsList.add(mockPayment);

        when(orderClient.fetchPayments(orderId)).thenReturn(paymentsList);

        String result = razorpayPaymentService.fetchPaymentIdForOrder(orderId);

        assertEquals(expectedPaymentId, result);
    }

    @Test
    void fetchPaymentIdForOrder_ReturnsNull_WhenNoPayments() throws RazorpayException {
        String orderId = "order_empty123";

        when(orderClient.fetchPayments(orderId)).thenReturn(new ArrayList<>());

        String result = razorpayPaymentService.fetchPaymentIdForOrder(orderId);

        assertNull(result);
    }

    @Test
    void fetchPaymentIdForOrder_ReturnsNull_WhenOnlyPendingPayments() throws RazorpayException {
        String orderId = "order_pending123";

        Payment mockPayment = mock(Payment.class, withSettings().lenient());
        when(mockPayment.get("status")).thenReturn("pending");
        when(mockPayment.get("id")).thenReturn("pay_pending123");

        List<Payment> paymentsList = new ArrayList<>();
        paymentsList.add(mockPayment);

        when(orderClient.fetchPayments(orderId)).thenReturn(paymentsList);

        String result = razorpayPaymentService.fetchPaymentIdForOrder(orderId);

        assertNull(result);
    }

    @Test
    void fetchPaymentIdForOrder_ReturnsNull_WhenPaymentsListIsNull() throws RazorpayException {
        String orderId = "order_null123";

        when(orderClient.fetchPayments(orderId)).thenReturn(null);

        String result = razorpayPaymentService.fetchPaymentIdForOrder(orderId);

        assertNull(result);
    }

    @Test
    void fetchPaymentIdForOrder_ThrowsException() throws RazorpayException {
        String orderId = "order_error123";

        when(orderClient.fetchPayments(orderId)).thenThrow(new RazorpayException("API Error"));

        assertThrows(RazorpayException.class, () -> {
            razorpayPaymentService.fetchPaymentIdForOrder(orderId);
        });
    }

    @Test
    void processRazorpayPayment_WithDifferentAmount() throws RazorpayException {
        PaymentRequest request = new PaymentRequest();
        request.setRechargeId("OMNI-B1");
        request.setAmount(new BigDecimal("999.99"));

        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_999");
        when(orderClient.create(any(JSONObject.class))).thenReturn(mockOrder);

        PaymentResponse response = razorpayPaymentService.processRazorpayPayment(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("999.99"), response.getAmount());
        assertTrue(response.getTransactionId().startsWith("TXN-"));
    }

    @Test
    void processPaymentFallback_WithDifferentException() {
        PaymentRequest request = new PaymentRequest();
        request.setRechargeId("OMNI-C1");
        request.setAmount(new BigDecimal("50.00"));

        Exception mockException = new Exception("Network Error");
        PaymentResponse response = razorpayPaymentService.processPaymentFallback(request, mockException);

        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        assertEquals(new BigDecimal("50.00"), response.getAmount());
        assertNotNull(response.getTimestamp());
    }
}

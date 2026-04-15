package com.omnicharge.payment.service;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.common.event.PaymentCompletedEvent;
import com.omnicharge.common.event.saga.PaymentApprovedEvent;
import com.omnicharge.common.exception.BadRequestException;
import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.payment.client.UserServiceClient;
import com.omnicharge.payment.dto.*;
import com.omnicharge.payment.entity.PaymentMethod;
import com.omnicharge.payment.entity.PaymentStatus;
import com.omnicharge.payment.entity.Transaction;
import com.omnicharge.payment.messaging.PaymentEventProducer;
import com.omnicharge.payment.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private IRazorpayPaymentService razorpayPaymentService;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private LogEventPublisher logEventPublisher;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest paymentRequest;
    private Transaction pendingTransaction;

    @BeforeEach
    void setUp() {
        paymentRequest = new PaymentRequest();
        paymentRequest.setRechargeId("OMNI-1234");
        paymentRequest.setUserId(1L);
        paymentRequest.setAmount(new BigDecimal("299.00"));
        paymentRequest.setPaymentMethod("CREDIT_CARD");
        paymentRequest.setUserEmail("test@test.com");
        paymentRequest.setUserMobile("9876543210");
        paymentRequest.setMobileNumber("9876543210");
        paymentRequest.setOperatorName("Jio");
        paymentRequest.setPlanName("Ultimate 5G");

        pendingTransaction = new Transaction();
        pendingTransaction.setId(100L);
        pendingTransaction.setTransactionId("TXN-XXXXXX");
        pendingTransaction.setRechargeId("OMNI-1234");
        pendingTransaction.setUserId(1L);
        pendingTransaction.setAmount(new BigDecimal("299.00"));
        pendingTransaction.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        pendingTransaction.setStatus(PaymentStatus.PENDING);
        pendingTransaction.setUserEmail("test@test.com");
    }

    @Test
    void processPayment_ReturnsSuccess() {
        PaymentResponse mockResponse = PaymentResponse.builder()
                .status("SUCCESS")
                .razorpayOrderId("order_123")
                .amount(new BigDecimal("299.00"))
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        when(razorpayPaymentService.processRazorpayPayment(any(PaymentRequest.class))).thenReturn(mockResponse);

        PaymentResponse response = paymentService.processPayment(paymentRequest);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("order_123", response.getRazorpayOrderId());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(paymentEventProducer, times(1)).publishPaymentCompleted(any(PaymentCompletedEvent.class));
    }

    @Test
    void processPayment_ReturnsPending() {
        PaymentResponse mockResponse = PaymentResponse.builder()
                .status("PENDING")
                .razorpayOrderId("order_999")
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(razorpayPaymentService.processRazorpayPayment(any(PaymentRequest.class))).thenReturn(mockResponse);

        PaymentResponse response = paymentService.processPayment(paymentRequest);

        assertEquals("PENDING", response.getStatus());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        // Should NOT publish completed event for PENDING
        verify(paymentEventProducer, never()).publishPaymentCompleted(any());
    }

    @Test
    void processPayment_ReturnsFailed() {
        PaymentResponse mockResponse = PaymentResponse.builder().status("FAILED").build();

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(razorpayPaymentService.processRazorpayPayment(any(PaymentRequest.class))).thenReturn(mockResponse);

        PaymentResponse response = paymentService.processPayment(paymentRequest);

        assertEquals("FAILED", response.getStatus());
        verify(paymentEventProducer, times(1)).publishPaymentCompleted(any(PaymentCompletedEvent.class));
    }

    @Test
    void confirmPayment_Success() {
        when(transactionRepository.findByTransactionId("TXN-XXXXXX")).thenReturn(Optional.of(pendingTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(pendingTransaction);

        TransactionResponse response = paymentService.confirmPayment("TXN-XXXXXX", "rzp_999", "sig_999");

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        verify(paymentEventProducer, times(1)).publishPaymentApproved(any(PaymentApprovedEvent.class));
        verify(paymentEventProducer, times(1)).publishPaymentCompleted(any(PaymentCompletedEvent.class));
    }

    @Test
    void confirmPayment_AlreadyConfirmed() {
        pendingTransaction.setStatus(PaymentStatus.SUCCESS);
        when(transactionRepository.findByTransactionId("TXN-XXXXXX")).thenReturn(Optional.of(pendingTransaction));

        TransactionResponse response = paymentService.confirmPayment("TXN-XXXXXX", "rzp_999", "sig_999");

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void confirmPayment_MissingMetadata_FetchesFromRechargeService() {
        // Remove metadata
        pendingTransaction.setMobileNumber(null);
        pendingTransaction.setOperatorName(null);
        pendingTransaction.setPlanName(null);

        when(transactionRepository.findByTransactionId("TXN-XXXXXX")).thenReturn(Optional.of(pendingTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(pendingTransaction);

        // Mock RestTemplate response
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        Map<String, Object> data = new HashMap<>();
        data.put("mobileNumber", "1234567890");
        data.put("operatorName", "MockOperator");
        data.put("planName", "MockPlan");
        body.put("data", data);

        ResponseEntity<Map<String, Object>> responseEntity = ResponseEntity.ok(body);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        paymentService.confirmPayment("TXN-XXXXXX", "rzp_999", "sig_999");

        assertEquals("1234567890", pendingTransaction.getMobileNumber());
        assertEquals("MockOperator", pendingTransaction.getOperatorName());
        assertEquals("MockPlan", pendingTransaction.getPlanName());
        verify(restTemplate, times(1)).exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    void getTransaction_ValidMatch() {
        when(transactionRepository.findByTransactionId("TXN-XXXXXX")).thenReturn(Optional.of(pendingTransaction));
        TransactionResponse response = paymentService.getTransaction("TXN-XXXXXX", 1L);
        assertEquals(100L, response.getId());
    }

    @Test
    void getTransaction_Unauthorized() {
        when(transactionRepository.findByTransactionId("TXN-XXXXXX")).thenReturn(Optional.of(pendingTransaction));
        assertThrows(BadRequestException.class, () -> paymentService.getTransaction("TXN-XXXXXX", 2L));
    }

    @Test
    void getTransaction_NotFound() {
        when(transactionRepository.findByTransactionId("TXN-XXXXXX")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> paymentService.getTransaction("TXN-XXXXXX", 1L));
    }

    @Test
    void getPaymentHistory_Success() {
        Page<Transaction> page = new PageImpl<>(Collections.singletonList(pendingTransaction));
        when(transactionRepository.findByUserIdWithFilters(anyLong(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        Page<TransactionResponse> result = paymentService.getPaymentHistory(1L, null, null, null, null, null, null, PageRequest.of(0, 10));

        assertFalse(result.isEmpty());
        assertEquals("TXN-XXXXXX", result.getContent().get(0).getTransactionId());
    }

    @Test
    void getPaymentStats_CheckNullDaysFallback() {
        when(transactionRepository.count()).thenReturn(150L);
        when(transactionRepository.countByStatus(any())).thenReturn(100L);
        when(transactionRepository.sumAmountByStatus(any())).thenReturn(new BigDecimal("5000"));

        PaymentStatsResponse stats = paymentService.getPaymentStats(null);

        assertEquals(150L, stats.getTotalTransactions());
        assertEquals(new BigDecimal("5000"), stats.getTotalRevenue());
    }

    @Test
    void getAllTransactions_Success() {
        Page<Transaction> page = new PageImpl<>(Collections.singletonList(pendingTransaction));
        when(transactionRepository.findAllWithFilters(anyLong(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        Page<TransactionResponse> result = paymentService.getAllTransactions(
                1L, null, null, null, null, null, null, PageRequest.of(0, 10));

        assertFalse(result.isEmpty());
        assertEquals("TXN-XXXXXX", result.getContent().get(0).getTransactionId());
        verify(transactionRepository, times(1)).findAllWithFilters(anyLong(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failPayment_FromPending() {
        when(transactionRepository.findByTransactionId("TXN-XXXXXX")).thenReturn(Optional.of(pendingTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(pendingTransaction);

        TransactionResponse response = paymentService.failPayment("TXN-XXXXXX", "User cancelled");

        assertEquals(PaymentStatus.FAILED, response.getStatus());
        verify(paymentEventProducer, times(1)).publishPaymentRejected(any());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void failPayment_AlreadySuccess_NoChange() {
        pendingTransaction.setStatus(PaymentStatus.SUCCESS);
        when(transactionRepository.findByTransactionId("TXN-XXXXXX")).thenReturn(Optional.of(pendingTransaction));

        TransactionResponse response = paymentService.failPayment("TXN-XXXXXX", "User cancelled");

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        verify(transactionRepository, never()).save(any());
        verify(paymentEventProducer, never()).publishPaymentRejected(any());
    }

    @Test
    void failPayment_AlreadyFailed_NoChange() {
        pendingTransaction.setStatus(PaymentStatus.FAILED);
        when(transactionRepository.findByTransactionId("TXN-XXXXXX")).thenReturn(Optional.of(pendingTransaction));

        TransactionResponse response = paymentService.failPayment("TXN-XXXXXX", "User cancelled");

        assertEquals(PaymentStatus.FAILED, response.getStatus());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void failPayment_NotFound() {
        when(transactionRepository.findByTransactionId("TXN-XXXXXX")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
                () -> paymentService.failPayment("TXN-XXXXXX", "User cancelled"));
    }

    @Test
    void getPaymentAnalytics_WithDays() {
        when(transactionRepository.sumAmountBetweenDates(any(), any(), any())).thenReturn(new BigDecimal("10000"));
        when(transactionRepository.countTransactionsBetweenDates(any(), any(), any())).thenReturn(50L);
        when(transactionRepository.findRevenueByDate(any(), any())).thenReturn(Collections.emptyList());
        when(transactionRepository.findTopSpendersByRevenueWithDateFilter(any(), any(), any())).thenReturn(Collections.emptyList());

        PaymentAnalyticsResponse response = paymentService.getPaymentAnalytics(30);

        assertNotNull(response);
        assertEquals(new BigDecimal("10000"), response.getGrossRevenue());
        verify(transactionRepository, atLeastOnce()).sumAmountBetweenDates(any(), any(), any());
    }

    @Test
    void getPaymentAnalytics_NullDays_AllTime() {
        when(transactionRepository.sumAmountBetweenDates(any(), any(), any())).thenReturn(new BigDecimal("50000"));
        when(transactionRepository.countTransactionsBetweenDates(any(), any(), any())).thenReturn(200L);
        when(transactionRepository.findRevenueByDate(any(), any())).thenReturn(Collections.emptyList());
        when(transactionRepository.findTopSpendersByRevenue(any(), any())).thenReturn(Collections.emptyList());

        PaymentAnalyticsResponse response = paymentService.getPaymentAnalytics(null);

        assertNotNull(response);
        assertEquals(new BigDecimal("50000"), response.getGrossRevenue());
    }

    @Test
    void getPaymentAnalytics_WithRevenueData() {
        Object[] revenueRow = new Object[]{"2024-01-15", 10L, new BigDecimal("5000")};
        when(transactionRepository.sumAmountBetweenDates(any(), any(), any())).thenReturn(new BigDecimal("5000"));
        when(transactionRepository.countTransactionsBetweenDates(any(), any(), any())).thenReturn(10L);
        when(transactionRepository.findRevenueByDate(any(), any())).thenReturn(Collections.singletonList(revenueRow));
        when(transactionRepository.findTopSpendersByRevenueWithDateFilter(any(), any(), any())).thenReturn(Collections.emptyList());

        PaymentAnalyticsResponse response = paymentService.getPaymentAnalytics(7);

        assertNotNull(response);
        assertFalse(response.getDailyRevenue().isEmpty());
        assertEquals("2024-01-15", response.getDailyRevenue().get(0).getDate());
        assertEquals(10L, response.getDailyRevenue().get(0).getTransactionCount());
    }

    @Test
    void getPaymentAnalytics_RevenueGrowthCalculation() {
        when(transactionRepository.sumAmountBetweenDates(any(), any(), any()))
                .thenReturn(new BigDecimal("8000"))  // lastMonthRevenue
                .thenReturn(new BigDecimal("10000")); // monthRevenue
        when(transactionRepository.countTransactionsBetweenDates(any(), any(), any())).thenReturn(50L);
        when(transactionRepository.findRevenueByDate(any(), any())).thenReturn(Collections.emptyList());
        when(transactionRepository.findTopSpendersByRevenueWithDateFilter(any(), any(), any())).thenReturn(Collections.emptyList());

        PaymentAnalyticsResponse response = paymentService.getPaymentAnalytics(30);

        assertNotNull(response);
        // Growth = (10000 - 8000) / 8000 * 100 = 25%
        assertTrue(response.getRevenueGrowthPercentage() >= 0);
    }

    @Test
    void getTopSpenders_WithLimit() {
        Object[] spenderRow = new Object[]{
                1L, // userId
                5L, // transactionCount
                new BigDecimal("2500"), // totalSpent
                new BigDecimal("500"), // avgTransactionValue
                LocalDateTime.now(), // lastTransactionDate
                LocalDateTime.now().minusDays(30) // firstTransactionDate
        };

        when(transactionRepository.findTopSpendersByRevenue(any(), any()))
                .thenReturn(Collections.singletonList(spenderRow));
        when(transactionRepository.findAllByUserId(anyLong(), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(pendingTransaction)));
        when(transactionRepository.getUserTransactionStats(anyLong()))
                .thenReturn(new Object[]{10L, 8L, 2L}); // total, success, failed

        ApiResponse<UserProfileResponse> userApiResponse = new ApiResponse<>();
        userApiResponse.setSuccess(true);
        UserProfileResponse userProfile = new UserProfileResponse();
        userProfile.setFullName("Test User");
        userProfile.setEmail("test@test.com");
        userProfile.setMobileNumber("9876543210");
        userProfile.setCreatedDate("2024-01-01");
        userApiResponse.setData(userProfile);

        when(userServiceClient.getUserById(anyLong())).thenReturn(userApiResponse);

        List<TopSpenderStats> result = paymentService.getTopSpenders(10, null);

        assertFalse(result.isEmpty());
        assertEquals(1L, result.get(0).getUserId());
        assertEquals("Test User", result.get(0).getFullName());
        assertEquals(new BigDecimal("2500"), result.get(0).getTotalSpent());
    }

    @Test
    void getTopSpenders_WithDaysFilter() {
        Object[] spenderRow = new Object[]{
                2L, 3L, new BigDecimal("1500"), new BigDecimal("500"), 
                LocalDateTime.now(), LocalDateTime.now().minusDays(7)
        };

        when(transactionRepository.findTopSpendersByRevenueWithDateFilter(any(), any(), any()))
                .thenReturn(Collections.singletonList(spenderRow));
        when(transactionRepository.findAllByUserId(anyLong(), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(pendingTransaction)));
        when(transactionRepository.getUserTransactionStats(anyLong()))
                .thenReturn(new Object[]{5L, 4L, 1L});

        ApiResponse<UserProfileResponse> userApiResponse = new ApiResponse<>();
        userApiResponse.setSuccess(true);
        UserProfileResponse userProfile = new UserProfileResponse();
        userProfile.setFullName("Another User");
        userProfile.setEmail("another@test.com");
        userApiResponse.setData(userProfile);

        when(userServiceClient.getUserById(anyLong())).thenReturn(userApiResponse);

        List<TopSpenderStats> result = paymentService.getTopSpenders(5, 7);

        assertFalse(result.isEmpty());
        assertEquals(2L, result.get(0).getUserId());
        verify(transactionRepository, times(1)).findTopSpendersByRevenueWithDateFilter(any(), any(), any());
    }

    @Test
    void getTopSpenders_NullLimit_DefaultsTo10() {
        when(transactionRepository.findTopSpendersByRevenue(any(), any())).thenReturn(Collections.emptyList());

        List<TopSpenderStats> result = paymentService.getTopSpenders(null, null);

        assertNotNull(result);
        verify(transactionRepository, times(1)).findTopSpendersByRevenue(eq(PaymentStatus.SUCCESS), any());
    }

    @Test
    void getTopSpenders_UserServiceFails_FallbackToEmail() {
        Object[] spenderRow = new Object[]{
                3L, 2L, new BigDecimal("1000"), new BigDecimal("500"), 
                LocalDateTime.now(), LocalDateTime.now().minusDays(5)
        };

        when(transactionRepository.findTopSpendersByRevenue(any(), any()))
                .thenReturn(Collections.singletonList(spenderRow));
        
        Transaction tx = new Transaction();
        tx.setUserEmail("fallback@test.com");
        tx.setUserMobile("1234567890");
        when(transactionRepository.findAllByUserId(anyLong(), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(tx)));
        when(transactionRepository.getUserTransactionStats(anyLong()))
                .thenReturn(new Object[]{3L, 2L, 1L});

        when(userServiceClient.getUserById(anyLong())).thenThrow(new RuntimeException("Service down"));

        List<TopSpenderStats> result = paymentService.getTopSpenders(10, null);

        assertFalse(result.isEmpty());
        assertEquals("fallback", result.get(0).getFullName()); // Email prefix
        assertEquals("fallback@test.com", result.get(0).getUserEmail());
    }

    @Test
    void getTopSpenders_NoTransactionData_FallbackToUserId() {
        Object[] spenderRow = new Object[]{
                4L, 1L, new BigDecimal("500"), new BigDecimal("500"), 
                LocalDateTime.now(), LocalDateTime.now()
        };

        when(transactionRepository.findTopSpendersByRevenue(any(), any()))
                .thenReturn(Collections.singletonList(spenderRow));
        when(transactionRepository.findAllByUserId(anyLong(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(transactionRepository.getUserTransactionStats(anyLong()))
                .thenReturn(null);

        ApiResponse<UserProfileResponse> userApiResponse = new ApiResponse<>();
        userApiResponse.setSuccess(false);
        when(userServiceClient.getUserById(anyLong())).thenReturn(userApiResponse);

        List<TopSpenderStats> result = paymentService.getTopSpenders(10, null);

        assertFalse(result.isEmpty());
        assertEquals("User #4", result.get(0).getFullName());
    }

    @Test
    void getUserTransactions_WithSearch() {
        Page<Transaction> page = new PageImpl<>(Collections.singletonList(pendingTransaction));
        when(transactionRepository.searchByUserIdAndTransactionId(anyLong(), anyString(), any()))
                .thenReturn(page);

        Page<TransactionResponse> result = paymentService.getUserTransactions(
                1L, null, "TXN-XXXXXX", PageRequest.of(0, 10));

        assertFalse(result.isEmpty());
        assertEquals("TXN-XXXXXX", result.getContent().get(0).getTransactionId());
        verify(transactionRepository, times(1)).searchByUserIdAndTransactionId(eq(1L), eq("TXN-XXXXXX"), any());
    }

    @Test
    void getUserTransactions_WithStatus() {
        Page<Transaction> page = new PageImpl<>(Collections.singletonList(pendingTransaction));
        when(transactionRepository.findByUserIdAndStatus(anyLong(), any(), any()))
                .thenReturn(page);

        Page<TransactionResponse> result = paymentService.getUserTransactions(
                1L, PaymentStatus.SUCCESS, null, PageRequest.of(0, 10));

        assertFalse(result.isEmpty());
        verify(transactionRepository, times(1)).findByUserIdAndStatus(eq(1L), eq(PaymentStatus.SUCCESS), any());
    }

    @Test
    void getUserTransactions_NoFilters() {
        Page<Transaction> page = new PageImpl<>(Collections.singletonList(pendingTransaction));
        when(transactionRepository.findAllByUserId(anyLong(), any()))
                .thenReturn(page);

        Page<TransactionResponse> result = paymentService.getUserTransactions(
                1L, null, null, PageRequest.of(0, 10));

        assertFalse(result.isEmpty());
        verify(transactionRepository, times(1)).findAllByUserId(eq(1L), any());
    }

    @Test
    void getUserTransactions_EmptySearch_TrimsToNull() {
        Page<Transaction> page = new PageImpl<>(Collections.singletonList(pendingTransaction));
        when(transactionRepository.findAllByUserId(anyLong(), any()))
                .thenReturn(page);

        Page<TransactionResponse> result = paymentService.getUserTransactions(
                1L, null, "   ", PageRequest.of(0, 10));

        assertFalse(result.isEmpty());
        verify(transactionRepository, times(1)).findAllByUserId(eq(1L), any());
        verify(transactionRepository, never()).searchByUserIdAndTransactionId(anyLong(), anyString(), any());
    }

    @Test
    void getPaymentAnalytics_NumberTypeConversion() {
        // Test BigDecimal to Number conversion in revenue data
        Object[] revenueRow = new Object[]{"2024-01-20", 15L, 7500.50}; // Double instead of BigDecimal
        when(transactionRepository.sumAmountBetweenDates(any(), any(), any())).thenReturn(new BigDecimal("7500.50"));
        when(transactionRepository.countTransactionsBetweenDates(any(), any(), any())).thenReturn(15L);
        when(transactionRepository.findRevenueByDate(any(), any())).thenReturn(Collections.singletonList(revenueRow));
        when(transactionRepository.findTopSpendersByRevenueWithDateFilter(any(), any(), any())).thenReturn(Collections.emptyList());

        PaymentAnalyticsResponse response = paymentService.getPaymentAnalytics(7);

        assertNotNull(response);
        assertFalse(response.getDailyRevenue().isEmpty());
        // Compare using compareTo to handle BigDecimal precision issues
        assertEquals(0, new BigDecimal("7500.50").compareTo(response.getDailyRevenue().get(0).getRevenue()));
    }

    @Test
    void getPaymentAnalytics_ZeroTransactions_AvoidsDivideByZero() {
        // Test divide-by-zero prevention when no successful transactions exist
        when(transactionRepository.sumAmountBetweenDates(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.countTransactionsBetweenDates(any(), any(), any())).thenReturn(0L);
        when(transactionRepository.findRevenueByDate(any(), any())).thenReturn(Collections.emptyList());
        when(transactionRepository.findTopSpendersByRevenueWithDateFilter(any(), any(), any())).thenReturn(Collections.emptyList());

        PaymentAnalyticsResponse response = paymentService.getPaymentAnalytics(30);

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getAverageTransactionValue());
        assertEquals(0.0, response.getSuccessRate());
        assertEquals(0L, response.getTotalTransactions());
    }

    @Test
    void getPaymentAnalytics_NullRevenue_HandlesGracefully() {
        // Test null revenue handling
        when(transactionRepository.sumAmountBetweenDates(any(), any(), any())).thenReturn(null);
        when(transactionRepository.countTransactionsBetweenDates(any(), any(), any())).thenReturn(0L);
        when(transactionRepository.findRevenueByDate(any(), any())).thenReturn(Collections.emptyList());
        when(transactionRepository.findTopSpendersByRevenueWithDateFilter(any(), any(), any())).thenReturn(Collections.emptyList());

        PaymentAnalyticsResponse response = paymentService.getPaymentAnalytics(7);

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getGrossRevenue());
        assertEquals(BigDecimal.ZERO, response.getTodayRevenue());
    }

    @Test
    void getPaymentAnalytics_ZeroLastMonthRevenue_AvoidsDivideByZero() {
        // Test revenue growth calculation when last month revenue is zero
        when(transactionRepository.sumAmountBetweenDates(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO)  // lastMonthRevenue
                .thenReturn(new BigDecimal("5000")); // monthRevenue
        when(transactionRepository.countTransactionsBetweenDates(any(), any(), any())).thenReturn(10L);
        when(transactionRepository.findRevenueByDate(any(), any())).thenReturn(Collections.emptyList());
        when(transactionRepository.findTopSpendersByRevenueWithDateFilter(any(), any(), any())).thenReturn(Collections.emptyList());

        PaymentAnalyticsResponse response = paymentService.getPaymentAnalytics(30);

        assertNotNull(response);
        assertEquals(0.0, response.getRevenueGrowthPercentage());
    }

    @Test
    void getTopSpenders_NumberTypeConversion() {
        // Test Number to BigDecimal conversion in top spenders
        Object[] spenderRow = new Object[]{
                5L, 4L, 
                3200.75, // Double instead of BigDecimal
                800.1875, // Double instead of BigDecimal
                LocalDateTime.now(), LocalDateTime.now().minusDays(10)
        };

        when(transactionRepository.findTopSpendersByRevenue(any(), any()))
                .thenReturn(Collections.singletonList(spenderRow));
        when(transactionRepository.findAllByUserId(anyLong(), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(pendingTransaction)));
        when(transactionRepository.getUserTransactionStats(anyLong()))
                .thenReturn(new Object[]{6L, 4L, 2L});

        ApiResponse<UserProfileResponse> userApiResponse = new ApiResponse<>();
        userApiResponse.setSuccess(true);
        UserProfileResponse userProfile = new UserProfileResponse();
        userProfile.setFullName("Number Test User");
        userApiResponse.setData(userProfile);

        when(userServiceClient.getUserById(anyLong())).thenReturn(userApiResponse);

        List<TopSpenderStats> result = paymentService.getTopSpenders(10, null);

        assertFalse(result.isEmpty());
        assertEquals(new BigDecimal("3200.75"), result.get(0).getTotalSpent());
        assertEquals(new BigDecimal("800.1875"), result.get(0).getAverageTransactionValue());
    }
}

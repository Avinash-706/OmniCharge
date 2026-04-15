package com.omnicharge.payment.controller;

import com.omnicharge.payment.dto.*;
import com.omnicharge.payment.entity.PaymentStatus;
import com.omnicharge.payment.service.IPaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminPaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
@MockBean(com.omnicharge.common.logging.LogEventPublisher.class)
class AdminPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IPaymentService paymentService;

    @Test
    void getAllTransactions_SuccessWhenAdmin() throws Exception {
        TransactionResponse response = TransactionResponse.builder().transactionId("TXN-ADMIN").build();
        Page<TransactionResponse> page = new PageImpl<>(Collections.singletonList(response));
        
        when(paymentService.getAllTransactions(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/payments")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].transactionId").value("TXN-ADMIN"));
    }

    @Test
    void getAllTransactions_ForbiddenWhenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/payments")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied: Admin role required"));
    }

    @Test
    void getPaymentStats_Success() throws Exception {
        PaymentStatsResponse stats = PaymentStatsResponse.builder()
                .totalTransactions(500L)
                .topUsers(Collections.emptyList())
                .build();
        when(paymentService.getPaymentStats(anyInt())).thenReturn(stats);

        mockMvc.perform(get("/api/admin/payments/stats")
                        .header("X-User-Role", "ADMIN")
                        .param("days", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTransactions").value(500));
    }

    @Test
    void getPaymentStats_ForbiddenWhenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/payments/stats")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPaymentAnalytics_Success() throws Exception {
        PaymentAnalyticsResponse analytics = PaymentAnalyticsResponse.builder()
                .grossRevenue(new BigDecimal("50000"))
                .todayRevenue(new BigDecimal("5000"))
                .monthRevenue(new BigDecimal("30000"))
                .totalTransactions(500L)
                .successfulTransactions(450L)
                .failedTransactions(50L)
                .pendingTransactions(0L)
                .successRate(90.0)
                .abandonedCheckoutRate(10.0)
                .averageTransactionValue(new BigDecimal("100"))
                .revenueGrowthPercentage(15.5)
                .topSpenders(Collections.emptyList())
                .dailyRevenue(Collections.emptyList())
                .build();

        when(paymentService.getPaymentAnalytics(anyInt())).thenReturn(analytics);

        mockMvc.perform(get("/api/admin/payments/analytics")
                        .param("days", "30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.grossRevenue").value(50000))
                .andExpect(jsonPath("$.data.totalTransactions").value(500))
                .andExpect(jsonPath("$.data.successRate").value(90.0));
    }

    @Test
    void getPaymentAnalytics_WithDefaultDays() throws Exception {
        PaymentAnalyticsResponse analytics = PaymentAnalyticsResponse.builder()
                .grossRevenue(new BigDecimal("100000"))
                .totalTransactions(1000L)
                .successfulTransactions(900L)
                .topSpenders(Collections.emptyList())
                .dailyRevenue(Collections.emptyList())
                .build();

        when(paymentService.getPaymentAnalytics(30)).thenReturn(analytics);

        mockMvc.perform(get("/api/admin/payments/analytics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.grossRevenue").value(100000));
    }

    @Test
    void getTopSpenders_Success() throws Exception {
        List<TopSpenderStats> topSpenders = new ArrayList<>();
        TopSpenderStats spender1 = TopSpenderStats.builder()
                .userId(1L)
                .fullName("John Doe")
                .userEmail("john@test.com")
                .totalSpent(new BigDecimal("5000"))
                .transactionCount(50L)
                .successfulTransactions(48L)
                .failedTransactions(2L)
                .averageTransactionValue(new BigDecimal("100"))
                .successRate(96.0)
                .build();
        topSpenders.add(spender1);

        when(paymentService.getTopSpenders(anyInt(), any())).thenReturn(topSpenders);

        mockMvc.perform(get("/api/admin/payments/top-spenders")
                        .param("limit", "10")
                        .param("days", "30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userId").value(1))
                .andExpect(jsonPath("$.data[0].fullName").value("John Doe"))
                .andExpect(jsonPath("$.data[0].totalSpent").value(5000));
    }

    @Test
    void getTopSpenders_WithDefaultLimit() throws Exception {
        List<TopSpenderStats> topSpenders = Collections.emptyList();
        when(paymentService.getTopSpenders(10, null)).thenReturn(topSpenders);

        mockMvc.perform(get("/api/admin/payments/top-spenders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getTopSpenders_WithDaysFilter() throws Exception {
        List<TopSpenderStats> topSpenders = new ArrayList<>();
        TopSpenderStats spender = TopSpenderStats.builder()
                .userId(2L)
                .fullName("Jane Smith")
                .totalSpent(new BigDecimal("3000"))
                .transactionCount(30L)
                .build();
        topSpenders.add(spender);

        when(paymentService.getTopSpenders(5, 7)).thenReturn(topSpenders);

        mockMvc.perform(get("/api/admin/payments/top-spenders")
                        .param("limit", "5")
                        .param("days", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(2));
    }

    @Test
    void getUserTransactions_Success() throws Exception {
        TransactionResponse transaction = TransactionResponse.builder()
                .transactionId("TXN-USER-001")
                .userId(1L)
                .amount(new BigDecimal("500"))
                .status(PaymentStatus.SUCCESS)
                .build();
        Page<TransactionResponse> page = new PageImpl<>(Collections.singletonList(transaction));

        when(paymentService.getUserTransactions(anyLong(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/payments/user/1/transactions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].transactionId").value("TXN-USER-001"))
                .andExpect(jsonPath("$.data.content[0].userId").value(1));
    }

    @Test
    void getUserTransactions_WithStatusFilter() throws Exception {
        TransactionResponse transaction = TransactionResponse.builder()
                .transactionId("TXN-USER-002")
                .userId(2L)
                .status(PaymentStatus.SUCCESS)
                .build();
        Page<TransactionResponse> page = new PageImpl<>(Collections.singletonList(transaction));

        when(paymentService.getUserTransactions(eq(2L), eq(PaymentStatus.SUCCESS), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/payments/user/2/transactions")
                        .param("status", "SUCCESS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("SUCCESS"));
    }

    @Test
    void getUserTransactions_WithSearchFilter() throws Exception {
        TransactionResponse transaction = TransactionResponse.builder()
                .transactionId("TXN-SEARCH-123")
                .userId(3L)
                .build();
        Page<TransactionResponse> page = new PageImpl<>(Collections.singletonList(transaction));

        when(paymentService.getUserTransactions(eq(3L), any(), eq("TXN-SEARCH"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/payments/user/3/transactions")
                        .param("search", "TXN-SEARCH")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].transactionId").value("TXN-SEARCH-123"));
    }

    @Test
    void getUserTransactions_WithPaginationAndSorting() throws Exception {
        List<TransactionResponse> transactions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            transactions.add(TransactionResponse.builder()
                    .transactionId("TXN-" + i)
                    .userId(1L)
                    .build());
        }
        Page<TransactionResponse> page = new PageImpl<>(transactions);

        when(paymentService.getUserTransactions(eq(1L), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/payments/user/1/transactions")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "createdDate")
                        .param("sortDir", "DESC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(5));
    }

    @Test
    void getUserTransactions_EmptyResult() throws Exception {
        Page<TransactionResponse> emptyPage = new PageImpl<>(Collections.emptyList());

        when(paymentService.getUserTransactions(anyLong(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/admin/payments/user/999/transactions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }
}

package com.omnicharge.recharge.controller;

import com.omnicharge.recharge.dto.*;
import com.omnicharge.recharge.entity.RechargeStatus;
import com.omnicharge.recharge.service.IRechargeService;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminRechargeController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
@MockBean(com.omnicharge.common.logging.LogEventPublisher.class)
class AdminRechargeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IRechargeService rechargeService;

    @Test
    void getAllRecharges() throws Exception {
        RechargeResponse response = RechargeResponse.builder()
                .rechargeId("OMNI-ADMINVIEW")
                .operatorName("Vodafone")
                .build();
        Page<RechargeResponse> page = new PageImpl<>(Collections.singletonList(response));
        
        when(rechargeService.getAllRecharges(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/recharges")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].operatorName").value("Vodafone"));
    }

    @Test
    void getRechargeStats() throws Exception {
        RechargeStatsResponse stats = RechargeStatsResponse.builder()
                .totalRecharges(100L)
                .successCount(80L)
                .failedCount(20L)
                .totalAmount(new BigDecimal("15000.00"))
                .build();

        when(rechargeService.getRechargeStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/recharges/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalAmount").value(15000.00))
                .andExpect(jsonPath("$.data.successCount").value(80));
    }

    @Test
    void getRechargeAnalytics_WithDaysParam() throws Exception {
        PlanPerformanceStats plan = PlanPerformanceStats.builder()
                .planId(1L)
                .planName("Premium Plan")
                .operatorId(1L)
                .operatorName("Airtel")
                .rechargeCount(50L)
                .totalRevenue(new BigDecimal("25000"))
                .averageAmount(new BigDecimal("500"))
                .build();

        OperatorMarketShare operator = OperatorMarketShare.builder()
                .operatorId(1L)
                .operatorName("Airtel")
                .rechargeCount(100L)
                .totalRevenue(new BigDecimal("50000"))
                .marketSharePercentage(60.0)
                .build();

        RechargeAnalyticsResponse analytics = RechargeAnalyticsResponse.builder()
                .totalRecharges(100L)
                .todayRecharges(10L)
                .monthRecharges(50L)
                .totalRevenue(new BigDecimal("50000"))
                .todayRevenue(new BigDecimal("5000"))
                .monthRevenue(new BigDecimal("25000"))
                .successRate(85.0)
                .successCount(85L)
                .failedCount(10L)
                .pendingCount(5L)
                .activeRecharges(80L)
                .expiredRecharges(20L)
                .activeRatio(80.0)
                .topPlans(List.of(plan))
                .operatorShares(List.of(operator))
                .build();

        when(rechargeService.getRechargeAnalytics(30)).thenReturn(analytics);

        mockMvc.perform(get("/api/admin/recharges/analytics")
                        .param("days", "30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRecharges").value(100))
                .andExpect(jsonPath("$.data.successRate").value(85.0))
                .andExpect(jsonPath("$.data.topPlans[0].planName").value("Premium Plan"))
                .andExpect(jsonPath("$.data.operatorShares[0].marketSharePercentage").value(60.0));
    }

    @Test
    void getRechargeAnalytics_NoDaysParam_AllTime() throws Exception {
        RechargeAnalyticsResponse analytics = RechargeAnalyticsResponse.builder()
                .totalRecharges(500L)
                .totalRevenue(new BigDecimal("250000"))
                .successRate(90.0)
                .topPlans(Collections.emptyList())
                .operatorShares(Collections.emptyList())
                .build();

        when(rechargeService.getRechargeAnalytics(null)).thenReturn(analytics);

        mockMvc.perform(get("/api/admin/recharges/analytics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRecharges").value(500));
    }

    @Test
    void getOperatorPlans_Success() throws Exception {
        PlanPerformanceStats plan1 = PlanPerformanceStats.builder()
                .planId(1L)
                .planName("Plan A")
                .rechargeCount(30L)
                .totalRevenue(new BigDecimal("15000"))
                .build();

        PlanPerformanceStats plan2 = PlanPerformanceStats.builder()
                .planId(2L)
                .planName("Plan B")
                .rechargeCount(20L)
                .totalRevenue(new BigDecimal("10000"))
                .build();

        OperatorPlansResponse response = OperatorPlansResponse.builder()
                .operatorId(1L)
                .operatorName("Airtel")
                .totalRecharges(50L)
                .totalRevenue(new BigDecimal("25000"))
                .plans(List.of(plan1, plan2))
                .build();

        when(rechargeService.getOperatorPlans(1L)).thenReturn(response);

        mockMvc.perform(get("/api/admin/recharges/operator/1/plans")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.operatorName").value("Airtel"))
                .andExpect(jsonPath("$.data.plans.length()").value(2))
                .andExpect(jsonPath("$.data.plans[0].planName").value("Plan A"));
    }

    @Test
    void getPlanRechargeHistory_WithAllParams() throws Exception {
        RechargeResponse response = RechargeResponse.builder()
                .rechargeId("OMNI-PLAN1")
                .planId(10L)
                .planName("Test Plan")
                .build();
        Page<RechargeResponse> page = new PageImpl<>(List.of(response));

        when(rechargeService.getPlanRechargeHistory(eq(10L), eq(RechargeStatus.SUCCESS), eq("test"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/recharges/plan/10/history")
                        .param("status", "SUCCESS")
                        .param("search", "test")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].planName").value("Test Plan"));
    }

    @Test
    void getPlanRechargeHistory_NoOptionalParams() throws Exception {
        Page<RechargeResponse> page = new PageImpl<>(Collections.emptyList());

        when(rechargeService.getPlanRechargeHistory(eq(10L), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/recharges/plan/10/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getPlanRechargeHistory_WithWhitespaceSearch_IgnoresSearch() throws Exception {
        Page<RechargeResponse> page = new PageImpl<>(Collections.emptyList());

        when(rechargeService.getPlanRechargeHistory(eq(10L), isNull(), eq("   "), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/recharges/plan/10/history")
                        .param("search", "   ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getOperatorRechargeHistory_WithStatusFilter() throws Exception {
        RechargeResponse response = RechargeResponse.builder()
                .rechargeId("OMNI-OP1")
                .operatorId(1L)
                .operatorName("Jio")
                .build();
        Page<RechargeResponse> page = new PageImpl<>(List.of(response));

        when(rechargeService.getOperatorRechargeHistory(eq(1L), eq(RechargeStatus.FAILED), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/recharges/operator/1/history")
                        .param("status", "FAILED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].operatorName").value("Jio"));
    }

    @Test
    void getOperatorRechargeHistory_WithSearchParam() throws Exception {
        Page<RechargeResponse> page = new PageImpl<>(Collections.emptyList());

        when(rechargeService.getOperatorRechargeHistory(eq(2L), isNull(), eq("vodafone"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/recharges/operator/2/history")
                        .param("search", "vodafone")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getOperatorRechargeHistory_WithEmptySearch_IgnoresSearch() throws Exception {
        Page<RechargeResponse> page = new PageImpl<>(Collections.emptyList());

        when(rechargeService.getOperatorRechargeHistory(eq(2L), isNull(), eq("  "), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/recharges/operator/2/history")
                        .param("search", "  ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getUserRechargeHistory_WithAllFilters() throws Exception {
        RechargeResponse response = RechargeResponse.builder()
                .rechargeId("OMNI-USER1")
                .userId(5L)
                .mobileNumber("9876543210")
                .build();
        Page<RechargeResponse> page = new PageImpl<>(List.of(response));

        when(rechargeService.getUserRechargeHistory(eq(5L), eq(RechargeStatus.PROCESSING), eq("9876"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/recharges/user/5/history")
                        .param("status", "PROCESSING")
                        .param("search", "9876")
                        .param("sortBy", "amount")
                        .param("sortDir", "ASC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].mobileNumber").value("9876543210"));
    }

    @Test
    void getUserRechargeHistory_NoFilters() throws Exception {
        Page<RechargeResponse> page = new PageImpl<>(Collections.emptyList());

        when(rechargeService.getUserRechargeHistory(eq(10L), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/recharges/user/10/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
